package chat

import akka.stream.scaladsl.Flow
import play.api.libs.json.JsValue
import play.http.websocket.Message.Ping

sealed abstract class ChatMessage

case class NoActMessage() extends ChatMessage
case class SendTextMessage(text: JsValue) extends ChatMessage

case class AddUserMessage(userId: String, actorFlow: Flow[ChatMessage, Either[Ping, JsValue], _]) extends ChatMessage
case class DeleteUserMessage(userId: String) extends ChatMessage
case class PublishMessage(text: String) extends ChatMessage
case class SubscribeMessage(text: String) extends ChatMessage
