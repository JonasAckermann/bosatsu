package org.bykn.bosatsu.pattern

sealed trait SeqPart[+Elem] {
  def notWild: Boolean = false
}
object SeqPart {
  sealed trait SeqPart1[+Elem] extends SeqPart[Elem] {
    override def notWild: Boolean = true
  }

  implicit def partOrdering[E](implicit elemOrdering: Ordering[E]): Ordering[SeqPart[E]] =
    new Ordering[SeqPart[E]] {
      def compare(a: SeqPart[E], b: SeqPart[E]) =
        (a, b) match {
          case (Lit(i1), Lit(i2)) =>
            elemOrdering.compare(i1, i2)
          case (Lit(_), _) => -1
          case (_, Lit(_)) => 1
          case (AnyElem, AnyElem) => 0
          case (AnyElem, Wildcard) => -1
          case (Wildcard, AnyElem) => 1
          case (Wildcard, Wildcard) => 0
        }
    }

  case class Lit[Elem](item: Elem) extends SeqPart1[Elem]
  case object AnyElem extends SeqPart1[Nothing]
  // 0 or more characters
  case object Wildcard extends SeqPart[Nothing]

  implicit def part1SetOps[A](implicit setOpsA: SetOps[A]): SetOps[SeqPart1[A]] =
    new SetOps[SeqPart1[A]] {

      private val anyList = AnyElem :: Nil

      private def toPart1(a: A): SeqPart1[A] =
        if (setOpsA.isTop(a)) AnyElem
        else Lit(a)

      def anyDiff(a: A) =
        setOpsA.top match {
          case None => anyList
          case Some(topA) => setOpsA.difference(topA, a).map(toPart1)
        }

      val top = Some(AnyElem)
      def isTop(c: SeqPart1[A]) =
        c match {
          case AnyElem => true
          case Lit(a) => setOpsA.isTop(a)
        }

      def intersection(p1: SeqPart1[A], p2: SeqPart1[A]): List[SeqPart1[A]] =
        (p1, p2) match {
          case (Lit(c1), Lit(c2)) =>
            setOpsA.intersection(c1, c2)
              .map(toPart1(_))
          case (AnyElem, _) => p2 :: Nil
          case (_, AnyElem) => p1 :: Nil
        }

      def difference(p1: SeqPart1[A], p2: SeqPart1[A]): List[SeqPart1[A]] =
        (p1, p2) match {
          case (Lit(c1), Lit(c2)) =>
            setOpsA
              .difference(c1, c2)
              .map(toPart1(_))
          case (_, AnyElem) => Nil
          case (AnyElem, Lit(a2)) =>
            if (setOpsA.isTop(a2)) Nil
            else anyDiff(a2)
        }

      def subset(p1: SeqPart1[A], p2: SeqPart1[A]): Boolean =
        p2 match {
          case AnyElem => true
          case Lit(c2) =>
            p1 match {
              case Lit(c1) => setOpsA.subset(c2, c1)
              case AnyElem => setOpsA.isTop(c2)
            }
        }

      def unifyUnion(u: List[SeqPart1[A]]): List[SeqPart1[A]] = {
        // never add top values, so if we return a list, it is a union
        // of non-top elements
        def litOpt(u: List[SeqPart1[A]], acc: List[A]): Option[List[Lit[A]]] =
          u match {
            case Nil => Some(setOpsA.unifyUnion(acc.reverse).map(Lit(_)))
            case AnyElem :: _ => None
            case Lit(a) :: _ if setOpsA.isTop(a) => None
            case Lit(a) :: tail => litOpt(tail, a :: acc)
          }


        litOpt(u, Nil) match {
          case None => AnyElem :: Nil
          case Some(u) => u
        }
      }
    }

  implicit def part1Matcher[A, S](implicit amatcher: Matcher[A, S, Unit]): Matcher[SeqPart1[A], S, Unit] =
    new Matcher[SeqPart1[A], S, Unit] {
      val someUnit: Option[Unit] = Some(())
      val anyMatch: S => Option[Unit] = { _ => someUnit }

      def apply(s: SeqPart1[A]): S => Option[Unit] =
        s match {
          case AnyElem => anyMatch
          case Lit(c) => amatcher(c)
        }
    }
}


