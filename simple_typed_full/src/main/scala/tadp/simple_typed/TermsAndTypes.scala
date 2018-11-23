package tadp.simple_typed


import scala.util.parsing.input.Positional

/** Abstract Syntax Trees for terms. */
abstract class Term extends Positional {

  def fv(): Set[String] = this match {
    case Variable(n) => Set(n)
    case TermPar(x) => x.fv
    case Abstraction(x, tType, t) => t.fv - x.name
    case Application(t1, t2) => t1.fv union t2.fv
    case _ => Set()
  }

  def getTermType(): Type = null
}

trait Value;

/**
  * Values from grammar. Ones that can change type have
  * it as constructor parameter, other return predefined value
  */

case object True extends Term with Value {
  override def toString() = "true"

  override def getTermType(): Type = TypeBool
}

case object False extends Term with Value {
  override def toString() = "false"

  override def getTermType(): Type = TypeBool
}

case object Zero extends Term with Value {
  override def toString() = "0"

  override def getTermType(): Type = TypeNat
}

case class Abstraction(var x: Variable, tType: Type, var t: Term) extends Term with Value {
  override def toString() = "\\" + x + ":" + tType + "." + absBody

  def absBody: String = t match {
    case TermPar(Abstraction(_, _, _)) => t.toString
    case TermPar(t1) => t1.toString
    case _ => t.toString
  }
}

// END VALUES

case class Succ(t: Term) extends Term {
  override def toString() = "succ " + t
}

case class Variable(name: String) extends Term {
  override def toString() = name
}

case class IfThenElse(t1: Term, t2: Term, t3: Term) extends Term {
  override def toString() = "if " + t1 + " then " + t2 + " else " + t3
}

case class Pred(t: Term) extends Term {
  override def toString() = "pred " + t
}

case class IsZero(t: Term) extends Term {
  override def toString() = "iszero " + t
}

case class Application(var t1: Term, t2: Term) extends Term {

  if (!Application.keepPar) {
    t1 = t1 match {
      case TermPar(Abstraction(_,_,_)) => t1
      case TermPar(inner) => inner
      case _ => t1
    }
  }

  override def toString() = t1 + " " + t2
}

case object Application {
  var keepPar = true;
}

case class TermPar(var t: Term) extends Term {
  {
    t = t match {
      case TermPar(inner) => inner
      case _ => t
    }
  }
  override def toString() = "(" + t + ")"
}

/* Pairs */

case class Pair(t1: Term, t2: Term) extends Term {
  override def toString: String = "{" + t1 + ", " + t2 + "}"
}

case class Fst(t: Term) extends Term {
  override def toString: String = "fst " + t
}

case class Snd(t: Term) extends Term {
  override def toString: String = "snd " + t
}


/** Types **/
/** Abstract Syntax Trees for types. */
abstract class Type extends Term

case object TypeBool extends Type {
  override def toString() = "Bool"
}

case object TypeNat extends Type {
  override def toString() = "Nat"
}

case class TypePair(fst: Type, snd: Type) extends Type {
  override def toString() = fst + "*" + snd
}

case class TypeFun(fst: Type, snd: Type) extends Type {
  override def toString() = fst + "->" + snd
}

case class TypePar(t: Type) extends Type with Equals {
  override def toString() = t match {
    case TypeBool | TypeNat => t.toString()
    case _ => "(" + t + ")"
  }

  /**
    * (Nat->Nat) is the same as Nat->Nat, so we have to override equals
    */
  override def equals(other: Any) = {
    other match {
      case that: fos.TypePar => t == that.t
      case that: fos.Type => t == that
      case _ => false
    }
  }

  override def hashCode() = {
    val prime = 19
    prime + t.hashCode
  }
}