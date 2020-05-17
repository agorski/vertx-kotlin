package ago.kotrx

import java.net.ServerSocket

object TestUtils {
  fun getFreePort(): Int {
    val s = ServerSocket(0)
    val portAssigned = s.localPort
    s.close()
    return portAssigned
  }
}
