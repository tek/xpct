package xpct

import cats.MonadError
import cats.effect.IO

class XpctMust[A, F[_]](io: F[A])
{
  import Match.Not
  import matcher._

  def must_==(a: A): Xpct[F, A] = must(be(a))

  def must[G[_], B, C](b: G[B])(implicit mtch: Match[A, G, B, C]): Xpct[F, C] = Xpct.Strict(io, b, mtch)

  def mustNot[G[_], B, C](b: G[B])(implicit mtch: Match[A, Not, G[B], C]): Xpct[F, C] = Xpct.Strict(io, not(b), mtch)

  def xp: Xpct[F, A] = Xpct.Value(io)
}

trait ToXpctMust
{
  implicit def ToXpctMust[A, F[_]](io: F[A])(implicit ME: MonadError[F, Throwable]): XpctMust[A, F] = new XpctMust(io)
}

trait LiftAnyIOMust
{
  implicit def LiftAnyIOMust[A](a: => A): XpctMust[A, IO] = new XpctMust(IO(a))
}
