package ago.kotrx.weather

import ago.kotrx.ConfigSupport
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.reactivex.config.ConfigRetriever
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.healthchecks.HealthChecks
import io.vertx.reactivex.ext.web.client.WebClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory


@ExtendWith(VertxExtension::class)
class WeatherClientTest {

  private val logger = LoggerFactory.getLogger(WeatherClientTest::class.java)

  private lateinit var server: MockWebServer
  private lateinit var configOverride: ConfigStoreOptions

  @AfterEach
  fun stop_web_server() {
    server.shutdown()
  }

  @BeforeEach
  fun start_web_server() {
    server = MockWebServer()
    server.start()

    configOverride = ConfigStoreOptions()
      .setType("json")
      .setConfig(
        json {
          obj(
            "api_weather" to obj(
              "url" to server.hostName,
              "port" to server.port,
              "timeoutMs" to 1000
            )
          )
        }
      )
  }

  @Test
  fun weather_for_city_happy_path(vertx: Vertx, testContext: VertxTestContext) {
    val dispatcher = object : Dispatcher() {

      override fun dispatch(request: RecordedRequest): MockResponse {
        when (request.path) {
          "/api/location/search/?query=London" -> return MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody("""[{"title":"London","location_type":"City","woeid":44418,"latt_long":"51.506321,-0.12714"}]""")

          "/api/location/44418" -> return MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody("""{"consolidated_weather":[]}""")
        }
        return MockResponse().setResponseCode(404)
      }
    }
    server.dispatcher = dispatcher

    val webClient: WebClient = WebClient.create(vertx)
    val config: JsonObject =
      ConfigRetriever.create(vertx, ConfigSupport.configRetrieverOptions.addStore(configOverride)).rxGetConfig()
        .blockingGet()
    val healthChecks: HealthChecks = HealthChecks.create(vertx)
    val weatherClient = WeatherClient(webClient, config, healthChecks)


    val weatherForCity = weatherClient.weatherForCity("London").blockingGet()
    Assertions.assertNotNull(weatherForCity)
    testContext.completeNow()
  }

}
