package org.bykn.bosatsu.rankn

import cats.data.{Ior, NonEmptyList}
import org.scalatest.FunSuite
import org.bykn.bosatsu._

import Expr._
import Type.Var.Bound
import Type.ForAll

import TestUtils.{checkLast, testPackage}

import Identifier.Constructor

import fastparse.all._

class RankNInferTest extends FunSuite {

  val emptyRegion: Region = Region(0, 0)

  implicit val unitRegion: HasRegion[Unit] = HasRegion.instance(_ => emptyRegion)

  private def strToConst(str: Identifier.Constructor): Type.Const =
    str.asString match {
      case "Int" => Type.Const.predef("Int")
      case "String" => Type.Const.predef("String")
      case s => Type.Const.Defined(testPackage, TypeName(str))
    }

  def asFullyQualified(ns: Iterable[(Identifier, Type)]): Map[Infer.Name, Type] =
    ns.iterator.map { case (n, t) => ((Some(testPackage), n), t) }.toMap

  def typeFrom(str: String): Type =
    TypeRef.parser.parse(str) match {
      case Parsed.Success(typeRef, _) =>
        TypeRefConverter[cats.Id](typeRef)(strToConst(_))
      case Parsed.Failure(exp, idx, extra) =>
        sys.error(s"failed to parse: $str: $exp at $idx with trace: ${extra.traced.trace}")
    }

  def runUnify(left: String, right: String) = {
    val t1 = typeFrom(left)
    val t2 = typeFrom(right)

    Infer.substitutionCheck(t1, t2, emptyRegion, emptyRegion)
      .runFully(Map.empty, Map.empty)
  }

  def assertTypesUnify(left: String, right: String) =
    assert(runUnify(left, right).isRight, s"$left does not unify with $right")

  def assertTypesDisjoint(left: String, right: String) =
    assert(runUnify(left, right).isLeft, s"$left unexpectedly unifies with $right")

  def defType(n: String): Type.Const.Defined =
    Type.Const.Defined(testPackage, TypeName(Identifier.Constructor(n)))

  val withBools: Map[Infer.Name, Type] =
    Map(
      (Some(PackageName.PredefName), Identifier.unsafe("True")) -> Type.BoolType,
      (Some(PackageName.PredefName), Identifier.unsafe("False")) -> Type.BoolType)
  val boolTypes: Map[(PackageName, Constructor), Infer.Cons] =
    Map(
      ((PackageName.PredefName, Constructor("True")), (Nil, Nil, Type.Const.predef("Bool"))),
      ((PackageName.PredefName, Constructor("False")), (Nil, Nil, Type.Const.predef("Bool"))))

  def testType[A: HasRegion](term: Expr[A], ty: Type) =
    Infer.typeCheck(term).runFully(withBools, boolTypes) match {
      case Left(err) => assert(false, err)
      case Right(tpe) => assert(tpe.getType == ty, term.toString)
    }

  def testLetTypes[A: HasRegion](terms: List[(String, Expr[A], Type)]) =
    Infer.typeCheckLets(testPackage, terms.map { case (k, v, _) => (Identifier.Name(k), RecursionKind.NonRecursive, v) })
      .runFully(withBools, boolTypes) match {
        case Left(err) => assert(false, err)
        case Right(tpes) =>
          assert(tpes.size == terms.size)
          terms.zip(tpes).foreach { case ((n, exp, expt), (n1, _, te)) =>
            assert(n == n1.asString, s"the name changed: $n != $n1")
            assert(te.getType == expt, s"$n = $exp failed to typecheck to $expt, got ${te.getType}")
          }
      }


  def lit(i: Int): Expr[Unit] = Literal(Lit(i.toLong), ())
  def lit(b: Boolean): Expr[Unit] =
    if (b) Global(PackageName.PredefName, Identifier.Constructor("True"), ())
    else Global(PackageName.PredefName, Identifier.Constructor("False"), ())
  def let(n: String, expr: Expr[Unit], in: Expr[Unit]): Expr[Unit] =
    Let(Identifier.Name(n), expr, in, RecursionKind.NonRecursive, ())
  def lambda(arg: String, result: Expr[Unit]): Expr[Unit] =
    Lambda(Identifier.Name(arg), result, ())
  def v(name: String): Expr[Unit] =
    Identifier.unsafe(name) match {
      case c@Identifier.Constructor(_) => Global(testPackage, c, ())
      case b: Identifier.Bindable => Local(b, ())
    }
  def ann(expr: Expr[Unit], t: Type): Expr[Unit] = Annotation(expr, t, ())

