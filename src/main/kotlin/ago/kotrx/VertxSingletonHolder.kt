package ago.kotrx

import io.vertx.reactivex.core.Vertx

object VertxSingletonHolder {
  val vertx: Vertx = Vertx.vertx()

}
