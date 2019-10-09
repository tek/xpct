package xpct

import cats.effect.IO

final class XpctMust[A, F[_]](io: F[A])
{
  import Match.Not
  import matcher._

  def must[G[_], B, C](b: G[B])(implicit mtch: Match[A, G, B, C]): Xp[F, C] = Xp.assert(io, b)

  def must_==(a: A): Xp[F, A] = must(be(a))

  def mustNot[G[_], B, C](b: G[B])(implicit mtch: Match[A, Not, G[B], C]): Xp[F, C] = Xp.assert(io, not(b))

  def xp: Xp[F, A] = Xp.Thunk(io)
}

trait ToXpctMust
{
  implicit def ToXpctMust[A, F[_]](io: F[A]): XpctMust[A, F] = new XpctMust(io)
}

trait LiftAnyIOMust
{
  implicit def LiftAnyIOMust[A](a: => A): XpctMust[A, IO] = new XpctMust(IO(a))
}
