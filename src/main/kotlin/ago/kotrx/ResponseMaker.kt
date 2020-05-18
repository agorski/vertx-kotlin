package ago.kotrx

import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN
import io.reactivex.Single
import io.vertx.core.MultiMap
import io.vertx.core.json.Json
import io.vertx.reactivex.core.http.HttpServerResponse
import io.vertx.reactivex.ext.web.RoutingContext
import java.util.*

object ResponseMaker {

  @Suppress("MemberVisibilityCanBePrivate")
  val jsonHeaders: MultiMap = MultiMap.caseInsensitiveMultiMap()
    .add(CONTENT_TYPE, "$APPLICATION_JSON; ${HttpHeaderValues.CHARSET}=utf-8")

  val textHeaders: MultiMap = MultiMap.caseInsensitiveMultiMap()
    .add(CONTENT_TYPE, "$TEXT_PLAIN; ${HttpHeaderValues.CHARSET}=utf-8")

  fun <T> sendResponse(
    context: RoutingContext,
    asyncResult: Single<T>,
    headers: MultiMap = jsonHeaders
  ) {
    asyncResult.subscribe(
      { r: T -> ok(context, r, headers) },
      { ex: Throwable -> internalError(context, ex) }
    )
  }

  private fun <T> ok(context: RoutingContext, maybeContent: T?, headers: MultiMap) {
    maybeContent?.let { content ->
      if (content == Unit) {
        notFound(context)
      } else {
        addHeadersToResponse(context.response(), headers)
          .setStatusCode(200)
          .end(convertToStringBasedOnContentType(content, headers))
      }
    } ?: notFound(context)
  }

  private fun notFound(context: RoutingContext) {
    context.response().setStatusCode(404).end()
  }

  private fun internalError(
    context: RoutingContext,
    ex: Throwable
  ) {
    context.response().setStatusCode(500)
      .putHeader(CONTENT_TYPE, TEXT_PLAIN)
      .end(ex.toString())
  }

  private fun addHeadersToResponse(response: HttpServerResponse, headers: MultiMap): HttpServerResponse {
    headers.forEach { e ->
      response.putHeader(e.key, e.value)
    }
    return response
  }

  private fun <T> convertToStringBasedOnContentType(content: T, headers: MultiMap): String {
    return Optional
      .ofNullable(headers.get(CONTENT_TYPE))
      .filter { v -> v.contains(APPLICATION_JSON, true) }
      .map { Json.encodePrettily(content) }
      .orElse(content.toString())

  }

}
