package ago.kotrx

import arrow.core.Either

fun <T> Result<T>.toEither(): Either<Throwable, T> =
  this.fold(
    { r -> Either.right(r!!) }, { e -> Either.left(e) }
  )
