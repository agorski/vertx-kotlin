package ago.kotrx

import arrow.Kind
import arrow.core.Either
import arrow.fx.rx2.ForObservableK
import arrow.fx.rx2.ObservableK
import arrow.fx.rx2.fix
import arrow.fx.rx2.k
import arrow.mtl.EitherTPartialOf
import arrow.mtl.value
import io.reactivex.Observable
import io.reactivex.Single

fun <T> Single<Either<Throwable, T>>.toObservableK(): ObservableK<Either<Throwable, T>> =
  this.toObservable().k()

fun <T> Kind<EitherTPartialOf<Throwable, ForObservableK>, T>.toObservable(): Observable<out Either<Throwable, T>> =
  this.value().fix().observable

inline fun <R> runCatchingEither(block: () -> R): Either<Throwable, R> {
  return try {
    Either.right(block())
  } catch (e: Throwable) {
    Either.left(e)
  }
}
