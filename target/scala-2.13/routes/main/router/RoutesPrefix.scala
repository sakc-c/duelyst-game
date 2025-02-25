// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/sakshijashnani/Desktop/SD Team Project/ITSD-DT2023-24-Template/conf/routes
// @DATE:Fri Feb 07 21:31:19 GMT 2025


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