  def app(fn: Expr[Unit], arg: Expr[Unit]): Expr[Unit] = App(fn, arg, ())
  def alam(arg: String, tpe: Type, res: Expr[Unit]): Expr[Unit] =
    AnnotatedLambda(Identifier.Name(arg), tpe, res, ())

  def ife(cond: Expr[Unit], ift: Expr[Unit], iff: Expr[Unit]): Expr[Unit] = Expr.ifExpr(cond, ift, iff, ())
  def matche(arg: Expr[Unit], branches: NonEmptyList[(Pattern[String, Type], Expr[Unit])]): Expr[Unit] =
    Match(arg,
      branches.map { case (p, e) =>
        val p1 = p.mapName { n => (testPackage, Constructor(n)) }
        (p1, e)
      },
      ())

  /**
   * Check that a no import program has a given type
   */
  def parseProgram(statement: String, tpe: String) =
    checkLast(statement) { te0 =>

      val te = TypedExpr.normalize(te0).getOrElse(te0)
      te.traverseType[cats.Id] {
        case t@Type.TyVar(Type.Var.Skolem(_, _)) =>
          fail(s"illegate skolem ($t) escape in $te")
          t
        case t@Type.TyMeta(_) =>
          fail(s"illegate meta ($t) escape in $te")
          t
        case good =>
          good
      }
      // make sure we can render repr:
      val rendered = te.repr
      val tp = te.getType
      lazy val teStr = TypeRef.fromTypes(None, tp :: Nil)(tp).toDoc.render(80)
      assert(Type.freeTyVars(tp :: Nil).isEmpty, s"illegal inferred type: $teStr")

      assert(Type.metaTvs(tp :: Nil).isEmpty,
        s"illegal inferred type: $teStr")
      assert(te.getType == typeFrom(tpe))
    }

  // this could be used to test the string representation of expressions
  def checkTERepr(statement: String, repr: String) =
    checkLast(statement) { te => assert(te.repr == repr) }

  /**
   * Test that a program is ill-typed
   */
  def parseProgramIllTyped(statement: String) =
    Statement.parser.parse(statement) match {
      case Parsed.Success(stmts, _) =>
        Package.inferBody(testPackage, Nil, stmts) match {
          case Ior.Left(_) | Ior.Both(_, _) => assert(true)
          case Ior.Right(program) =>
            fail("expected an invalid program, but got: " + program.lets.toString)
        }
      case Parsed.Failure(exp, idx, extra) =>
        fail(s"failed to parse: $statement: $exp at $idx with trace: ${extra.traced.trace}")
    }

  test("assert some basic unifications") {
    assertTypesUnify("forall a. a", "forall b. b")
    assertTypesUnify("forall a. a", "Int")
    assertTypesUnify("forall a, b. a -> b", "forall b. b -> Int")
    assertTypesUnify("forall a, b. a -> b", "forall a. a -> (forall b. b -> b)")
    assertTypesUnify("(forall a. a) -> Int", "(forall a. a) -> Int")
    assertTypesUnify("(forall a. a -> Int) -> Int", "(forall a. a -> Int) -> Int")
    assertTypesUnify("forall a, b. a -> b -> b", "forall a. a -> a -> a")
    // these aren't disjoint but the right is more polymorphic
    assertTypesDisjoint("forall a. a -> a -> a", "forall a, b. a -> b -> b")
    assertTypesUnify("forall a, b. a -> b", "forall b, c. b -> (c -> Int)")
    // assertTypesUnify("(forall a. a)[Int]", "Int")
    // assertTypesUnify("(forall a. Int)[b]", "Int")
    assertTypesUnify("forall a, f. f[a]", "forall x. List[x]")
    //assertTypesUnify("(forall a, b. a -> b)[x, y]", "z -> w")

    assertTypesDisjoint("Int", "String")
    assertTypesDisjoint("Int -> Unit", "String")
    assertTypesDisjoint("Int -> Unit", "String -> a")
    //assertTypesDisjoint("forall a. Int", "Int") // the type on the left has * -> * but the right is *
  }

