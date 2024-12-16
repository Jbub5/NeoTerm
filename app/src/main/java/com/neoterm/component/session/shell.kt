package com.neoterm.component.session

import io.neolang.frontend.ConfigVisitor
import com.neoterm.R
import com.neoterm.app.TermuxApplication
import com.termux.terminal.TerminalSession
import com.neoterm.bridge.SessionId
import com.neoterm.component.ComponentManager
import com.neoterm.component.colorscheme.ColorSchemeComponent
import com.neoterm.component.config.DefaultValues
import com.neoterm.component.config.NeoPreference
import com.neoterm.component.config.NeoTermPath
import com.neoterm.component.font.FontComponent
import com.neoterm.component.profile.NeoProfile
import com.neoterm.frontend.session.terminal.TermSessionCallback
import java.io.File

/**
 * @author kiva
 */
class ShellParameter {
  var sessionId: SessionId? = null
  var executablePath: String? = null
  var arguments: Array<String>? = null
  var cwd: String? = null
  var initialCommand: String? = null
  var env: Array<Pair<String, String>>? = null
  var sessionCallback: TerminalSession.SessionChangedCallback? = null

  fun executablePath(executablePath: String?): ShellParameter {
    this.executablePath = executablePath
    return this
  }

  fun arguments(arguments: Array<String>?): ShellParameter {
    this.arguments = arguments
    return this
  }

  //fun currentWorkingDirectory(cwd: String?): ShellParameter {
    //this.cwd = cwd
    //return this
  //}

  fun initialCommand(initialCommand: String?): ShellParameter {
    this.initialCommand = initialCommand
    return this
  }

  fun callback(callback: TerminalSession.SessionChangedCallback?): ShellParameter {
    this.sessionCallback = callback
    return this
  }

  fun session(sessionId: SessionId?): ShellParameter {
    this.sessionId = sessionId
    return this
  }

  fun willCreateNewSession(): Boolean {
    return sessionId?.equals(SessionId.NEW_SESSION) ?: true
  }
}

/**
 * @author kiva
 */
class ShellProfile : NeoProfile() {
  companion object {
    const val PROFILE_META_NAME = "profile-shell"

    private const val INITIAL_COMMAND = "init-command"
    private const val BELL = "bell"
    private const val VIBRATE = "vibrate"
    private const val SPECIAL_VOLUME_KEYS = "special-volume-keys"
    private const val AUTO_COMPLETION = "auto-completion"
    private const val BACK_KEY_TO_ESC = "back-key-esc"
    private const val TOOLBAR = "toolbar"
    private const val EXTRA_KEYS = "extra-keys"
    private const val FONT = "font"
    private const val COLOR_SCHEME = "color-scheme"
    private const val WORD_BASED_IME = "word-based-ime"

    fun create(): ShellProfile {
      return ShellProfile()
    }
  }

  override val profileMetaName = PROFILE_META_NAME

  var initialCommand = DefaultValues.initialCommand

  var enableBell = DefaultValues.enableBell
  var enableVibrate = DefaultValues.enableVibrate
  var enableSpecialVolumeKeys = DefaultValues.enableSpecialVolumeKeys
  var enableAutoCompletion = DefaultValues.enableAutoCompletion
  var enableBackKeyToEscape = DefaultValues.enableBackButtonBeMappedToEscape
  var enableToolbar = DefaultValues.enableToolbar
  var enableExtraKeys = DefaultValues.enableExtraKeys
  var enableWordBasedIme = DefaultValues.enableWordBasedIme

  var profileFont: String
  var profileColorScheme: String

  init {
    val fontComp = ComponentManager.getComponent<FontComponent>()
    val colorComp = ComponentManager.getComponent<ColorSchemeComponent>()

    profileFont = fontComp.getCurrentFontName()
    profileColorScheme = colorComp.getCurrentColorSchemeName()

    initialCommand = NeoPreference.getInitialCommand()
    enableBell = NeoPreference.isBellEnabled()
    enableVibrate = NeoPreference.isVibrateEnabled()
    enableSpecialVolumeKeys = NeoPreference.isSpecialVolumeKeysEnabled()
    enableAutoCompletion = NeoPreference.isAutoCompletionEnabled()
    enableBackKeyToEscape = NeoPreference.isBackButtonBeMappedToEscapeEnabled()
    enableToolbar = NeoPreference.isToolbarEnabled()
    enableExtraKeys = NeoPreference.isExtraKeysEnabled()
    enableWordBasedIme = NeoPreference.isWordBasedImeEnabled()
  }

