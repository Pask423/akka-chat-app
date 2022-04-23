package org.pasksoftware

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable

object Chat {

  sealed trait ChatCommand
  final case class ProcessMessage(sender: String, content: String) extends ChatCommand
  final case class AddNewUser(ref: ActorRef[String]) extends ChatCommand

  def apply(): Behavior[ChatCommand] =
    Behaviors.setup { _ =>
      var participants = List.empty[ActorRef[String]]
      val messageQueue = mutable.Queue.empty[String]
      Behaviors.receiveMessage[ChatCommand] {
        case ProcessMessage(sender, content) =>
          val message = s"$sender: $content"
          messageQueue.enqueue(message)
          participants.foreach(ref => ref ! message)
          Behaviors.same
        case AddNewUser(ref) =>
          participants = participants.appended(ref)
          messageQueue.foreach(m => ref ! m)
          Behaviors.same
      }
    }
}
