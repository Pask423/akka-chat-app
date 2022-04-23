package org.pasksoftware

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher

import scala.annotation.tailrec

class ChatService(contextPath: List[String], httpConfig: HttpConfig) {

  private val apiPath = contextPath.mkString("/")

  val path: PathMatcher[Unit] = toPath(contextPath, PathMatcher(""))

  @tailrec
  private def toPath(l: List[String], pathMatcher: PathMatcher[Unit]): PathMatcher[Unit] = {
    l match {
      case x :: Nil => toPath(Nil, pathMatcher.append(x))
      case x :: tail => toPath(tail, pathMatcher.append(x / ""))
      case Nil => pathMatcher
    }
  }

  def generateChatLinks(chatId: Int, senderId: String, receiverId: String): (String, String) = {
    val chatPath = s"ws://${httpConfig.toPath}/$apiPath/chats/$chatId/messages"
    (s"$chatPath/$senderId", s"$chatPath/$receiverId")
  }

}
