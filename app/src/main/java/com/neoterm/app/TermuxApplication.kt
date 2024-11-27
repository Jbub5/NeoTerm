package com.neoterm.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.neoterm.R
import com.neoterm.component.NeoInitializer
import com.neoterm.component.config.NeoPreference
import com.neoterm.utils.CrashHandler

/**
 * @author kiva
 */
class TermuxApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    app = this
    NeoPreference.init(this)
    CrashHandler.init()
    NeoInitializer.init(this)
  }

  fun errorDialog(context: Context, message: Int, dismissCallback: (() -> Unit)?) {
    errorDialog(context, getString(message), dismissCallback)
  }

  fun errorDialog(context: Context, message: String, dismissCallback: (() -> Unit)?) {
    AlertDialog.Builder(context)
      .setTitle(R.string.error)
      .setMessage(message)
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(R.string.show_help) { _, _ ->
        openHelpLink()
      }
      .setOnDismissListener {
        dismissCallback?.invoke()
      }
      .show()
  }

  fun openHelpLink() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://neoterm.gitbooks.io/neoterm-wiki/content/"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }

  companion object {
    private var app: TermuxApplication? = null

    fun get(): TermuxApplication {
      return app!!
    }
  }
}
