package com.neoterm.ui.term

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.neoterm.R
import com.neoterm.app.TermuxActivity
import com.neoterm.app.TermuxApplication
import com.neoterm.bridge.Bridge.*
import com.neoterm.bridge.SessionId
import com.neoterm.component.config.NeoPreference
import com.neoterm.component.session.ShellParameter
import com.neoterm.frontend.session.terminal.TermSessionCallback
import com.neoterm.utils.Terminals
import com.neoterm.utils.getPathOfMediaUri
import com.termux.app.TermuxService
import java.io.File

/**
 * @author kiva
 */
class NeoTermRemoteInterface : AppCompatActivity(), ServiceConnection {
  private var termService: TermuxService? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val serviceIntent = Intent(this, TermuxService::class.java)
    startService(serviceIntent)
    if (!bindService(serviceIntent, this, 0)) {
      TermuxApplication.get().errorDialog(this, R.string.service_connection_failed) { finish() }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (termService != null) {
      if (termService!!.sessions.isEmpty()) {
        termService!!.stopSelf()
      }
      termService = null
      unbindService(this)
    }
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    if (termService != null) {
      finish()
    }
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    termService = (service as TermuxService.NeoTermBinder).service
    if (termService == null) {
      finish()
      return
    }

    handleIntent()
  }

  private fun handleIntent() = when (intent.component?.className?.substringAfterLast('.')) {
    "TermHere" -> handleTermHere()
    else -> handleNormal()
  }

  private fun handleNormal() {
    when (intent.action) {
      ACTION_EXECUTE -> {
        if (!intent.hasExtra(EXTRA_COMMAND)) {
          TermuxApplication.get().errorDialog(this, R.string.no_command_extra)
          { finish() }
          return
        }
        val command = intent.getStringExtra(EXTRA_COMMAND)
        val foreground = intent.getBooleanExtra(EXTRA_FOREGROUND, true)
        val session = intent.getStringExtra(EXTRA_SESSION_ID)

        openTerm(command, SessionId.of(session), foreground)
      }

      else -> openTerm(null, null)
    }
    finish()
  }

  private fun handleTermHere() {
    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
      val extra = intent.extras?.get(Intent.EXTRA_STREAM)
      if (extra is Uri) {
        val path = this.getPathOfMediaUri(extra)
        val file = File(path)
        val dirPath = if (file.isDirectory) path else file.parent
        val command = "cd " + Terminals.escapeString(dirPath)
        openTerm(command, null)
      }
      finish()
    } else {
      TermuxApplication.get().errorDialog(
        this,
        getString(R.string.unsupported_term_here, intent?.toString())
      ) {
        finish()
      }
    }
  }

  private fun openTerm(
    parameter: ShellParameter,
    foreground: Boolean = true
  ) {
    val session = termService!!.createTermSession(parameter)

    val data = Intent()
    data.putExtra(EXTRA_SESSION_ID, session.mHandle)
    setResult(AppCompatActivity.RESULT_OK, data)

    if (foreground) {
      // Set current session to our new one
      // In order to switch to it when entering TermuxActivity
      NeoPreference.storeCurrentSession(session)

      val intent = Intent(this, TermuxActivity::class.java)
      intent.addCategory(Intent.CATEGORY_DEFAULT)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      startActivity(intent)
    }
  }

  private fun openTerm(
    initialCommand: String?,
    sessionId: SessionId? = null,
    foreground: Boolean = true
  ) {
    val parameter = ShellParameter()
      .initialCommand(initialCommand)
      .callback(TermSessionCallback())
      .session(sessionId)
    openTerm(parameter, foreground)
  }

  /* private fun openCustomExecTerm(executablePath: String?, arguments: Array<String>?, cwd: String?) {
    val parameter = ShellParameter()
      .executablePath(executablePath)
      .arguments(arguments)
      .currentWorkingDirectory(cwd)
      .callback(TermSessionCallback())
    openTerm(parameter)
  } */
}
