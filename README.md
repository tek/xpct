# About
**xpct** provides an algebra that abstracts assertion of conditions and extraction of values from heterogeneous data
types contained in a computation effect.

In more concrete terms, you can sequence `IO`s in a for-comprehension, adding an expectation to each step, while
extracting values contained in `Option`s or `Either`s through typeclass based matchers:

```scala
import cats.implicits._

for {
  a <- IO.pure(Either.right("test")) must contain("test")
  b <- IO.pure(Option(5)) must beSome(be_>=(2))
  _ <- IO.pure(s"$a $b") must_== "test 5"
} yield ()

```

# Module IDs
```sbt
"io.tryp" %% "xpct-core" % "0.2.0"
"io.tryp" %% "xpct-klk" % "0.2.0"
"io.tryp" %% "xpct-specs2" % "0.2.0"
"io.tryp" %% "xpct-scalatest" % "0.2.0"
"io.tryp" %% "xpct-utest" % "0.2.0"
```

# Features
* [typeclass based matchers](#matching-and-extracting)
* [monadic extraction of tested values](#matching-and-extracting)
* [arbitrary matcher nesting](#matching-and-extracting)
* [parameterized IO for the main effect](#io-and-retrying)
* [transparent sleep/retry mechanism](#io-and-retrying)
* [integration with spec frameworks](#test-frameworks)
* [cats-effect] based

# Matching and Extracting

Matches are based on the typeclass `Match`, where `Predicate[_]` is an arbitrary data type that represents a specific
condition, like `IsSome[Int](5)` (here `Target` is `Int`):

```scala
trait Match[Predicate[_], Target, Subject, Output]
{
  def apply(a: Subject, fb: Predicate[Target]): AssertResult[Output]
}
```

`Subject` is the expectable value; matches can be performed on differing types, producing a third type `Output` that is
extracted monadically.
When a `Predicate[Target]` value is passed to the implicit `must` method on the expectable, an `Xp` value is
produced, which uses the `Output` value returned from `Match.apply` for monadic composition, allowing you to use the
expectation in a for-comprehension regardless of the type of `Subject`.

Nesting matchers is a mechanism that is implemented in an ad-hoc way in common spec frameworks. With **xpct**,
a separate instance of `Match` can be defined that has another matcher type as its `Target`, allowing arbitrary nesting.

# Included Matchers and Modifiers

All matchers and modifiers can be chained, i.e. applied to both `IO` and `Xp[IO, *]`.

## Match Types

Alternative syntaxes are available for matching:

### Implicit Methods

```scala
IO(1).must(beSome(1)).retryEvery(100.milli)(30)
IO(1).assert(beSome(1)).attempt.retry(5)
```

### Combinators

```scala
retryEvery(100.milli)(30)(assert(beSome(1))(IO(1)))
retry(5)(attempt(assert(beSome(1))(IO(1))))
```

# IO and Retrying
When testing asynchronous programs, especially UIs, it is not unusual to wait for a condition to become fulfilled.
In frameworks like **[specs2]** and **[scalatest]**, this feature is implemented as a special case with severe limitations
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

Aside from `cats.effect.IO`, arbitrary async effects can be used, as long as they implement the typeclass `EvalXp`:

```scala
trait EvalXp[F[_]]
{
  def apply[A](fa: F[A]): A
}
```

For the retry operation, an instance of `cats.effect.Timer[F]` is required.

# Test Frameworks

## [kallikrein]
**kallikrein** integration is the most seamless one, since it also focuses on `IO` programs.

The `xpct-klk` package contains instances of `Compile[Xp]` and `TestResult[XpResult]`.
Either import the package `xpct.klk._`, mix in `XpctKlk` or subclass `XpctKlkTest[F]`.

## [specs2]
The `xpct-specs2` package contains an instance of `AsResult[Xpct]`, which is sufficient for automatic conversion of
`Xpct` values to specs2 `Fragment`s.

A convenience trait `XpctSpec` is provided, which includes the implicit conversion to the extension class with `must`
methods.

## [scalatest]
The `xpct-scalatest` package contains the trait `XpctSpec`, providing a helper function `xpct` that converts an `Xpct`
to a `TestFailedException`.

## [utest]
The `xpct-utest` package contains the trait `XpctSpec`, providing a helper function `xpct` that converts an `Xpct` to an
exception.


[cats-effect]: https://github.com/typelevel/cats-effect
[kallikrein]: https://github.com/tek/kallikrein
[specs2]: https://github.com/etorreborre/specs2
[scalatest]: https://github.com/scalatest/scalatest
[utest]: https://github.com/lihaoyi/utest
