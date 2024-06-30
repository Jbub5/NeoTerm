package com.neoterm.ui.term

import com.termux.terminal.TerminalSession
import com.termux.app.TermuxService

/**
 * @author kiva
 */
object SessionRemover {
  fun removeSession(termService: TermuxService?, tab: TermTab) {
    tab.termData.termSession?.finishIfRunning()
    removeFinishedSession(termService, tab.termData.termSession)
    tab.cleanup()
  }

  private fun removeFinishedSession(termService: TermuxService?, finishedSession: TerminalSession?) {
    if (termService == null || finishedSession == null) {
      return
    }

    termService.removeTermSession(finishedSession)
  }
}
