package org.pasksoftware

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import org.slf4j.Logger

import scala.util.{Failure, Success}

object Server {

  def start(routes: Route, config: HttpConfig, logger: Logger)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val bindingFuture = Http()
      .newServerAt(config.host, config.port)
      .bind(routes)
    bindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        logger.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
