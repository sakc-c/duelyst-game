// @GENERATOR:play-routes-compiler
// @SOURCE:C:/Users/rache/Documents/duelystgame/conf/routes
// @DATE:Mon Feb 24 21:02:02 GMT 2025


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
