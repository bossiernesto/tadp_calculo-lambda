package tadp.typed

//Simple transition to simple typed lambda using the first solution

//Simple Type structures
sealed trait Type
case object IntegerType extends Type
case object BooleanType extends Type
case class FunctionType(left: Type, right: Type) extends Type

//Now we can just rollback some decision using a simpler model before adding and joining the typer and the reducer/evaluator
sealed trait Term
case class Var(name: String) extends Term
case class Lit(value: Int) extends Term
case class Application(e1: Term, e2: Term) extends Term
case class Function(name: String, typ: Type, body: Term) extends Term

object NaiveReducer {
  // Reduce a term `t` to normal form given an environment `env`
  def eval(env: String => Option[Term], t: Term): Term = t match {
    case Var(s) => eval(env, env(s).getOrElse(sys.error(s"Unbound variable: $s")))
    case Application(e1, e2) => eval(env, e1) match {
      case Function(x, _, e) => eval(env, subst(x, e2, e))
      case _         => sys.error("Not a function: $e1")
    }
    case l@Function(_, _, _) => repaint(allNames, l)
    case x => x
  }

  // A source of fresh names
  lazy val allNames: Stream[String] =
    Stream.from(1).map("t" + _)

  // Substitute `t1` for variables names `v` in `t2`
  def subst(v: String, t1: Term, t2: Term): Term =  t2 match {
    case x@Var(s) => if (v == s) t1 else t2
    case Application(e1, e2) => Application(subst(v, t1, e1), subst(v, t1, e2))
    case f@Function(x, t, e) => if (x == v) f else Function(x, t, subst(v, t1, e))
    case x => x
  }

  def freeVars(t: Term): List[String] = {
    def go(free: List[String], bound: List[String], term: Term): List[String] =
      term match {
        case Var(s) => if (bound contains s) s :: free else free
        case Application(e1, e2) => go(free, bound, e1) ++ go(free, bound, e2)
        case Function(s, _, e) => go(free, s :: bound, e)
        case x => free
      }
    go(List(), List(), t)
  }

  def repaint(bucket: Stream[String], t: Term): Term = t match {
    case Function(v, tp, e) =>
      val fresh = bucket.head
      lazy val freshBucket = bucket.filterNot(freeVars(e) contains _)
      val e2 = subst(v, Var(fresh), e)
      Function(fresh, tp, repaint(freshBucket.tail, e2))
    case Application(e1, e2) => Application(repaint(bucket, e1), repaint(bucket, e2))
    case _ => t
  }
}


object NaiveTyper {

  val i = Function("x", IntegerType, Var("x"))
  val k = Function("x", IntegerType, Function("y", IntegerType, Var("x")))

  val capturing = Application(Application(k, Var("y")), Lit(10))
  val noncapturing = Application(Application(Function("a", IntegerType, Function("b", IntegerType, Var("a"))), Var("y")), Lit(10))

  val empty: String => Option[Term] = _ => None
  val wy: String => Option[Term] = y => if (y == "y") Some(Lit(42)) else None

  def typer(env: Map[String, Type], exp: Term): Type = exp match {
    case Lit(_) => IntegerType
    case Var(v) => env.getOrElse(v, sys.error(s"Unbound variable $v"))
    case Application(e1, e2) =>
      val t1 = typer(env, e1)
      t1 match {
        case FunctionType(ta, tr) =>
          val t2 = typer(env, e2)
          if (ta == t2) tr
          else sys.error(s"Type mismatch. Expected $ta, found $t2")
        case _ => sys.error(s"Not a function type: $t1")
      }
    case Function(x, t, e) => FunctionType(t, typer(env + (x -> t), e))
  }
}