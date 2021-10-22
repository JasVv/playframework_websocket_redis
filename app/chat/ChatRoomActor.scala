package chat

import akka.actor.Actor
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import play.api.libs.json.{JsValue, Json}
import play.http.websocket.Message.Ping
import redis.clients.jedis.{Jedis, JedisPubSub}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ChatRoomActor(implicit roomId: String, materializer: Materializer, ec: ExecutionContext) extends Actor {

  val chatRoom = new ChatRoom
  var userFlows: mutable.Map[String, Flow[ChatMessage, Either[Ping, JsValue], _]] = mutable.Map()

  override def preStart(): Unit = {
    println("chatroom create")
  }

  override def postStop(): Unit = {
    chatRoom.jedisUnsubscribe()
    println("chatroom delete")
  }

  override def receive: Receive = {
    case AddUserMessage(userId, actorFlow) =>
      // ユーザーの登録
      userFlows.put(userId, actorFlow)

    case DeleteUserMessage(userId) =>
      // ユーザーの削除
      userFlows.remove(userId)
      // if (userFlows.isEmpty) context stop self

    case PublishMessage(text) =>
      chatRoom.publish(text)

    case SubscribeMessage(text) =>
      val message: ChatMessage = SendTextMessage(Json.parse(text))
      userFlows.foreach {
        case (_, flow) =>
          flow.runWith(Source.single(message).concat(Source.maybe), Sink.ignore)
      }
  }

  class ChatRoom extends JedisPubSub{
    val id: String = roomId
    val jedis = new Jedis("localhost", 6379)
    Future {
      jedis.subscribe(this, roomId)
    }

    def publish(text: String): Unit = {
      val publishJedis = new Jedis("localhost", 6379)
      publishJedis.publish(roomId, text)
      publishJedis.close()
    }

    def jedisUnsubscribe(): Unit = {
      unsubscribe()
      jedis.close()
    }

    override def onSubscribe(channel: String, subscribedChannels: Int): Unit = println(s"onSubscribe: $channel")

    override def onUnsubscribe(channel: String, subscribedChannels: Int): Unit = println("onUnsubscribe")

    override def onMessage(channel: String, message: String): Unit = self ! SubscribeMessage(message)
  }
}
