package xpct

import scala.concurrent.duration.FiniteDuration

import cats.Applicative
import cats.effect.Timer
import cats.implicits._

final class XpctOps[F[_], Subject](subject: Xp[F, Subject])
{
  def assert[Predicate[_], Target, Output]
  (matcher: Predicate[Target])
  (implicit m: Match[Predicate, Target, Subject, Output], applicative: Applicative[F])
  : Xp[F, Output] =
    subject.flatMap(a => Xp.assert(matcher)(a.pure[F]))

  def retry(times: Int): Xp[F, Subject] =
    Xp.retry(times)(subject)

  def retryEvery(interval: FiniteDuration)(times: Int)(implicit timer: Timer[F]): Xp[F, Subject] =
    Xp.retryEvery(interval)(times)(subject)

  def attempt: Xp[F, Subject] =
    Xp.attempt(subject)

  def equal(other: Subject)(implicit applicative: Applicative[F]): Xp[F, Subject] =
    assert(Match.Equals(other))
}

trait ToXpctOps
{
  implicit def ToXpctOps[F[_], A](subject: Xp[F, A]): XpctOps[F, A] = new XpctOps(subject)
}

object ops
extends ToXpctOps
with ToXpctThunkOps
with XpCombinators
