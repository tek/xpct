package xpct

import cats.{Eq, Foldable, Order}
import cats.kernel.Comparison
import cats.syntax.either._

trait Match[A, G[_], B, C]
{
  def apply(a: A, fb: G[B]): AssertResult[C]
}

object Match
{
  case class IsAny[A]()

  object IsAny
  {
    implicit def Match_IsAny[A]: Match[A, IsAny, A, A] =
      new Match[A, IsAny, A, A] {
        def apply(a: A, fb: IsAny[A]): AssertResult[A] = {
          AssertResult.Success(a, XpSuccess(s"$a exists"))
        }
      }
  }

  case class Not[A](value: A)

  object Not
  {
    implicit def Match_Not[A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[A, Not, G[B], A] =
      new Match[A, Not, G[B], A] {
        def apply(a: A, fb: Not[G[B]]): AssertResult[A] = {
          nested(a, fb.value) match {
            case AssertResult.Success(b, XpSuccess(message)) =>
              AssertResult.Failure(XpFailure.Assert(s"$a matched $b in $fb, should have failed ($message)"))
            case AssertResult.Failure(failure) =>
              AssertResult.Success(a, XpSuccess(s"$fb failed for $a ($failure)"))
          }
        }
      }
  }

  case class Equals[A](value: A)

  object Equals
  {
    implicit def Match_Equals[A]: Match[A, Equals, A, A] =
      new Match[A, Equals, A, A] {
        def apply(a: A, fb: Equals[A]): AssertResult[A] = {
          val b = fb.value
          if (a == b) AssertResult.success(s"$a == $b")(b)
          else AssertResult.failure(s"$a /= $b")
        }
      }
  }

  case class IsSome[A](value: A)

  object IsSome
  {
    implicit def Match_IsSome[A: Eq]: Match[Option[A], IsSome, A, A] =
      new Match[Option[A], IsSome, A, A] {
        def apply(fa: Option[A], fb: IsSome[A]): AssertResult[A] = {
          val a1 = fb.value
          fa match {
            case Some(v) =>
              if (Eq[A].eqv(v, a1)) AssertResult.success(s"Some contains $a1")(v)
              else AssertResult.failure(s"is `Some`, but $v != $a1")
            case None => AssertResult.failure(s"is `None`, expected $fb")
          }
        }
      }

    implicit def Match_IsSome_Match[A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[Option[A], IsSome, G[B], C] =
      new Match[Option[A], IsSome, G[B], C] {
        def apply(fa: Option[A], fb: IsSome[G[B]]): AssertResult[C] = {
          fa match {
            case Some(a) => nested(a, fb.value)
            case None => AssertResult.failure(s"is `None`, expected $fb")
          }
        }
      }
  }

  case class Compares[A](value: A, comp: PartialFunction[Comparison, Boolean], desc: String)

  object Compares
  {
    implicit def Match_Compares[A: Order]: Match[A, Compares, A, A] =
      new Match[A, Compares, A, A] {
        def apply(a: A, fb: Compares[A]): AssertResult[A] = {
          fb.comp.lift(Order[A].comparison(a, fb.value)) match {
            case Some(_) => AssertResult.success(s"$a ${fb.desc} ${fb.value}")(a)
            case None => AssertResult.failure(s"$a is not ${fb.desc} ${fb.value}")
          }
        }
      }
  }

  case class Contains[A](value: A)

  object Contains
  {
    implicit def Match_Contains_mono[F[_]: Foldable, A: Eq]: Match[F[A], Contains, A, A] =
      new Match[F[A], Contains, A, A] {
        def apply(fa: F[A], fb: Contains[A]): AssertResult[A] = {
          Foldable[F].find(fa)(Eq[A].eqv(_, fb.value)) match {
            case Some(a) => AssertResult.success(s"contains ${fb.value}")(a)
            case None => AssertResult.failure(s"does not contain ${fb.value}")
          }
        }
      }

    implicit def Match_Contains_Match[F[_]: Foldable, A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[F[A], Contains, G[B], C] =
      new Match[F[A], Contains, G[B], C] {
        def apply(fa: F[A], fb: Contains[G[B]]): AssertResult[C] = {
          Foldable[F]
            .foldLeft(fa, Either.left[List[XpFailure], (C, XpSuccess)](Nil)) {
              case (Right(a), _) => Right(a)
              case (Left(err), a) =>
                nested(a, fb.value) match {
                  case AssertResult.Success(c, success) => Right((c, success))
                  case AssertResult.Failure(e) => Left(e :: err)
                }
            }
            match {
              case Right((c, success)) => AssertResult.success(s"contains a match for $fb: ${success.message}")(c)
              case Left(Nil) => AssertResult.failure(s"empty $fa cannot contain ${fb.value}")
              case Left(es) => AssertResult.failure(s"no element in $fa matched:\n${es.mkString("; ")}")
            }
        }
      }
  }
}

trait MatcherCons
{
  import Match._

  def be[A](a: A): Equals[A] = Equals(a)

  def not[A](a: A): Not[A] = Not(a)

  def beSome[A](a: A): IsSome[A] = IsSome(a)

  def beASome[A]: IsSome[IsAny[A]] = IsSome(IsAny[A]())

  def be_>=[A](a: A): Compares[A] =
    Compares(a, { case Comparison.GreaterThan | Comparison.EqualTo => true }, ">=")

  def contain[A](a: A): Contains[A] = Contains(a)
}

object matcher
extends MatcherCons