  test("Basic inferences") {

    testType(lit(100), Type.IntType)
    testType(let("x", lambda("y", v("y")), lit(100)), Type.IntType)
    testType(lambda("y", v("y")),
      ForAll(NonEmptyList.of(Bound("a")),
        Type.Fun(Type.TyVar(Bound("a")),Type.TyVar(Bound("a")))))
    testType(lambda("y", lambda("z", v("y"))),
      ForAll(NonEmptyList.of(Bound("a"), Bound("b")),
        Type.Fun(Type.TyVar(Bound("a")),
          Type.Fun(Type.TyVar(Bound("b")),Type.TyVar(Bound("a"))))))

    testType(app(lambda("x", v("x")), lit(100)), Type.IntType)
    testType(ann(app(lambda("x", v("x")), lit(100)), Type.IntType), Type.IntType)
    testType(app(alam("x", Type.IntType, v("x")), lit(100)), Type.IntType)

    // test branches
    testType(ife(lit(true), lit(0), lit(1)), Type.IntType)
    testType(let("x", lit(0), ife(lit(true), v("x"), lit(1))), Type.IntType)

    val identFnType =
      ForAll(NonEmptyList.of(Bound("a")),
        Type.Fun(Type.TyVar(Bound("a")), Type.TyVar(Bound("a"))))
    testType(let("x", lambda("y", v("y")),
      ife(lit(true), v("x"),
        ann(lambda("x", v("x")), identFnType))), identFnType)

    // test some lets
    testLetTypes(
      List(
        ("x", lit(100), Type.IntType),
        ("y", Expr.Global(testPackage, Identifier.Name("x"), ()), Type.IntType)))
  }

  test("match inference") {
    testType(
      matche(lit(10),
        NonEmptyList.of(
          (Pattern.WildCard, lit(0))
          )), Type.IntType)

    testType(
      matche(lit(true),
        NonEmptyList.of(
          (Pattern.WildCard, lit(0))
          )), Type.IntType)

    testType(
      matche(lit(true),
        NonEmptyList.of(
          (Pattern.Annotation(Pattern.WildCard, Type.BoolType), lit(0))
          )), Type.IntType)
  }

  object OptionTypes {
    val optName = defType("Option")
    val optType: Type.Tau = Type.TyConst(optName)

    val pn = testPackage
    val definedOption = Map(
      ((pn, Constructor("Some")), (Nil, List(Type.IntType), optName)),
      ((pn, Constructor("None")), (Nil, Nil, optName)))

    val definedOptionGen = Map(
      ((pn, Constructor("Some")), (List((Bound("a"), Variance.co)), List(Type.TyVar(Bound("a"))), optName)),
      ((pn, Constructor("None")), (List((Bound("a"), Variance.co)), Nil, optName)))
  }

  test("match with custom non-generic types") {
    def b(a: String): Type.Var.Bound = Type.Var.Bound(a)
    def tv(a: String): Type = Type.TyVar(b(a))

    import OptionTypes._

    val constructors = Map(
      (Identifier.unsafe("Some"), Type.Fun(Type.IntType, optType))
    )

    def testWithOpt[A: HasRegion](term: Expr[A], ty: Type) =
      Infer.typeCheck(term).runFully(withBools ++ asFullyQualified(constructors), definedOption ++ boolTypes) match {
        case Left(err) => assert(false, err)
        case Right(tpe) => assert(tpe.getType == ty, term.toString)
      }

    def failWithOpt[A: HasRegion](term: Expr[A]) =
      Infer.typeCheck(term).runFully(withBools ++ asFullyQualified(constructors), definedOption ++ boolTypes) match {
        case Left(err) => assert(true)
        case Right(tpe) => assert(false, s"expected to fail, but inferred type $tpe")
      }

    testWithOpt(
      matche(app(v("Some"), lit(1)),
        NonEmptyList.of(
          (Pattern.WildCard, lit(0))
          )), Type.IntType)

    testWithOpt(
      matche(app(v("Some"), lit(1)),
        NonEmptyList.of(
          (Pattern.PositionalStruct("Some", List(Pattern.Var(Identifier.Name("a")))), v("a")),
          (Pattern.PositionalStruct("None", Nil), lit(42))
          )), Type.IntType)

    failWithOpt(
      matche(app(v("Some"), lit(1)),
        NonEmptyList.of(
          (Pattern.PositionalStruct("Foo", List(Pattern.WildCard)), lit(0))
          )))
  }

