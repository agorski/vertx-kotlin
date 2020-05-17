@file:Suppress("UnusedMainParameter")

package ago.kotrx

import ago.kotrx.weather.Weather
import ago.kotrx.weather.WeatherClient
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.reactivex.config.ConfigRetriever
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.client.WebClient
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module


fun main(args: Array<String>) {
  val vertx: Vertx = Vertx.vertx()
  val file =
    ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(JsonObject().put("path", "application.yaml"))

  val config:JsonObject = ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(file)).rxGetConfig().blockingGet()
  println("> server port ${config.getInteger("port")}")

  startKoin {
    printLogger(Level.INFO)
    // declare properties from given map
    // properties( /* properties map */)
    // load properties from koin.properties file or given file name
    // fileProperties()

    // load properties from environment
    environmentProperties()
    modules(module {
      single { WeatherClient(WebClient.create(vertx), config) }
      single { Weather(get(), vertx) }
      single { config }
    })
  }

  val options = DeploymentOptions()
    .setConfig(config)

  vertx.deployVerticle(ApiVerticle(), options)
}