  override fun onConfigLoaded(configVisitor: ConfigVisitor) {
    super.onConfigLoaded(configVisitor)
    initialCommand = configVisitor.getProfileString(INITIAL_COMMAND, initialCommand)
    enableBell = configVisitor.getProfileBoolean(BELL, enableBell)
    enableVibrate = configVisitor.getProfileBoolean(VIBRATE, enableVibrate)
    enableSpecialVolumeKeys = configVisitor.getProfileBoolean(SPECIAL_VOLUME_KEYS, enableSpecialVolumeKeys)
    enableAutoCompletion = configVisitor.getProfileBoolean(AUTO_COMPLETION, enableAutoCompletion)
    enableBackKeyToEscape = configVisitor.getProfileBoolean(BACK_KEY_TO_ESC, enableBackKeyToEscape)
    enableToolbar = configVisitor.getProfileBoolean(TOOLBAR, enableToolbar)
    enableExtraKeys = configVisitor.getProfileBoolean(EXTRA_KEYS, enableExtraKeys)
    enableWordBasedIme = configVisitor.getProfileBoolean(WORD_BASED_IME, enableWordBasedIme)
    profileFont = configVisitor.getProfileString(FONT, profileFont)
    profileColorScheme = configVisitor.getProfileString(COLOR_SCHEME, profileColorScheme)
  }
}

/**
 * @author kiva
 */
