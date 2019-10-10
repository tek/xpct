package xpct

final class XpctThunkMust[F[_], Subject](subject: F[Subject])
{
  import Match.Not
  import matcher._

  def must[Predicate[_], Target, Output]
  (b: Predicate[Target])
  (implicit m: Match[Predicate, Target, Subject, Output])
  : Xp[F, Output] =
    Xp.assert(b)(subject)

  def must_==(a: Subject): Xp[F, Subject] = must(equal(a))

  def mustNot[Predicate[_], Target, Output]
  (b: Predicate[Target])
  (implicit mtch: Match[Not, Predicate[Target], Subject, Output])
  : Xp[F, Output] = Xp.assert(not(b))(subject)
}

trait ToXpctThunkMust
{
  implicit def ToXpctThunkMust[F[_], A](subject: F[A]): XpctThunkMust[F, A] =
    new XpctThunkMust(subject)
}
