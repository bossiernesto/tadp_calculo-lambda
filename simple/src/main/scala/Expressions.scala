package tadp.lambda

sealed abstract class Expression {
  def substitute(orig: Var, sub: Expression): Expression
  def freeVars: Set[Var]
  def boundVars: Set[Var]
  def toString: String

  def evaluate(callback: Expression => Unit): Expression = {
    callback(this)
    val beta = LambdaRules.betaReduction(this)
    if (beta != this)
      beta.evaluate(callback)
    else {
      val eta = LambdaRules.etaConversion(this)
      if (eta != this) callback(eta)
      eta
    }
  }
  def evaluate: Expression = evaluate { e => () }
}

object Expression extends LambdaParsers {
  def main(args: Array[String]) {
    for (arg <- args) {
      val exp = Expression(arg)
      exp evaluate { println(_) }
    }
  }

  def apply(input: String): Expression = {
    (parseAll(expression, input): @unchecked) match {
      case Success(e, _) => e
    }
  }

  lazy val constants = sources transform { (name, src) => Expression(src) }

  val sources = Map(
    "0"     -> "λfx.x",
    "1"     -> "λfx.f x",
    "2"     -> "λfx.f (f x)",
    "3"     -> "λfx.f (f (f x))",
    "SUCC"  -> "λnfx.f (n f x)",
    "+"     -> "λmnfx.m f (n f x)",
    "*"     -> "λmn.m (+ n) 0",
    "^"     -> "λbe.e b",  // exponentiation
    "PRED"  -> "λnfx.n (λgh.h (g f)) (λu.x) (λu.u)",
    "-"     -> "λmn.n PRED m",
    "TRUE"  -> "λxy.x",
    "FALSE" -> "λxy.y",
    "&&"    -> "λpq.p q p",
    "||"    -> "λpq.p p q",
    "!"     -> "λpab.p b a"  // negation
  )
}

//VAR
case class Var(name: String) extends Expression {
  def substitute(orig: Var, sub: Expression): Expression =
    if (orig == this) sub else this

  def freeVars: Set[Var] = Set(this)
  def boundVars: Set[Var] = Set()

  override def toString: String = name

  /**
   * Adds prime (') to the variable name as many times as is necessary to get a
   * variable name that does not conflict with anything in `conflicting`.
   */
  def prime(conflicting: Set[Var]): Var = {
    val newVar = Var(name + "'")
    if (conflicting.contains(newVar))
      newVar.prime(conflicting)
    else
      newVar
  }
}

//FUNCTION
case class Function(argument: Var, body: Expression) extends Expression {
  def substitute(orig: Var, sub: Expression): Expression = {
    if (orig != argument) {
      if ((sub.freeVars) contains argument)
        (LambdaRules.alphaConversion(this, sub.freeVars)).substitute(orig, sub)
      else
        Function(argument, body.substitute(orig, sub))
    } else
      this
  }

  def freeVars: Set[Var] = body.freeVars - argument
  def boundVars: Set[Var] = body.boundVars + argument

  override def toString: String = String.format("λ%s.%s", argument, body)

  /**
   * `equals` is overriden here to capture alpha-equivalence.
   */
  override def equals(other: Any): Boolean = other match {
    case Function(a, b) =>
      (a == argument && b == body) || body == b.substitute(a, argument)
    case _ => false
  }
}

//APP
case class Application(function: Expression, argument: Expression) extends Expression {
  def substitute(orig: Var, sub: Expression): Expression =
    Application(function.substitute(orig, sub), argument.substitute(orig, sub))

  def freeVars: Set[Var] = function.freeVars ++ argument.freeVars
  def boundVars: Set[Var] = function.boundVars ++ argument.boundVars

  override def toString: String = {
    val left = function match {
      case Function(_, _) => "("+ function +")"
      case _ => function
    }
    val right = argument match {
      case Var(_) => argument
      case _ => "("+ argument +")"
    }
    left +" "+ right
  }
}

object LambdaRules {
  def alphaConversion(expression: Expression, conflicting: Set[Var]): Expression = expression match {
    case Var(_) => expression
    case Function(arg, body) =>
      if (conflicting contains arg) {
        val newArg = arg.prime(conflicting ++ expression.boundVars)
        Function(newArg, body.substitute(arg, newArg))
      } else
        Function(arg, alphaConversion(body, conflicting))
    case Application(a, b) =>
      Application(alphaConversion(a, conflicting),
        alphaConversion(b, conflicting))
  }

  def betaReduction(expression: Expression): Expression = expression match {
    case Application(Function(arg, body), b) => body.substitute(arg, b)
    case Application(a, b) => {
      val left = Application(betaReduction(a), b)
      if (left != expression)
        left
      else
        Application(a, betaReduction(b))
    }
    case Function(arg, body) => Function(arg, betaReduction(body))
    case _ => expression
  }

  def etaConversion(expression: Expression): Expression = expression match {
    case Function(x, Application(f, y)) if x == y =>
      if (f.freeVars contains x) expression else f
    case _ => expression
  }
}