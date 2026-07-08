object StringOps {

  implicit class RichString(val s: String) extends AnyVal {
    def indentLevels(levels: Int): String = s.split("\n").map(s => s"${"  " * levels}$s").mkString("\n")
  }

}