  test("match with custom generic types") {
    def b(a: String): Type.Var.Bound = Type.Var.Bound(a)
    def tv(a: String): Type = Type.TyVar(b(a))

    import OptionTypes._

    val constructors = Map(
      (Identifier.unsafe("Some"), Type.ForAll(NonEmptyList.of(b("a")), Type.Fun(tv("a"), Type.TyApply(optType, tv("a"))))),
      (Identifier.unsafe("None"), Type.ForAll(NonEmptyList.of(b("a")), Type.TyApply(optType, tv("a"))))
    )

    def testWithOpt[A: HasRegion](term: Expr[A], ty: Type) =
      Infer.typeCheck(term).runFully(withBools ++ asFullyQualified(constructors), definedOptionGen ++ boolTypes) match {
        case Left(err) => assert(false, err)
        case Right(tpe) => assert(tpe.getType == ty, term.toString)
      }

    def failWithOpt[A: HasRegion](term: Expr[A]) =
      Infer.typeCheck(term).runFully(withBools ++ asFullyQualified(constructors), definedOptionGen ++ boolTypes) match {
        case Left(err) => assert(true)
        case Right(tpe) => assert(false, s"expected to fail, but inferred type $tpe")
      }

    testWithOpt(
      matche(app(v("Some"), lit(1)),
        NonEmptyList.of(
          (Pattern.WildCard, lit(0))
          )), Type.IntType)

    testWithOpt(
      matche(app(v("Some"), lit(1)),
        NonEmptyList.of(
          (Pattern.PositionalStruct("Some", List(Pattern.Var(Identifier.Name("a")))), v("a")),
          (Pattern.PositionalStruct("None", Nil), lit(42))
          )), Type.IntType)

    // Nested Some
    testWithOpt(
      matche(app(v("Some"), app(v("Some"), lit(1))),
        NonEmptyList.of(
          (Pattern.PositionalStruct("Some", List(Pattern.Var(Identifier.Name("a")))), v("a"))
          )), Type.TyApply(optType, Type.IntType))

    failWithOpt(
      matche(app(v("Some"), lit(1)),
        NonEmptyList.of(
          (Pattern.PositionalStruct("Foo", List(Pattern.WildCard)), lit(0))
          )))
  }

  test("Test a constructor with ForAll") {
    def b(a: String): Type.Var.Bound = Type.Var.Bound(a)
    def tv(a: String): Type = Type.TyVar(b(a))

    val pureName = defType("Pure")
    val pureType: Type.Tau = Type.TyConst(pureName)
    val optName = defType("Option")
    val optType: Type.Tau = Type.TyConst(optName)

    val pn = testPackage
    /**
     * struct Pure(pure: forall a. a -> f[a])
     */
    val defined = Map(
      ((pn, Constructor("Pure")), (List((b("f"), Variance.in)),
        List(Type.ForAll(NonEmptyList.of(b("a")), Type.Fun(tv("a"), Type.TyApply(tv("f"), tv("a"))))),
        pureName)),
      ((pn, Constructor("Some")), (List((b("a"), Variance.co)), List(tv("a")), optName)),
      ((pn, Constructor("None")), (List((b("a"), Variance.co)), Nil, optName)))

    val constructors = Map(
      (Identifier.unsafe("Pure"), Type.ForAll(NonEmptyList.of(b("f")),
        Type.Fun(Type.ForAll(NonEmptyList.of(b("a")), Type.Fun(tv("a"), Type.TyApply(tv("f"), tv("a")))),
          Type.TyApply(Type.TyConst(pureName), tv("f")) ))),
      (Identifier.unsafe("Some"), Type.ForAll(NonEmptyList.of(b("a")), Type.Fun(tv("a"), Type.TyApply(optType, tv("a"))))),
      (Identifier.unsafe("None"), Type.ForAll(NonEmptyList.of(b("a")), Type.TyApply(optType, tv("a"))))
    )

    def testWithTypes[A: HasRegion](term: Expr[A], ty: Type) =
      Infer.typeCheck(term).runFully(withBools ++ asFullyQualified(constructors), defined ++ boolTypes) match {
        case Left(err) => assert(false, err)
        case Right(tpe) => assert(tpe.getType == ty, term.toString)
      }

    testWithTypes(
      app(v("Pure"), v("Some")), Type.TyApply(Type.TyConst(pureName), optType))
  }

