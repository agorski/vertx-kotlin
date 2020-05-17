package ago.kotrx

import ago.kotrx.weather.Weather
import ago.kotrx.weather.WeatherClient
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.client.WebClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock


@ExtendWith(VertxExtension::class)
class TestApiVerticle {
  private var port = 8890


  @BeforeEach
  fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
    val apiMock = mock(WeatherClient::class.java)
    port = TestUtils.getFreePort()
    val config = JsonObject(mapOf("port" to port))
    startKoin {
      modules(
        module {
          single { Weather(apiMock, vertx) }
          single { WeatherClient(WebClient.create(vertx), config) }
          single { config }
        })
    }
    vertx.deployVerticle(ApiVerticle(), testContext.succeeding<String> { _ -> testContext.completeNow() })
  }

  @AfterEach
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun default_endpoint_respond_with_code_200(vertx: Vertx, testContext: VertxTestContext) {
    val statusCode = WebClient
      .create(vertx)
      .get(port, "localhost", "/")
      .rxSend()
      .blockingGet()
      .statusCode()

    assertEquals(200, statusCode)

    testContext.completeNow()
  }
}
