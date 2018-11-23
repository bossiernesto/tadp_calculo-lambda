package tadp.simple_typed

object Reducers extends Helpers {

  type Context = List[(String, Type)]

  def alpha(t: Abstraction): Abstraction = {
    val newVarName = t.x.name + "$"
    var i = 1
    while (t.fv contains (newVarName + i)) {
      i += 1
    }
    val newVar = Variable(newVarName + i)
    Abstraction(newVar, t.tType, subst(t.t, t.x.name, newVar))
  }

  /**
    * Substitute into term t all occurrences of x with s
    */
  def subst(t: Term, x: String, s: Term): Term = t match {
    case Variable(n) => {
      if (n == x) s
      else t
    }
    case Abstraction(varBind, absType, b) => {
      if (varBind.name == x) t
      else {
        if (s.fv contains varBind.name) {
          val t1 = alpha(t.asInstanceOf[Abstraction])
          Abstraction(t1.x, t1.tType, subst(t1.t, x, s))
        } else {
          Abstraction(varBind, t.asInstanceOf[Abstraction].tType, subst(b, x, s))
        }
      }
    }
    case IsZero(numVal) => IsZero(subst(numVal, x, s))
    case Pred(numVal) => Pred(subst(numVal, x, s))
    case Succ(numVal) => Succ(subst(numVal, x, s))
    case Pair(t1, t2) => Pair(subst(t1, x, s), subst(t2, x, s))
    case Fst(numVal) => Fst(subst(numVal, x, s))
    case Snd(numVal) => Snd(subst(numVal, x, s))
    case IfThenElse(t1, t2, t3) => IfThenElse(subst(t1, x, s), subst(t2, x, s), subst(t3, x, s))
    case Application(t1, t2) => Application(subst(t1, x, s), subst(t2, x, s))
    case TermPar(termPar) => TermPar(subst(termPar, x, s))
    case _ => t
  }

  /** Call by value reducer. */
  def reduce(t: Term): Term = t match {
    case _ if isValue(t) => throw NoRuleApplies(t)
    case Succ(x) => Succ(reduce(x))
    case Pred(x) => x match {
      case TermPar(inner) if (isNumericVal(inner)) => reduce(Pred(inner))
      case Zero => Zero
      case Succ(numVal) if isNumericVal(numVal) => numVal
      case _ => Pred(reduce(x))
    }
    case IsZero(x) => x match {
      case TermPar(inner) if (isNumericVal(inner)) => reduce(IsZero(inner))
      case Zero => True
      case Succ(numVal) if isNumericVal(numVal) => False
      case _ => IsZero(reduce(x))
    }
    case IfThenElse(t1, t2, t3) => t1 match {
      case TermPar(inner) if (isValue(inner)) => reduce(IfThenElse(inner, t2, t3))
      case True => t2
      case False => t3
      case _ => IfThenElse(reduce(t1), t2, t3)
    }
    case Fst(x) => x match {
      case TermPar(inner) if (isValue(inner)) => reduce(Fst(inner))
      case Pair(x1, x2) if isValue(x) => x1
      case _ => Fst(reduce(x))
    }
    case Snd(x) => x match {
      case TermPar(inner) if (isValue(inner)) => reduce(Snd(inner))
      case Pair(x1, x2) if isValue(x) => x2
      case _ => Snd(reduce(x))
    }
    case TermPar(inner) => TermPar(reduce(inner))
    case p: Pair => isValue(p.t1) match {
      case false => Pair(reduce(p.t1), p.t2)
      case true => Pair(p.t1, reduce(p.t2))
    }
    case Application(t1, t2) => t1 match {
      case TermPar(inner) if (isValue(inner)) => isValue(t2) match {
        case true => TermPar(reduce(Application(inner, t2)))
        case false => Application(t1, reduce(t2))
      }
      case Abstraction(x, _, b) => isValue(t2) match {
        case true => subst(b, x.name, t2)
        case false => Application(t1, reduce(t2))
      }
      case _ => {
        Application(reduce(t1), t2)
      }
    }
    case _ => throw NoRuleApplies(t)
  }

  def contextLookup(ctx: Context, name: String): Type = {
    ctx.find({ e => e._1 == name }) match {
      case Some(x) =>
        x._2
      case None => null
    }
  }

  def addToContext(ctx: Context, name: String, t: Type): Context = {
    // just add the variable
    List((name, t)) ::: ctx
  }

  def renameVar(t: Abstraction, ctx: Context): Abstraction = {
    val newVarName = t.x.name + "$"
    var i = 1
    while ((t.fv contains (newVarName + i)) || contextLookup(ctx, newVarName + i) != null) {
      i += 1
    }
    val newVar = Variable(newVarName + i)
    Abstraction(newVar, t.tType, subst(t.t, t.x.name, newVar))
  }


  /**
    * Returns a stream of terms, each being one step of reduction.
    *
    *  @param t      the initial term
    *  @param reduce the evaluation strategy used for reduction.
    *  @return       the stream of terms representing the big reduction.
    */
  def path(t: Term, reduce: Term => Term): Stream[Term] =
    try {
      var t1 = reduce(t)
      t1 match {
        case TermPar(inner) => Stream.cons(t, path(inner, reduce))
        case _ => Stream.cons(t, path(t1, reduce))
      }
    } catch {
      case NoRuleApplies(_) =>
        Stream.cons(t, Stream.empty)
    }

}