  test("test inference of basic expressions") {
    parseProgram("""#
main = (\x -> x)(1)
""", "Int")

    parseProgram("""#
x = 1
y = x
main = y
""", "Int")
  }

  test("test inference with partial def annotation") {
    parseProgram("""#

def ident -> forall a. a -> a:
  \x -> x

main = ident(1)
""", "Int")

    parseProgram("""#

def ident(x: a): x

main = ident(1)
""", "Int")

    parseProgram("""#

def ident(x) -> a: x

main = ident(1)
""", "Int")

    parseProgram("""#

enum MyBool: T, F
struct Pair(fst, snd)

def swap_maybe(x: a, y, swap) -> Pair[a, a]:
  match swap:
    T: Pair(y, x)
    F: Pair(x, y)

def res:
  Pair(r, _) = swap_maybe(1, 2, F)
  r

main = res
""", "Int")


    parseProgram("""#

struct Pair(fst: a, snd: a)

def mkPair(y, x: a):
  Pair(x, y)

def fst:
  Pair(f, _) = mkPair(1, 2)
  f

main = fst
""", "Int")
  }

  test("test inference with some defined types") {
    parseProgram("""#
struct Unit

main = Unit
""", "Unit")

    parseProgram("""#
enum Option:
  None
  Some(a)

main = Some(1)
""", "Option[Int]")

    parseProgram("""#
enum Option:
  None
  Some(a)

main = Some
""", "forall a. a -> Option[a]")

    parseProgram("""#
id = \x -> x
main = id
""", "forall a. a -> a")

    parseProgram("""#
id = \x -> x
main = id(1)
""", "Int")

   parseProgram("""#
enum Option:
  None
  Some(a)

x = Some(1)
main = match x:
  None: 0
  Some(y): y
""", "Int")

   parseProgram("""#
enum List:
  Empty
  NonEmpty(a: a, tail: b)

x = NonEmpty(1, Empty)
main = match x:
  Empty: 0
  NonEmpty(y, _): y
""", "Int")

   parseProgram("""#
enum Opt:
  None, Some(a)

struct Monad(pure: forall a. a -> f[a], bind: forall a, b. f[a] -> (a -> f[b]) -> f[b])

def optBind(opt, bindFn):
  match opt:
    None: None
    Some(a): bindFn(a)

main = Monad(Some, optBind)
""", "Monad[Opt]")

   parseProgram("""#
enum Opt:
  None, Some(a)

struct Monad(pure: forall a. a -> f[a], bind: forall a, b. f[a] -> (a -> f[b]) -> f[b])

def opt_bind(opt, bind_fn):
  match opt:
    None: None
    Some(a): bind_fn(a)

option_monad = Monad(Some, opt_bind)

def use_bind(m: Monad[f], a, b, c):
  Monad { pure, bind } = m
  a1 = bind(a, pure)
  b1 = bind(b, pure)
  c1 = bind(c, pure)
  bind(a1)(\_ -> bind(b1)(\_ -> c1))

main = use_bind(option_monad, None, None, None)
""", "forall a. Opt[a]")

   // TODO:
   // The challenge here is that the naive curried form of the
   // def will not see the forall until the final parameter
   // we need to bubble up the forall on the whole function.
   //
   // same as the above with a different order in use_bind
   parseProgram("""#
enum Opt:
  None, Some(a)

struct Monad(pure: forall a. a -> f[a], bind: forall a, b. f[a] -> (a -> f[b]) -> f[b])

def opt_bind(opt, bind_fn):
  match opt:
    None: None
    Some(a): bind_fn(a)

option_monad = Monad(Some, opt_bind)

def use_bind(a, b, c, m: Monad[f]):
  Monad { pure, bind } = m
  a1 = bind(a, pure)
  b1 = bind(b, pure)
  c1 = bind(c, pure)
  bind(a1)(\_ -> bind(b1)(\_ -> c1))

main = use_bind(None, None, None, option_monad)
""", "forall a. Opt[a]")
  }

