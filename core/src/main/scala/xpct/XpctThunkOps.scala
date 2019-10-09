package xpct

final class XpctThunkOps[F[_], Subject](subject: F[Subject])
{
  def xp: Xp[F, Subject] = Xp.Thunk(subject)

  def assert[Predicate[_], Target, Output]
  (matcher: Predicate[Target])
  (implicit m: Match[Predicate, Target, Subject, Output])
  : Xp[F, Output] =
    Xp.assert(subject, matcher)

  def extract[Output]
  (f: PartialFunction[Subject, Output])
  (implicit m: Match[Match.Extract[Subject, ?], Output, Subject, Output])
  : Xp[F, Output] =
    assert(matcher.extract(f))
}

trait ToXpctThunkOps
{
  implicit def ToXpctThunkOps[F[_], A](subject: F[A]): XpctThunkOps[F, A] = new XpctThunkOps(subject)
}
