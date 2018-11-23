package tadp.simple_typed

import scala.util.parsing.input._

trait Helpers {
  /** Is the given term a numeric value? */
  def isNumericVal(t: Term): Boolean = t match {
    case Zero => true
    case TermPar(x) => isNumericVal(x)
    case Succ(x) => isNumericVal(x)
    case _ => false
  }

  /** Is the given term a value? */
  def isValue(t: Term): Boolean = t match {
    case _: Value | TermPar(_: Value) => true
    case Pair(t1, t2) => { isValue(t1) && isValue(t2) }
    case _ => isNumericVal(t)
  }
}

/** Thrown when no reduction rule applies to the given term. */
case class NoRuleApplies(t: Term) extends Exception(t.toString)

/** Print an error message, together with the position where it occured. */
case class TypeError(pos: Position, msg: String) extends Exception(msg) {
  override def toString =
    msg + "\n" + pos.longString
}
