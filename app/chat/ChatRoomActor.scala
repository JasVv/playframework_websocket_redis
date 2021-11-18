package chat

import akka.actor.Actor
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import play.api.libs.json.{JsValue, Json}
import play.http.websocket.Message.Ping
import redis.clients.jedis._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ChatRoomActor(implicit roomId: String, materializer: Materializer, ec: ExecutionContext) extends Actor {

  val chatRoom = new ChatRoom
  var userFlows: mutable.Map[String, Flow[ChatMessage, Either[Ping, JsValue], _]] = mutable.Map()

  override def preStart(): Unit = {
    println(s"chatroom create $roomId")
  }

  override def postStop(): Unit = {
    chatRoom.jedisUnsubscribe()
    println(s"chatroom delete $roomId")
  }

  override def receive: Receive = {
    case AddUserMessage(userId, actorFlow) =>
      // ユーザーの登録
      userFlows.put(userId, actorFlow)

    case DeleteUserMessage(userId) =>
      // ユーザーの削除
      userFlows.remove(userId)
      if (userFlows.isEmpty) {
        ChatActorMap.chatRoomActors.remove(roomId)
        context stop self
      }

    case PublishMessage(text) =>
      chatRoom.publish(text)

    case SubscribeMessage(text) =>
      val message: ChatMessage = SendTextMessage(Json.parse(text))
      userFlows.foreach {
        case (_, flow) =>
          flow.runWith(Source.single(message).concat(Source.maybe), Sink.ignore)
      }
  }

  class ChatRoom {
    val id: String = roomId
    val jedis = new JedisCluster(
      new HostAndPort("localhost", 7000),
      BinaryJedisCluster.DEFAULT_TIMEOUT, // connectionTimeout: Int
      BinaryJedisCluster.DEFAULT_TIMEOUT, // soTimeout: Int
      BinaryJedisCluster.DEFAULT_MAX_ATTEMPTS, // maxAttempts: Int
      null, // user: String
      null, // password: String
      null, // clientName: String
      new JedisPoolConfig(), // poolConfig: GenericObjectPoolConfig[Jedis]
      false // ssl: Boolean
    )
    val pubsub: JedisPubSub = new JedisPubSub() {
      override def onSubscribe(channel: String, subscribedChannels: Int): Unit = println(s"onSubscribe $channel")

      override def onUnsubscribe(channel: String, subscribedChannels: Int): Unit = println(s"onUnsubscribe $roomId")

      override def onMessage(channel: String, message: String): Unit = self ! SubscribeMessage(message)
    }

    Future {
      jedis.subscribe(pubsub, roomId)
    }

    def publish(text: String): Unit = {
      val publishJedis = new JedisCluster(
        new HostAndPort("localhost", 7000),
        BinaryJedisCluster.DEFAULT_TIMEOUT, // connectionTimeout: Int
        BinaryJedisCluster.DEFAULT_TIMEOUT, // soTimeout: Int
        BinaryJedisCluster.DEFAULT_MAX_ATTEMPTS, // maxAttempts: Int
        null, // user: String
        null, // password: String
        null, // clientName: String
        new JedisPoolConfig(), // poolConfig: GenericObjectPoolConfig[Jedis]
        false // ssl: Boolean
      )
      publishJedis.publish(roomId, text)
      publishJedis.close()
    }

    def jedisUnsubscribe(): Unit = {
      pubsub.unsubscribe()
      jedis.close()
    }


  }
}