  test("test zero arg defs") {
   parseProgram("""#

struct Foo

def fst -> Foo: Foo

main = fst
""", "Foo")

   parseProgram("""#

enum Foo:
  Bar, Baz(a)

def fst -> Foo[a]: Bar

main = fst
""", "forall a. Foo[a]")
  }


  test("substition works correctly") {

    parseProgram("""#
(id: forall a. a -> a) = \x -> x

struct Foo

def apply(fn, arg: Foo): fn(arg)

main = apply(id, Foo)
""", "Foo")

    parseProgram("""#
(id: forall a. a -> a) = \x -> x

struct Foo

(idFoo: Foo -> Foo) = id

def apply(fn, arg: Foo): fn(arg)

main = apply(id, Foo)
""", "Foo")

    parseProgram("""#

struct FnWrapper(fn: a -> a)

(id: forall a. FnWrapper[a]) = FnWrapper(\x -> x)

struct Foo

(idFoo: FnWrapper[Foo]) = id

def apply(fn, arg: Foo):
  FnWrapper(f) = fn
  f(arg)

main = apply(id, Foo)
""", "Foo")

    parseProgram("""#
struct Foo
(id: forall a. a -> Foo) = \_ -> Foo

(idFoo: Foo -> Foo) = id

(id2: Foo -> Foo) = \x -> x
(idGen2: (forall a. a) -> Foo) = id2

main = Foo
""", "Foo")

    parseProgramIllTyped("""#

struct Foo
(idFooRet: forall a. a -> Foo) = \_ -> Foo

(id: forall a. a -> a) = idFooRet

main = Foo
""")

    parseProgram("""#
enum Foo: Bar, Baz

(bar1: forall a. (Foo -> a) -> a) = \fn -> fn(Bar)
(baz1: forall a. (Foo -> a) -> a) = \fn -> fn(Baz)
(bar2: forall a. (a -> Foo) -> Foo) = \_ -> Bar
(baz2: forall a. (a -> Foo) -> Foo) = \_ -> Baz
(bar3: ((forall a. a) -> Foo) -> Foo) = \_ -> Bar
(baz3: ((forall a. a) -> Foo) -> Foo) = \_ -> Baz

(bar41: (Foo -> Foo) -> Foo) = bar1
(bar42: (Foo -> Foo) -> Foo) = bar2
(baz41: (Foo -> Foo) -> Foo) = baz1
(baz42: (Foo -> Foo) -> Foo) = baz2
# since (a -> b) -> b is covariant in a, we can substitute bar3 and baz3
(baz43: (Foo -> Foo) -> Foo) = baz3
(baz43: (Foo -> Foo) -> Foo) = baz3

(producer: Foo -> (forall a. (Foo -> a) -> a)) = \_ -> bar1
# in the covariant position, we can substitute
(producer1: Foo -> ((Foo -> Foo) -> Foo)) = producer

main = Bar
""", "Foo")
    parseProgram("""#
enum Foo: Bar, Baz

struct Cont(cont: (b -> a) -> a)

(bar1: forall a. Cont[Foo, a]) = Cont(\fn -> fn(Bar))
(baz1: forall a. Cont[Foo, a]) = Cont(\fn -> fn(Baz))
(bar2: forall a. Cont[a, Foo]) = Cont(\_ -> Bar)
(baz2: forall a. Cont[a, Foo]) = Cont(\_ -> Baz)
(bar3: Cont[forall a. a, Foo]) = Cont(\_ -> Bar)
(baz3: Cont[forall a. a, Foo]) = Cont(\_ -> Baz)

(bar41: Cont[Foo, Foo]) = bar1
(bar42: Cont[Foo, Foo]) = bar2
(baz41: Cont[Foo, Foo]) = baz1
(baz42: Cont[Foo, Foo]) = baz2
# Cont is covariant in a, this should be allowed
(baz43: Cont[Foo, Foo]) = baz3
(baz43: Cont[Foo, Foo]) = baz3

(producer: Foo -> (forall a. Cont[Foo, a])) = \_ -> bar1
# in the covariant position, we can substitute
(producer1: Foo -> Cont[Foo, Foo]) = producer

main = Bar
""", "Foo")

    parseProgramIllTyped("""#
enum Foo: Bar, Baz

struct Cont(cont: (b -> a) -> a)

(consumer: (forall a. Cont[Foo, a]) -> Foo) = \x -> Bar
# in the contravariant position, we cannot substitute
(consumer1: Cont[Foo, Foo] -> Foo) = consumer

main = Bar
""")

     parseProgram("""#
struct Foo
enum Opt: Nope, Yep(a)

(producer: Foo -> forall a. Opt[a]) = \_ -> Nope
# in the covariant position, we can substitute
(producer1: Foo -> Opt[Foo]) = producer
(consumer: Opt[Foo]-> Foo) = \_ -> Foo
# in the contravariant position, we can generalize
(consumer1: (forall a. Opt[a])  -> Foo) = consumer

main = Foo
""", "Foo")

     parseProgramIllTyped("""#
struct Foo
enum Opt: Nope, Yep(a)

(consumer: (forall a. Opt[a]) -> Foo) = \x -> Foo
# in the contravariant position, we cannot substitute
(consumer1: Opt[Foo] -> Foo) = consumer

main = Foo
""")
     parseProgramIllTyped("""#
struct Foo
enum Opt: Nope, Yep(a)

(producer: Foo -> Opt[Foo]) = \x -> Nope
# the variance forbid generalizing in this direction
(producer1: Foo -> forall a. Opt[a]) = producer

main = Foo
""")

    parseProgram("""#
struct Foo
enum Opt: Nope, Yep(a)

struct FnWrapper(fn: a -> b)

(producer: FnWrapper[Foo, forall a. Opt[a]]) = FnWrapper(\_ -> Nope)
# in the covariant position, we can substitute
(producer1: FnWrapper[Foo, Opt[Foo]]) = producer
(consumer: FnWrapper[Opt[Foo], Foo]) = FnWrapper(\_ -> Foo)
# in the contravariant position, we can generalize
(consumer1: FnWrapper[forall a. Opt[a], Foo]) = consumer

main = Foo
""", "Foo")

    parseProgramIllTyped("""#
struct Foo
enum Opt: Nope, Yep(a)

struct FnWrapper(fn: a -> b)

(consumer: FnWrapper[forall a. Opt[a], Foo]) = FnWrapper(\x -> Foo)
# in the contravariant position, we cannot substitute
(consumer1: FnWrapper[Opt[Foo], Foo]) = consumer

main = Foo
""")
    parseProgramIllTyped("""#
struct Foo
enum Opt: Nope, Yep(a)

struct FnWrapper(fn: a -> b)

(producer: FnWrapper[Foo, Opt[Foo]]) = FnWrapper(\x -> Nope)
# in the covariant position, we can't generalize
(producer1: FnWrapper[Foo, forall a. Opt[a]]) = producer

main = Foo
""")

  }

