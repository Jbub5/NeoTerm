package com.neoterm.utils

import android.content.Intent
import com.neoterm.app.TermuxApplication
import com.neoterm.ui.other.CrashActivity

/**
 * @author kiva
 */
object CrashHandler : Thread.UncaughtExceptionHandler {
  private lateinit var defaultHandler: Thread.UncaughtExceptionHandler

  fun init() {
    defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler(this)
  }

  override fun uncaughtException(t: Thread?, e: Throwable?) {
    e?.printStackTrace()

    val intent = Intent(TermuxApplication.get(), CrashActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.putExtra("exception", e)
    TermuxApplication.get().startActivity(intent)
    defaultHandler.uncaughtException(t, e)
  }
}
