package tadp.simple_typed

import tadp.simple_typed.Reducers._

trait Typer {
  /**
    * Returns the type of the given term <code>t</code>.
    *
    *  @param ctx the initial context
    *  @param t   the given term
    *  @return    the computed type
    */
  def typeof(ctx: Context, t: Term): Type = t match {
    case True | False | Zero =>
      t.getTermType
    case Succ(x) => {
      val argType = typeof(ctx, x)
      if (argType == TypeNat) {
        TypeNat
      } else {
        throw new TypeError(t.pos, "expected: Nat, found: " + argType)
      }
    }
    case Pred(x) => {
      val argType = typeof(ctx, x)
      if (argType == TypeNat) {
        TypeNat
      } else {
        throw new TypeError(t.pos, "expected: Nat, found: " + argType)
      }
    }
    case IsZero(x) => {
      val argType = typeof(ctx, x)
      if (argType == TypeNat) {
        TypeBool
      } else {
        throw new TypeError(t.pos, "expected: Nat, found: " + argType)
      }
    }
    case IfThenElse(t1, t2, t3) => typeof(ctx, t1) match {
      case TypeBool => {
        val t2Type = typeof(ctx, t2)
        val t3Type = typeof(ctx, t3)
        if (t2Type == t3Type) {
          t2Type
        } else {
          throw TypeError(t.pos, "type mismatch between conditional branches")
        }
      }
      case notBool => throw TypeError(t.pos, "expected: Bool, found: " + notBool)
    }
    case Variable(x) => {
      val varType = contextLookup(ctx, x)
      if (null == varType) {
        throw TypeError(t.pos, "undefined variable " + x)
      }
      varType
    }
    case Fst(x) => {
      val pairType = typeof(ctx, x)
      pairType match {
        case TypePair(p1, p2) => p1
        case _ => throw TypeError(t.pos, "pair type expected but " + pairType + " found")
      }
    }
    case Snd(x) => {
      val pairType = typeof(ctx, x)
      pairType match {
        case TypePair(p1, p2) => p2
        case _ => throw TypeError(t.pos, "pair type expected but " + pairType + " found")
      }
    }
    case TermPar(x) => typeof(ctx, x)
    case Application(t1, t2) => {
      val t1Type = typeof(ctx, t1)
      t1Type match {
        case TypePar(inner) => typeof(ctx, Application(inner, t2))
        case TypeFun(t1Fun, t2Fun) => {
          val t2Type = typeof(ctx, t2)
          if (t1Fun == t2Type) {
            t2Fun
          } else {
            throw TypeError(t.pos, "expected: " + t1Fun + ", found: " + t2Type)
          }
        }
        case notFun => throw TypeError(t.pos, "expected: function type, found: " + t1Type)
      }
    }
    case a:Abstraction => a.x match {
      case Variable(name) => {
        if (contextLookup(ctx, name) != null) {
          val renamed = renameVar(a, ctx)
          a.x = renamed.x
          a.t = renamed.t
        }
        val newContext = addToContext(ctx, a.x.name, a.tType)
        val bodyType = typeof(newContext, a.t)
        TypeFun(a.tType, bodyType)
      }
      case _ => throw TypeError(t.pos, "How did I parse this?!#@?! -.-")
    }
    case Pair(t1, t2) => TypePair(typeof(ctx, t1), typeof(ctx, t2))
    case _ => throw TypeError(t.pos, "This is not Term type " + t)
  }
}
