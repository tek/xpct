package xpct

import cats.kernel.Comparison
import cats.{Eq, Order, Foldable}
import cats.syntax.either._

trait Match[A, G[_], B, C]
{
  def apply(a: A, fb: G[B]): Either[String, C]
}

object Match
{
  case class IsAny[A]()

  object IsAny
  {
    implicit def Match_IsAny[A]: Match[A, IsAny, A, A] =
      new Match[A, IsAny, A, A] {
        def apply(a: A, fb: IsAny[A]): Either[String, A] = {
          Right(a)
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
        def apply(a: A, fb: Not[G[B]]): Either[String, A] = {
          nested(a, fb.value) match {
            case Right(b) => Left(s"$a matched $b in $fb, should have failed")
            case Left(_) => Right(a)
          }
        }
      }
  }

  case class Equals[A](value: A)

  object Equals
  {
    implicit def Match_Equals[A]: Match[A, Equals, A, A] =
      new Match[A, Equals, A, A] {
        def apply(a: A, fb: Equals[A]): Either[String, A] = {
          val b = fb.value
          if (a == b) Right(b)
          else Left(s"$a != $b")
        }
      }
  }

  case class IsSome[A](value: A)

  object IsSome
  {
    implicit def Match_IsSome[A: Eq]: Match[Option[A], IsSome, A, A] =
      new Match[Option[A], IsSome, A, A] {
        def apply(fa: Option[A], fb: IsSome[A]): Either[String, A] = {
          val a1 = fb.value
          fa match {
            case Some(v) =>
              if (Eq[A].eqv(v, a1)) Right(v)
              else Left(s"$v != $a1")
            case None => Left(s"got `None` for $fb")
          }
        }
      }

    implicit def Match_IsSome_Match[A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[Option[A], IsSome, G[B], C] =
      new Match[Option[A], IsSome, G[B], C] {
        def apply(fa: Option[A], fb: IsSome[G[B]]): Either[String, C] = {
          fa match {
            case Some(a) => nested(a, fb.value)
            case None => Left(s"got `None` for $fb")
          }
        }
      }
  }

  case class Compares[A](value: A, comp: PartialFunction[Comparison, Boolean], desc: String)

  object Compares
  {
    implicit def Match_Compares[A: Order]: Match[A, Compares, A, A] =
      new Match[A, Compares, A, A] {
        def apply(a: A, fb: Compares[A]): Either[String, A] = {
          fb.comp.lift(Order[A].comparison(a, fb.value)) match {
            case Some(_) => Right(a)
            case None => Left(s"$a is not ${fb.desc} ${fb.value}")
          }
        }
      }
  }

  case class Contains[A](value: A)

  object Contains
  {
    implicit def Match_Contains_mono[F[_]: Foldable, A: Eq]: Match[F[A], Contains, A, A] =
      new Match[F[A], Contains, A, A] {
        def apply(fa: F[A], fb: Contains[A]): Either[String, A] = {
          Foldable[F].find(fa)(Eq[A].eqv(_, fb.value)) match {
            case Some(a) => Right(a)
            case None => Left(s"$fa does not contain ${fb.value}")
          }
        }
      }

    implicit def Match_Contains_Match[F[_]: Foldable, A, G[_], B, C]
    (implicit nested: Match[A, G, B, C])
    : Match[F[A], Contains, G[B], C] =
      new Match[F[A], Contains, G[B], C] {
        def apply(fa: F[A], fb: Contains[G[B]]): Either[String, C] = {
          Foldable[F]
            .foldLeft(fa, Either.left[List[String], C](Nil)) {
              case (Right(a), _) => Right(a)
              case (Left(err), a) =>
                nested(a, fb.value) match {
                  case Right(a) => Right(a)
                  case Left(e) => Left(e :: err)
                }
            }
            match {
              case Right(a) => Right(a)
              case Left(Nil) => Left(s"empty $fa cannot contain ${fb.value}")
              case Left(es) => Left(s"no element in $fa matched:\n${es.mkString("; ")}")
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
