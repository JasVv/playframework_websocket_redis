package controllers

import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep}
import chat._
import play.api.http.websocket._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.{ActorFlow, AkkaStreams}
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.http.websocket.Message.Ping

import java.util.UUID
import javax.inject._
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

@Singleton
class ChatController @Inject()(override val controllerComponents: ControllerComponents)
                              (implicit system: ActorSystem,
                               materializer: Materializer,
                               ec: ExecutionContext,
                              ) extends BaseController {
  implicit val jsonMessageFlowTransformer: MessageFlowTransformer[JsValue, Either[Ping, JsValue]] = {
    def closeOnException[T](block: => T) =
      try {
        Left(block)
      } catch {
        case NonFatal(_) => Right(CloseMessage(Some(CloseCodes.Unacceptable), "Unable to parse json message"))
      }

    (flow: Flow[JsValue, Either[Ping, JsValue], _]) => {
      AkkaStreams.bypassWith[Message, JsValue, Message](Flow[Message].collect {
        case BinaryMessage(data) => closeOnException(Json.parse(data.iterator.asInputStream))
        case TextMessage(text) => closeOnException(Json.parse(text))
      })(flow.map {
        case Left(ping) => PingMessage(ping.data())
        case Right(json) => TextMessage(Json.stringify(json))
      })
    }
  }

  def view: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.main())
  }

  def ws(roomId: String): WebSocket = WebSocket.accept[JsValue, Either[Ping, JsValue]] { _ =>
    val chatRoomActorRef = ChatActorMap.chatRoomActors.getOrElse(roomId, {
      val actor = system.actorOf(Props(classOf[ChatRoomActor], roomId, materializer, ec), name = s"chatRoom_$roomId")
      ChatActorMap.chatRoomActors.put(roomId, actor)
      actor
    })

    val userId = UUID.randomUUID().toString
    val in = Flow.fromFunction[JsValue, ChatMessage] { text =>
      chatRoomActorRef ! PublishMessage(text.toString())
      NoActMessage()
    }
    val out = ActorFlow.actorRef[ChatMessage, Either[Ping, JsValue]](UserActor.props(_, chatRoomActorRef, userId, ec))

    chatRoomActorRef ! AddUserMessage(userId, out)

    in.viaMat(out)(Keep.left)
  }

  // 指定したチャットルームにAPIでメッセージを送信する
  def send(roomId: String, message: String): Action[AnyContent] = Action { implicit request =>
    ChatActorMap.chatRoomActors.get(roomId) match {
      case Some(chatRoom) =>
        val text = Json.obj(
          "id" -> UUID.randomUUID().toString,
          "user" -> "api",
          "text" -> message,
        )
        chatRoom ! SubscribeMessage(text.toString())
        Ok("ok")
      case None =>
        InternalServerError("チャットルームがありません")
    }
  }
}
