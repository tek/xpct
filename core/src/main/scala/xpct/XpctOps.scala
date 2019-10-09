package xpct

import cats.Applicative
import cats.implicits._

final class XpctOps[F[_], Subject](subject: Xp[F, Subject])
{
  def assert[Predicate[_], Target, Output]
  (matcher: Predicate[Target])
  (implicit m: Match[Predicate, Target, Subject, Output], applicative: Applicative[F])
  : Xp[F, Output] =
    subject.flatMap(a => Xp.assert(a.pure[F], matcher))
}

trait ToXpctOps
{
  implicit def ToXpctOps[F[_], A](subject: Xp[F, A]): XpctOps[F, A] = new XpctOps(subject)
}

object ops
extends ToXpctOps
with ToXpctThunkOps
