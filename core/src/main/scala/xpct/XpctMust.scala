package xpct

import cats.Applicative
import cats.implicits._

final class XpctMust[F[_]: Applicative, Subject](subject: Xp[F, Subject])
{
  import Match.Not
  import matcher._

  def must[Predicate[_], Target, Output]
  (b: Predicate[Target])
  (implicit m: Match[Predicate, Target, Subject, Output])
  : Xp[F, Output] =
    subject.flatMap(a => Xp.assert(a.pure[F], b))

  def must_==(a: Subject): Xp[F, Subject] =
    must(equal(a))

  def mustNot[Predicate[_], Target, Output]
  (b: Predicate[Target])
  (implicit mtch: Match[Not, Predicate[Target], Subject, Output])
  : Xp[F, Output] =
    subject.flatMap(a => Xp.assert(a.pure[F], not(b)))
}

trait ToXpctMust
{
  implicit def ToXpctMust[F[_]: Applicative, A](subject: Xp[F, A]): XpctMust[F, A] =
    new XpctMust(subject)
}

object must
extends ToXpctMust
with ToXpctThunkMust
