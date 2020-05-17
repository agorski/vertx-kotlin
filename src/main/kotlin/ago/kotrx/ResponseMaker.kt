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

  private fun <T> ok(context: RoutingContext, maybeContent: T?) {
    maybeContent?.let { content ->
      if (content == Unit) {
        notFound(context)
      } else {
        context.response().setStatusCode(200)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(content))
      }
    } ?: notFound(context)
  }

  private fun notFound(context: RoutingContext) {
    context.response().setStatusCode(404).end()
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
