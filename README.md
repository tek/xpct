# xpct
This package provides an algebra for the abstraction of simultaneous assertion of conditions and extraction of a value
from heterogeneous effects, all contained in a computation effect.

In more concrete terms, you can sequence `IO`s in a for-comprehension, adding a spec expectation to each step, while
extracting values contained in `Option`s or `Either`s through typeclass based matchers:

```scala
for {
  a <- IO.pure(Either.right("test")) must contain("test")
  b <- IO.pure(Option(5)) must beSome(be_>=(2))
  _ <- IO.pure(s"$a $b") must_== "test 5"
} yield ()

```

# Module IDs
```sbt
libraryDependencies ++= List(
  "io.tryp" %% "xpct-specs2" % "0.1.1",
  "io.tryp" %% "xpct-fs2" % "0.1.1"
)
```

# Features
* [typeclass based matchers](#matching-and-extracting)
* [monadic extraction of tested values](#matching-and-extracting)
* [arbitrary matcher nesting](#matching-and-extracting)
* [parameterized IO for the main effect](#io-and-retrying)
* [transparent sleep/retry mechanism](#io-and-retrying)
* [integration with spec frameworks](#spec-frameworks)
* [cats] based

# Matching and extracting

Matches are based on the typeclass `Match`, where `G[_]` is an arbitrary data type that represents a specific condition,
like `IsSome[Int](5)` (here `B` is `Int`):

```scala
trait Match[A, G[_], B, C]
{
  def apply(a: A, fb: G[B]): Either[String, C]
}
```

`A` is the expectable value; matches can be performed on differing types, producing a third type `C` that is extracted
monadically.
When a `G[B]` value is passed to the implicit `must` method on the expectable, an `Xpct` instance is produced, which
uses the `C` value returned from `Match.apply` for monadic composition, allowing to use the expectation in a
for-comprehension regardless of the type of `A`.

Nesting matchers is a mechanism that is implemented in an ad-hoc way in common spec frameworks. With **xpct**,
a separate instance of `Match` can be defined that has another matcher type as its `B`, allowing arbitrary nesting.

# IO and retrying
When testing asynchronous programs, especially UIs, it is not unusual to wait for a condition to become fulfilled.
In frameworks like **specs2** and **scalatest**, this feature is implemented as a special case with severe limitations
on composability.
**xpct** treats retrying as a first class operation, allowing to retry a sequence of expectations with the same
semantics as strict operations:

```scala
for {
  text <- {
    for {
      elem <- getUiElement(5) must beASome[Text]
      text <- IO.pure(elem) must containText("Cancel")
    } yield text
  }.retryEvery(10, 100.milliseconds)
  _ <- setUiElementText(6, text) must beRight
} yield ()
```

Aside from `cats.effect.IO`, arbitrary async effects can be used, as long as they implement the typeclass `EvalXpct`:

```scala
trait EvalXpct[F[_]]
{
  def sync[A](fa: F[A]): A
}
```

For the retry operation, an additional typeclass instance is required:

```scala
trait Sleep[F[_]]
{
  def sleep(d: FiniteDuration): F[Unit]
}
```

# Spec frameworks

## specs2
For [specs2] integration, the `xpct-specs2` package contains an instance of `AsResult[Xpct]`, which is sufficient for
automatic conversion of `Xpct` values to specs2 `Fragment`s.

A convenience trait `XpctSpec` is provided, which includes the implicit conversion to the extension class with `must`
methods.

## scalatest
For [scalatest] integration, the `xpct-scalatest` package contains the trait `XpctSpec`, providing a helper function
`xpct` that converts an `Xpct` to a `TestFailedException`.

## utest
For [utest] integration, the `xpct-utest` package contains the trait `XpctSpec`, providing a helper function `xpct` that
converts an `Xpct` to an exception.


[cats]: https://github.com/typelevel/cats
[specs2]: https://github.com/etorreborre/specs2
[scalatest]: https://github.com/scalatest/scalatest
[utest]: https://github.com/lihaoyi/utest
