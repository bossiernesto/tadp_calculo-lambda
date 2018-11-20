package tadp.lambda

import scala.util.parsing.combinator._

class LambdaParsers extends RegexParsers {
  def expression: Parser[Expression] = (
    application
      | simpleExpression
    )

  def simpleExpression: Parser[Expression] = (
    function
      | variable
      | constant
      | "("~>expression<~")"
    )

  def function: Parser[Expression] =
    lambda~>arguments~"."~expression ^^ {
      case args~"."~exp => (args :\ exp) { Function(_, _) }
    }

  def application: Parser[Expression] =
    simpleExpression~rep1(simpleExpression) ^^ {
      case exp~exps => (exp /: exps) { (app, e) => Application(app, e) }
    }

  def arguments: Parser[List[Var]] = rep1(variable)

  def lambda: Parser[String] = """\\|λ""".r

  def variable: Parser[Var] = """[a-z]'*""".r ^^ { Var(_) }

  def constant: Parser[Expression] = """[^a-z\\λ\(\)\s\.']+""".r ^^ {
    case name => Expression(Expression.sources(name))
  }
}
