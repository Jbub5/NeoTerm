package com.neoterm.component.session

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.neoterm.component.NeoComponent
import com.neoterm.component.config.NeoTermPath
import com.neoterm.utils.NLog

class SessionComponent : NeoComponent {

  override fun onServiceInit() {
  }

  override fun onServiceDestroy() {
  }

  override fun onServiceObtained() {
  }

  fun createSession(context: Context, parameter: ShellParameter): ShellTermSession {
    return ShellTermSession.Builder()
      .executablePath(parameter.executablePath)
      .currentWorkingDirectory(parameter.cwd)
      .callback(parameter.sessionCallback)
      .systemShell(parameter.systemShell)
      .envArray(parameter.env)
      .argArray(parameter.arguments)
      .initialCommand(parameter.initialCommand)
      .profile(parameter.shellProfile)
      .create()
  }
}
