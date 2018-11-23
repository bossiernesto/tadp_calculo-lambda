package tadp.simple_typed


import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import scala.util.parsing.input._


class SimplyTypedParser extends StandardTokenParsers {
  lexical.delimiters ++= List("(", ")", "\\", ".", ":", "=", "->", "{", "}", ",", "*")

  lexical.reserved ++= List("Bool", "Nat", "true", "false", "if", "then", "else", "succ",
    "pred", "iszero", "let", "in", "fst", "snd")

  /**
    * Term     ::= SimpleTerm { SimpleTerm }
    */
  def Term: Parser[Term] = positioned(
    rep1(SimpleTerm) ^^ { case termList => parseTermList(termList) }
      | failure("illegal start of term"))

  /**
    * SimpleTerm ::= "true"
    *               | "false"
    *               | number
    *               | "succ" Term
    *               | "pred" Term
    *               | "iszero" Term
    *               | "if" Term "then" Term "else" Term
    *               | ident
    *               | "\" ident ":" Type "." Term
    *               | "(" Term ")"
    *               | "let" ident ":" Type "=" Term "in" Term
    *               | "{" Term "," Term "}"
    *               | "fst" Term
    *               | "snd" Term
    */
  def SimpleTerm: Parser[Term] = positioned(
    "true" ^^^ True
      | "false" ^^^ False
      | nv
      | "succ" ~> Term ^^ {
      case e1 => Succ(e1)
    }
      | "pred" ~> Term ^^ {
      case e1 => Pred(e1)
    }
      | "iszero" ~> Term ^^ {
      case e1 => IsZero(e1)
    }
      | "let" ~ ident ~ ":" ~ Type ~ "=" ~ Term ~ "in" ~ Term ^^ {
      case "let" ~ x ~ ":" ~ tType ~ "=" ~ t1 ~ "in" ~ t2 =>
        Application(TermPar(Abstraction(Variable(x), tType, t2)), t1)
    }
      | "if" ~ Term ~ "then" ~ Term ~ "else" ~ Term ^^ {
      case "if" ~ t1 ~ "then" ~ t2 ~ "else" ~ t3 =>
        IfThenElse(t1, t2, t3)
    }
      | ident ^^ { case x => Variable(x.toString) }
      | "\\" ~ ident ~ ":" ~ Type ~ "." ~ Term ^^ {
      case "\\" ~ x ~ ":" ~ t ~ "." ~ t1 =>
        Abstraction(Variable(x.toString), t, t1)
    }
      | "(" ~ Term ~ ")" ^^ {
      case "(" ~ t ~ ")" => TermPar(t)
    }
      | "{" ~ Term ~ "," ~ Term ~ "}" ^^ {
      case "{" ~ t1 ~ "," ~ t2 ~ "}" => Pair(t1, t2)
    }
      | "fst" ~ Term ^^ { case "fst" ~ t => Fst(t) }
      | "snd" ~ Term ^^ { case "snd" ~ t => Snd(t) }
      | failure("illegal start of simple term"))

  def nv: Parser[Term] = positioned(
    rep("succ") ~ numericLit ^^ {
      case list ~ numLit => syntacticSugar(list.size + numLit.toInt)
    }
      | failure("illegal start of simple term"))

  /**
    * Type       ::= SimpleType [ "->" Type ]
    */
  def Type: Parser[Type] = positioned(
    PairType ~ rep("->" ~ PairType) ^^ { case t1 ~ list => parserTypeList(t1, list.map { _._2 }, true) }
      | failure("illegal start of type"))

  def PairType: Parser[Type] = positioned(
    SType ~ rep("*" ~ SType) ^^ { case t1 ~ list => parserTypeList(t1, list.map { _._2 }, false) }
      | failure("illegal start of type"))

  def SType: Parser[Type] = positioned(
    "Nat" ^^^ TypeNat
      | "Bool" ^^^ TypeBool
      | "(" ~ Type ~ ")" ^^ {
      case "(" ~ t ~ ")" => t match {
        case TypePar(x) => t
        case _ => TypePar(t)
      }
    }
      | failure("illegal start of type"))

  def syntacticSugar(x: Int): Term = {
    x match {
      case t if t == 0 => Zero
      case _ => Succ(syntacticSugar(x - 1))
    }
  }

  def parseTermList(termList: List[Term]): Term = {
    termList match {
      case head :: Nil => head
      case _ => Application(parseTermList(termList.init), termList.last)
    }
  }

  def parserTypeList(first: Type, typeList: List[Type], isFun: Boolean): Type = typeList match {
    case head :: Nil => if (isFun) TypeFun(first, head) else TypePair(first, head)
    case head :: tail => {
      if (isFun) TypeFun(first, parserTypeList(head, tail, isFun))
      else TypePair(first, parserTypeList(head, tail, isFun))
    }
    case nil => first
  }

}
