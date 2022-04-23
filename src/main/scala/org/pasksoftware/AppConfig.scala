package org.pasksoftware

case class AppConfig(httpConfig: HttpConfig)
case class HttpConfig(port: Int, host: String) {
  val toPath = s"$host:$port"
}