  test("def with type annotation and use the types inside") {
   parseProgram("""#

struct Pair(fst, snd)

def fst(p: Pair[a, b]) -> a:
  Pair(f, _) = p
  f

main = fst(Pair(1, "1"))
""", "Int")
  }

  test("test that we see some ill typed programs") {
  parseProgramIllTyped("""#

def foo(i: Int): i

main = foo("Not an Int")
""")
  }

  test("using a literal the wrong type is ill-typed") {

  parseProgramIllTyped("""#

x = "foo"

main = match x:
  1: "can't really be an int"
  y: y
""")

  parseProgramIllTyped("""#

x = 1

main = match x:
  "1": "can't really be a string"
  y: y
""")
  }

  test("badly shaped top-level match fails to compile") {
    parseProgramIllTyped("""#

struct Foo(x)
x = 1

Foo(_) = x
main = 1
""")

    parseProgramIllTyped("""#

enum LR: L(a), R(b)

# this isn't legit: it is a non-total match
L(_) = L(1)
main = 1
""")
  }

  test("structural recursion can be typed") {
    parseProgram("""#

enum Nat: Zero, Succ(prev: Nat)

def len(l):
  recur l:
    Zero: 0
    Succ(p): len(p)

main = len(Succ(Succ(Zero)))
""", "Int")

    parseProgram("""#

enum Nat: Zero, Succ(prev: Nat)

def len(l):
  def len0(l):
    recur l:
      Zero: 0
      Succ(p): len0(p)
  len0(l)

main = len(Succ(Succ(Zero)))
""", "Int")
  }

