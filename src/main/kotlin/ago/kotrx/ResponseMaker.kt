package ago.kotrx

import io.reactivex.Single
import io.vertx.core.json.Json
import io.vertx.reactivex.ext.web.RoutingContext

object ResponseMaker {

  fun <T> sendResponse(
    context: RoutingContext,
    asyncResult: Single<T>?
  ) {
    asyncResult?.subscribe(
      { r: T -> ok(context, r) },
      { ex: Throwable -> internalError(context, ex.message) }
    )
      ?: internalError(context, "invalid_status")
  }

  private fun <T> ok(context: RoutingContext, content: T?) {
    if (content != null) {
      context.response().setStatusCode(200)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(content!!))

    } else {
      context.response().setStatusCode(404).end()
    }
  }

  private fun internalError(
    context: RoutingContext,
    ex: String?
  ) {
    context.response().setStatusCode(500)
      .putHeader("content-type", "text/plain")
      .end(ex)
  }

}
