@file:Suppress("UnusedMainParameter")

package ago.kotrx

import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx


fun main(args: Array<String>) {
  val config = JsonObject()
    .put("version", "1")
  val options = DeploymentOptions()
    .setConfig(config)

  val vertx: Vertx = VertxSingletonHolder.vertx
  vertx.deployVerticle(ApiVerticle(), options)
}




