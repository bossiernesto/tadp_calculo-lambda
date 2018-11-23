package tadp.simple_typed

import Reducers._

class SimplyTyped extends SimplyTypedParser with Typer {
  def main(args: Array[String]): Unit = {
    val tokens = new lexical.Scanner("\\x:Nat.\\x:Nat.\\x:Nat.x")
    phrase(Term)(tokens) match {
      case Success(trees, _) =>
        try {
          Application.keepPar = false
          println("typed: " + typeof(Nil, trees))
          for (t <- path(trees, reduce))
            println(t)
        } catch {
          case tperror: Throwable => println(tperror.toString)
        }
      case e =>
        println(e)
    }
  }
}