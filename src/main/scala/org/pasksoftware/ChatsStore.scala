package org.pasksoftware

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.pasksoftware.Chat.ChatCommand
import org.pasksoftware.ChatApi.User

import scala.collection.mutable

object ChatsStore {

  sealed trait StoreCommand
  final case class AddNewChat(sender: User, receiver: User, replyTo: ActorRef[Int]) extends StoreCommand
  final case class GetChatMeta(chatId: Int, userName: String, replyTo: ActorRef[Option[GetChatMetaResponse]]) extends StoreCommand

  final case class GetChatMetaResponse(userName: String, ref: ActorRef[ChatCommand])

  private var sequence = 0
  private val store = mutable.Map.empty[Int, ChatMetadata]

  private case class ChatMetadata(participants: Map[String, User], ref: ActorRef[ChatCommand]) {
    def containUserId(userId: String): Boolean =
      participants.contains(userId)
  }

  def apply(): Behavior[StoreCommand] =
    Behaviors.setup(context => {
      Behaviors.receiveMessage {
        case AddNewChat(sender, receiver, replyTo) =>
          sequence += 1
          val newChat: ActorRef[ChatCommand] = context.spawn(Chat(), s"Chat$sequence")
          val participants = Map(sender.id.toString -> sender, receiver.id.toString -> receiver)
          val metadata = ChatMetadata(participants, newChat)
          store.put(sequence, metadata)
          replyTo ! sequence
          Behaviors.same
        case GetChatMeta(chatId, userId, replyTo) =>
          val chatRef = store
            .get(chatId)
            .filter(_.containUserId(userId))
            .flatMap(meta =>
              meta.participants
                .get(userId)
                .map(user => GetChatMetaResponse(user.name, meta.ref))
            )
          replyTo ! chatRef
          Behaviors.same
      }
    })
}