open class ShellTermSession private constructor(
  shellPath: String, cwd: String,
  args: Array<String>, env: Array<String>,
  changeCallback: SessionChangedCallback,
  private val initialCommand: String?,
  val shellProfile: ShellProfile
) : TerminalSession(shellPath, cwd, args, env, changeCallback) {

  var exitPrompt = TermuxApplication.get().getString(R.string.process_exit_prompt)

  override fun initializeEmulator(columns: Int, rows: Int, cellWidthPixels: Int, cellHeightPixels: Int) {
    super.initializeEmulator(columns, rows, cellWidthPixels, cellHeightPixels)
    sendInitialCommand(shellProfile.initialCommand)
    sendInitialCommand(initialCommand)
  }

  override fun getExitDescription(exitCode: Int): String {
    val builder = StringBuilder("\r\n[")
    val context = TermuxApplication.get()
    builder.append(context.getString(R.string.process_exit_info))
    if (exitCode > 0) {
      // Non-zero process exit.
      builder.append(" (")
      builder.append(context.getString(R.string.process_exit_code, exitCode))
      builder.append(")")
    } else if (exitCode < 0) {
      // Negated signal.
      builder.append(" (")
      builder.append(context.getString(R.string.process_exit_signal, -exitCode))
      builder.append(")")
    }
    builder.append(" - $exitPrompt]")
    return builder.toString()
  }

  private fun sendInitialCommand(command: String?) {
    if (command?.isNotEmpty() == true) {
      write(command + '\r')
    }
  }

  class Builder {
    private var executablePath: String? = null
    private var cwd: String? = null
    private var args: MutableList<String>? = null
    private var env: MutableList<Pair<String, String>>? = null
    private var changeCallback: SessionChangedCallback? = null
    private var initialCommand: String? = null
    private var shellProfile = ShellProfile()

    fun initialCommand(command: String?): Builder {
      this.initialCommand = command
      return this
    }

    fun executablePath(shell: String?): Builder {
      this.executablePath = shell
      return this
    }

    fun currentWorkingDirectory(cwd: String?): Builder {
      this.cwd = cwd
      return this
    }

    fun arg(arg: String?): Builder {
      if (arg != null) {
        if (args == null) {
          args = mutableListOf(arg)
        } else {
          args!!.add(arg)
        }
      } else {
        this.args = null
      }
      return this
    }

    fun argArray(args: Array<String>?): Builder {
      if (args != null) {
        if (args.isEmpty()) {
          this.args = null
          return this
        }
        args.forEach { arg(it) }
      } else {
        this.args = null
      }
      return this
    }

    fun env(env: Pair<String, String>?): Builder {
      if (env != null) {
        if (this.env == null) {
          this.env = mutableListOf(env)
        } else {
          this.env!!.add(env)
        }
      } else {
        this.env = null
      }
      return this
    }

    fun envArray(env: Array<Pair<String, String>>?): Builder {
      if (env != null) {
        if (env.isEmpty()) {
          this.env = null
          return this
        }
        env.forEach { env(it) }
      } else {
        this.env = null
      }
      return this
    }

    fun callback(callback: SessionChangedCallback?): Builder {
      this.changeCallback = callback
      return this
    }

    fun create(): ShellTermSession {
      val cwd = this.cwd ?: NeoTermPath.HOME_PATH

      val shell = this.executablePath ?: "${TermuxApplication.get().applicationInfo.nativeLibraryDir}/libstartup.so"

      val args = this.args ?: mutableListOf(shell)
      val env = transformEnvironment(this.env) ?: buildEnvironment(cwd)
      val callback = changeCallback ?: TermSessionCallback()
      return ShellTermSession(
        shell, cwd, args.toTypedArray(), env, callback,
        initialCommand ?: "", shellProfile
      )
    }

    private fun transformEnvironment(env: MutableList<Pair<String, String>>?): Array<String>? {
      if (env == null) {
        return null
      }

      val result = mutableListOf<String>()
      return env.mapTo(result) { "${it.first}=${it.second}" }
        .toTypedArray()
    }


    private fun buildEnvironment(cwd: String?): Array<String> {
      val selectedCwd = cwd ?: NeoTermPath.HOME_PATH
      File(NeoTermPath.HOME_PATH).mkdirs()

      val termEnv = "TERM=xterm-256color"
      val homeEnv = "HOME=" + NeoTermPath.HOME_PATH
      val prefixEnv = "PREFIX=" + NeoTermPath.USR_PATH
      val colorterm = "COLORTERM=truecolor"
      val pathEnv = "PATH=" + buildPathEnv() + ":" + System.getenv("PATH")
      val langEnv = "LANG=en_US.UTF-8"
      val pwdEnv = "PWD=$selectedCwd"
      val tmpdirEnv = "TMPDIR=${NeoTermPath.USR_PATH}/tmp"
      val androidRootEnv = "ANDROID_ROOT=" + System.getenv("ANDROID_ROOT")
      val androidDataEnv = "ANDROID_DATA=" + System.getenv("ANDROID_DATA")
      val externalStorageEnv = "EXTERNAL_STORAGE=" + System.getenv("EXTERNAL_STORAGE")

      val dex2oatbootclasspath = "DEX2OATBOOTCLASSPATH=" + System.getenv("DEX2OATBOOTCLASSPATH")
      val androidi18nroot = "ANDROID_I18N_ROOT=" + System.getenv("ANDROID_I18N_ROOT")
      val bootclasspath = "BOOTCLASSPATH=" + System.getenv("BOOTCLASSPATH")
      val androidtzdatanroot = "ANDROID_TZDATA_ROOT=" + System.getenv("ANDROID_TZDATA_ROOT")
      val androidartroot = "ANDROID_ART_ROOT=" + System.getenv("ANDROID_ART_ROOT")

      // PY Trade: Some programs support NeoTerm in a special way.
      //val neotermIdEnv = "__NEOTERM=1"
      //val originPathEnv = "__NEOTERM_ORIGIN_PATH=" + buildOriginPathEnv()
      //val originLdEnv = "__NEOTERM_ORIGIN_LD_LIBRARY_PATH=" + buildOriginLdLibEnv()
      //val pathEnv = "PATH=" + System.getenv("PATH")
      //val ldEnv = "LD_LIBRARY_PATH=" + buildLdLibraryEnv()

      return arrayOf(
        termEnv, homeEnv, androidRootEnv, androidDataEnv,
        externalStorageEnv, pathEnv, prefixEnv, colorterm,
        langEnv, pwdEnv, tmpdirEnv,
        dex2oatbootclasspath, androidi18nroot, bootclasspath, androidtzdatanroot, androidartroot
      )
        .filter { it.isNotEmpty() }
        .toTypedArray()
    }

    //private fun buildOriginPathEnv(): String {
      //val path = System.getenv("PATH")
      //return path ?: ""
    //}

    //private fun buildOriginLdLibEnv(): String {
      //val path = System.getenv("LD_LIBRARY_PATH")
      //return path ?: ""
    //}

    //private fun buildLdLibraryEnv(): String {
      //return "${NeoTermPath.USR_PATH}/lib"
    //}

    private fun buildPathEnv(): String {
      return "${NeoTermPath.USR_PATH}/bin"
    }
  }
}
