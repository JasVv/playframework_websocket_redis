package chat

import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString
import play.api.libs.json.Json
import play.http.websocket.Message.Ping

import scala.concurrent.{ExecutionContext, Future}

/**
 * WebSocketでのやりとりをするActor。1接続ごとに1つ生成される。
 * 生成されたUserActorはいずれかの [[ChatRoomActor]] に所属する。
 * ユーザ自身の情報に加え、クライアントとのやりとりする [[out: ActorRef]] と、自身の所属するChatRoomActorの [[chatRoom: ActorRef]] を持つ
 */
class UserActor(out: ActorRef, chatRoom: ActorRef, userId: String, ec: ExecutionContext) extends Actor {
  var isClosed = false

  override def receive: Receive = {
    case NoActMessage() =>
    // 前のFlowからのinputを捨てるためのメッセージ。何もしない

    case SendTextMessage(text) =>
      // ユーザーにテキストをpushするメッセージ
      val jsonMessage = Json.toJson(text)
      out ! Right(jsonMessage)
  }

  override def preStart(): Unit = {
    ping()
  }

  // WebSocketが切断された場合はpostStopが呼ばれる。postStopは所属ChatRoomにメッセージを投げ、自身への参照を削除させる。
  override def postStop(): Unit = {
    isClosed = true
    chatRoom ! DeleteUserMessage(userId)
  }

  private def ping(): Unit = Future {
    while (!isClosed) {
      out ! Left(new Ping(ByteString(Array[Byte](8, 1, 8, 1))))
      Thread.sleep(2000)
    }
  }(ec)
}

object UserActor {
  def props(out: ActorRef, chatRoom: ActorRef, userId: String, ec: ExecutionContext): Props = Props(new UserActor(out, chatRoom, userId, ec))
}