  test("nested def example") {

    parseProgram("""#
struct Pair(first, second)

def bar(x):
  def baz(y):
    Pair(x, y)

  baz(10)

main = bar(5)
""", "Pair[Int, Int]")
  }

  test("test checkRho on annotated lambda") {

    parseProgram("""#
struct Foo
struct Bar

(fn: forall a. a -> Bar) = \_ -> Bar
#(fn: Bar -> Bar) = \x -> Bar

#dontCall = \(fn: forall a. a -> Bar) -> Foo
#dontCall = \(fn: Bar -> Bar) -> Foo
dontCall = \(_: (forall a. a) -> Bar) -> Foo

(main: Foo) = dontCall(fn)
""", "Foo")
  }
  test("ForAll as function arg") {
    parseProgram("""#
struct Wrap[bbbb](y1: bbbb)
struct Foo[cccc](y2: cccc)
struct Nil

# TODO: These variants don't work, but the one with a fully
# ascribed type does. There is a problem here with using subsCheck
# which can never substitute a metavariable for a sigma type (outer forall)
#Wrap(_: ((forall x. Foo[x]) -> Nil)) = cra_fn
#def foo(cra_fn: Wrap[(forall ssss. ssss) -> Nil]):
# Wrap(_: ((forall x. x) -> Nil)) = cra_fn
#Nil
def foo(cra_fn: Wrap[(forall ssss. Foo[ssss]) -> Nil]):
  match cra_fn:
    (_: Wrap[(forall x. Foo[x]) -> Nil]): Nil
main = foo
""", "Wrap[(forall ssss. Foo[ssss]) -> Nil] -> Nil")
  }

  test("use a type annotation inside a def") {
    parseProgram("""#
struct Foo
struct Bar
def ignore(_): Foo
def add(x):
  (y: Foo) = x
  _ = ignore(y)
  Bar
""", "Foo -> Bar")

    parseProgram("""#
struct Foo
struct Bar(f: Foo)
def ignore(_): Foo
def add(x):
  (b@(y: Foo)) = x
  _ = ignore(y)
  Bar(b)
""", "Foo -> Bar")
  }

  test("top level matches don't introduce colliding bindings") {
    parseProgramIllTyped("""#
struct Pair(fst, snd)

Pair(a, b) = Pair(1, 2)
d = c
""")
  }

  test("check that annotations work") {
    parseProgramIllTyped("""#
struct Foo
struct Bar

def x:
  f = Foo
  f: Bar
""")

    parseProgramIllTyped("""#
struct Foo
struct Bar

def x:
  f: Bar = Foo
  f
""")
    parseProgramIllTyped("""#
struct Pair(a, b)
struct Foo
struct Bar

def x:
  Pair(f: Bar, g) = Pair(Foo, Foo)
  f
""")

    parseProgramIllTyped("""#
struct Pair(a, b)
struct Foo
struct Bar

def x:
  Pair(f, g) = Pair(Foo: Bar, Foo)
  f
""")

    parseProgramIllTyped("""#
struct Foo
struct Bar

x: Bar = Foo
""")

    parseProgram("""#
struct Foo
struct Bar

def x:
  f = Foo
  f: Foo
""", "Foo")

    parseProgram("""#
struct Foo
struct Bar

def x:
  f: Foo = Foo
  f
""", "Foo")
    parseProgram("""#
struct Pair(a, b)
struct Foo

def ignore(_): Foo

def x:
  Pair(f: Foo, g) = Pair(Foo, Foo)
  _ = ignore(g)
  f
""", "Foo")

    parseProgram("""#
struct Pair(a, b)
struct Foo

def x:
  Pair(f, _) = Pair(Foo: Foo, Foo)
  f
""", "Foo")

    parseProgram("""#
struct Foo

x: Foo = Foo
""", "Foo")
  }

  test("test inner quantification") {
    parseProgram("""#
struct Foo

# this should just be: type Foo
def foo:
  # TODO, we would like this test to pass with
  # the annotations below
  #def ident(x: a) -> a: x
  def ident(x): x
  ident(Foo)

""", "Foo")
  }
}
