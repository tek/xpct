package xpct

import cats.{Eq, Foldable, Order}
import cats.kernel.Comparison
import cats.syntax.either._

trait Match[Predicate[_], Target, Subject, Output]
{
  def apply(a: Subject, fb: Predicate[Target]): AssertResult[Output]
}

object Match
{
  case class IsAny[A]()

  object IsAny
  {
    implicit def Match_IsAny[A]: Match[IsAny, A, A, A] =
      new Match[IsAny, A, A, A] {
        def apply(a: A, fb: IsAny[A]): AssertResult[A] = {
          AssertResult.Success(a, XpSuccess(s"$a exists"))
        }
      }
  }

  case class Not[A](value: A)

  object Not
  {
    implicit def Match_Not[Predicate[_], Target, Subject, A]
    (implicit nested: Match[Predicate, Target, Subject, A])
    : Match[Not, Predicate[Target], Subject, Subject] =
      new Match[Not, Predicate[Target], Subject, Subject] {
        def apply(a: Subject, fb: Not[Predicate[Target]]): AssertResult[Subject] = {
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
    implicit def Match_Equals[A]: Match[Equals, A, A, A] =
      new Match[Equals, A, A, A] {
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
    implicit def Match_IsSome[A: Eq]: Match[IsSome, A, Option[A], A] =
      new Match[IsSome, A, Option[A], A] {
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

    implicit def Match_IsSome_Match[Predicate[_], Target, Subject, Output]
    (implicit nested: Match[Predicate, Target, Subject, Output])
    : Match[IsSome, Predicate[Target], Option[Subject], Output] =
      new Match[IsSome, Predicate[Target], Option[Subject], Output] {
        def apply(fa: Option[Subject], fb: IsSome[Predicate[Target]]): AssertResult[Output] = {
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
    implicit def Match_Compares[A: Order]: Match[Compares, A, A, A] =
      new Match[Compares, A, A, A] {
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
    implicit def Match_Contains_mono[T[_]: Foldable, A: Eq]: Match[Contains, A, T[A], A] =
      new Match[Contains, A, T[A], A] {
        def apply(fa: T[A], fb: Contains[A]): AssertResult[A] = {
          Foldable[T].find(fa)(Eq[A].eqv(_, fb.value)) match {
            case Some(a) => AssertResult.success(s"contains ${fb.value}")(a)
            case None => AssertResult.failure(s"does not contain ${fb.value}")
          }
        }
      }

    implicit def Match_Contains_Match[T[_]: Foldable, Predicate[_], Target, Subject, Output]
    (implicit nested: Match[Predicate, Target, Subject, Output])
    : Match[Contains, Predicate[Target], T[Subject], Output] =
      new Match[Contains, Predicate[Target], T[Subject], Output] {
        def apply(fa: T[Subject], fb: Contains[Predicate[Target]]): AssertResult[Output] = {
          Foldable[T]
            .foldLeft(fa, Either.left[List[XpFailure], (Output, XpSuccess)](Nil)) {
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

  case class Extract[A, B](f: PartialFunction[A, B])

  object Extract
  {
    implicit def Match_Extract[A, B]: Match[Extract[A, ?], B, A, B] =
      new Match[Extract[A, ?], B, A, B] {
        def apply(a: A, fb: Extract[A, B]): AssertResult[B] = {
          fb.f.lift(a) match {
            case Some(b) => AssertResult.success(s"extracted $b from $a")(b)
            case None => AssertResult.failure(s"couldn't extract from $a")
          }
        }
      }
  }
}

trait MatcherCons
{
  import Match._

  def equal[A](a: A): Equals[A] = Equals(a)

  def not[A](a: A): Not[A] = Not(a)

  def beSome[A](a: A): IsSome[A] = IsSome(a)

  def beASome[A]: IsSome[IsAny[A]] = IsSome(IsAny[A]())

  def gt[A](a: A): Compares[A] =
    Compares(a, { case Comparison.GreaterThan => true }, ">")

  def gte[A](a: A): Compares[A] =
    Compares(a, { case Comparison.GreaterThan | Comparison.EqualTo => true }, ">=")

  def contain[A](a: A): Contains[A] = Contains(a)

  def extract[A, B](f: PartialFunction[A, B]): Extract[A, B] = Extract(f)
}

object matcher
extends MatcherCons
