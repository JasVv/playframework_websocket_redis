package chat

import akka.actor.ActorRef

import scala.collection.mutable

object ChatActorMap {
  val chatRoomActors: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()
}
