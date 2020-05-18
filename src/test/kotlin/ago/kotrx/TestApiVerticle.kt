package ago.kotrx

import ago.kotrx.weather.Weather
import ago.kotrx.weather.WeatherClient
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.Status
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.healthchecks.HealthChecks
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
  private lateinit var healthChecks: HealthChecks

  @BeforeEach
  fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
    val apiMock = mock(WeatherClient::class.java)
    port = TestUtils.getFreePort()
    val config = JsonObject(mapOf("port" to port))
    healthChecks = HealthChecks
      .create(vertx)
      .register("app") { future -> future.complete(Status.OK()) }
    startKoin {
      modules(
        module {
          single { Weather(apiMock, vertx) }
          single { WeatherClient(WebClient.create(vertx), config, healthChecks) }
          single { healthChecks }
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

    assertEquals(HttpResponseStatus.OK.code(), statusCode)

    testContext.completeNow()
  }

  @Test
  fun ping_endpoint_respond_with_code_200(vertx: Vertx, testContext: VertxTestContext) {
    val statusCode = WebClient
      .create(vertx)
      .get(port, "localhost", ApiVerticle.endpointPing)
      .rxSend()
      .blockingGet()
      .statusCode()

    assertEquals(HttpResponseStatus.OK.code(), statusCode)

    testContext.completeNow()
  }

  @Test
  fun health_endpoint_respond_with_code_200_if_all_checks_fine(vertx: Vertx, testContext: VertxTestContext) {
    val response = WebClient
      .create(vertx)
      .get(port, "localhost", ApiVerticle.endpointHealth)
      .rxSend()
      .blockingGet()


    assertEquals(HttpResponseStatus.OK.code(), response.statusCode(), "invalid status code")
    assertEquals("UP", response.bodyAsJsonObject().getString("outcome"), "outcome is not UP")
    assertEquals(1, response.bodyAsJsonObject().getJsonArray("checks").size(), "the should be one check")

    testContext.completeNow()
  }

  @Test
  fun health_endpoint_respond_with_code_503_if_check_fails(vertx: Vertx, testContext: VertxTestContext) {
    healthChecks.register("wrong") { future -> future.complete(Status.KO()) }
    val response = WebClient
      .create(vertx)
      .get(port, "localhost", ApiVerticle.endpointHealth)
      .rxSend()
      .blockingGet()
    healthChecks.unregister("wrong")

    assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE.code(), response.statusCode(), "invalid status code")
    assertEquals("DOWN", response.bodyAsJsonObject().getString("outcome"), "outcome is not DOWN")
    testContext.completeNow()
  }
}
