// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/alaamabrouk/ITSD-TeamProject/conf/routes
// @DATE:Wed Feb 26 17:43:06 GMT 2025


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
