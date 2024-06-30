package com.neoterm.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.termux.terminal.TerminalSession
import com.neoterm.component.ComponentManager
import com.neoterm.component.config.NeoPreference
import com.neoterm.component.font.FontComponent
import com.neoterm.component.session.SessionComponent
import com.neoterm.component.session.ShellParameter
import com.neoterm.component.session.XParameter
import com.neoterm.component.session.XSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import com.termux.view.extrakey.ExtraKeysView

/**
 * @author kiva
 */
object Terminals {
  fun setupTerminalView(terminalView: TerminalView?, terminalViewClient: TerminalViewClient? = null) {
    terminalView?.textSize = NeoPreference.getFontSize();

    val fontComponent = ComponentManager.getComponent<FontComponent>()
    fontComponent.applyFont(terminalView, null, fontComponent.getCurrentFont())

    if (terminalViewClient != null) {
      terminalView?.setTerminalViewClient(terminalViewClient)
    }
  }

  fun setupExtraKeysView(extraKeysView: ExtraKeysView?) {
    val fontComponent = ComponentManager.getComponent<FontComponent>()
    val font = fontComponent.getCurrentFont()
    fontComponent.applyFont(null, extraKeysView, font)
  }

  fun createSession(context: Context, parameter: ShellParameter): TerminalSession {
    val sessionComponent = ComponentManager.getComponent<SessionComponent>()
    return sessionComponent.createSession(context, parameter)
  }

  fun createSession(activity: AppCompatActivity, parameter: XParameter): XSession {
    val sessionComponent = ComponentManager.getComponent<SessionComponent>()
    return sessionComponent.createSession(activity, parameter)
  }

  fun escapeString(s: String?): String {
    if (s == null) {
      return ""
    }

    val builder = StringBuilder()
    val specialChars = "\"\\$`!"
    builder.append('"')
    val length = s.length
    for (i in 0 until length) {
      val c = s[i]
      if (specialChars.indexOf(c) >= 0) {
        builder.append('\\')
      }
      builder.append(c)
    }
    builder.append('"')
    return builder.toString()
  }
}
