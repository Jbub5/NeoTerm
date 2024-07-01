package com.neoterm.component.session

import android.content.Context
import com.neoterm.component.NeoComponent

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
      .envArray(parameter.env)
      .argArray(parameter.arguments)
      .initialCommand(parameter.initialCommand)
      .create()
  }
}
