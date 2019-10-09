package xpct

final class XpctThunkMust[F[_], Subject](subject: F[Subject])
{
  import Match.Not
  import matcher._

  def must[Predicate[_], Target, Output]
  (b: Predicate[Target])
  (implicit m: Match[Predicate, Target, Subject, Output])
  : Xp[F, Output] =
    Xp.assert(subject, b)

  def must_==(a: Subject): Xp[F, Subject] = must(equal(a))

  def mustNot[Predicate[_], Target, Output]
  (b: Predicate[Target])
  (implicit mtch: Match[Not, Predicate[Target], Subject, Output])
  : Xp[F, Output] = Xp.assert(subject, not(b))
}

trait ToXpctThunkMust
{
  implicit def ToXpctThunkMust[F[_], A](subject: F[A]): XpctThunkMust[F, A] =
    new XpctThunkMust(subject)
}
