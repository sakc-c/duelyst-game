// @GENERATOR:play-routes-compiler
// @SOURCE:C:/Users/rache/Documents/duelystgame/conf/routes
// @DATE:Fri Feb 28 23:32:37 GMT 2025


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
