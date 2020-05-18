package ago.kotrx

import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.json.JsonObject

object ConfigSupport {
  private val configFileStore =
    ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(JsonObject().put("path", "application.yaml"))

  val configRetrieverOptions: ConfigRetrieverOptions = ConfigRetrieverOptions()
    .addStore(configFileStore)

}
