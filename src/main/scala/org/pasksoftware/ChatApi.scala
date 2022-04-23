package org.pasksoftware

import akka.Done
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.pasksoftware.Chat.{AddNewUser, ChatCommand, ProcessMessage}
import org.pasksoftware.ChatApi.{ChatCreated, StartChat, startChatDecoder}
import org.pasksoftware.ChatsStore.{AddNewChat, GetChatMeta, StoreCommand}
import org.slf4j.Logger

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

class ChatApi(service: ChatService, store: ActorRef[StoreCommand], logger: Logger)(implicit val system: ActorSystem[_]) {

  private implicit val timeout: Timeout = Timeout(2.seconds)
  private implicit val ec: ExecutionContextExecutor = system.executionContext

  val routes: Route = {
    pathPrefix(service.path / "chats") {
      concat(pathEnd {
        post {
          entity(as[StartChat]) { start =>
            val senderId = start.sender.id.toString
            val receiverId = start.receiver.id.toString
            logger.info(s"Starting new chat sender: $senderId, receiver: $receiverId")
            val eventualCreated =
              store
                .ask(ref => AddNewChat(start.sender, start.receiver, ref))
                .map(id => {
                  val chatLinks = service.generateChatLinks(id, senderId, receiverId)
                  ChatCreated(id, chatLinks._1, chatLinks._2)
                })
            onSuccess(eventualCreated) { c =>
              complete(StatusCodes.Created, c)
            }
          }
        }
      }, path(IntNumber / "messages" / Segment) { (id, userId) =>
        onSuccess(store.ask(ref => GetChatMeta(id, userId, ref))) {
          case Some(meta) => handleWebSocketMessages(websocketFlow(meta.userName, meta.ref))
          case None => complete(StatusCodes.NotFound)
        }
      })
    }
  }

  def websocketFlow(userName: String, chatActor: ActorRef[ChatCommand]): Flow[Message, Message, Any] = {
    val source: Source[TextMessage, Unit] =
      ActorSource.actorRef[String](PartialFunction.empty, PartialFunction.empty, 5, OverflowStrategy.fail)
        .map[TextMessage](TextMessage(_))
        .mapMaterializedValue(sourceRef => chatActor ! AddNewUser(sourceRef))

    val sink: Sink[Message, Future[Done]] = Sink
      .foreach[Message] {
        case tm: TextMessage =>
          chatActor ! ProcessMessage(userName, tm.getStrictText)
        case _ =>
          logger.warn(s"User with id: '{}', send unsupported message", userName)
      }

    Flow.fromSinkAndSource(sink, source)
  }
}

object ChatApi {

  case class StartChat(sender: User, receiver: User)
  case class User(id: UUID, name: String)
  case class ChatCreated(chatId: Int, senderChatLink: String, receiverChatLink: String)

  implicit val startChatDecoder: Decoder[StartChat] = deriveDecoder
  implicit val startChatEncoder: Encoder[StartChat] = deriveEncoder
  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val userEncoder: Encoder[User] = deriveEncoder
  implicit val chatCreatedDecoder: Decoder[ChatCreated] = deriveDecoder
  implicit val chatCreatedEncoder: Encoder[ChatCreated] = deriveEncoder
}
