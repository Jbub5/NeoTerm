package com.neoterm.app

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import com.neoterm.R
import com.neoterm.component.config.NeoPreference
import com.neoterm.component.session.ShellParameter
import com.neoterm.frontend.session.terminal.*
import com.neoterm.ui.pm.PackageManagerActivity
import com.neoterm.ui.settings.SettingActivity
import com.neoterm.ui.term.NeoTab
import com.neoterm.ui.term.NeoTabDecorator
import com.neoterm.ui.term.SessionRemover
import com.neoterm.ui.term.TermTab
import com.neoterm.utils.FullScreenHelper
import com.neoterm.utils.NeoPermission
import com.neoterm.utils.RangedInt
import com.termux.app.TermuxService
import com.termux.terminal.TerminalSession
import de.mrapp.android.tabswitcher.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class TermuxActivity : AppCompatActivity(), ServiceConnection, SharedPreferences.OnSharedPreferenceChangeListener {
  companion object {
    const val KEY_NO_RESTORE = "no_restore"
    const val REQUEST_SETUP = 22313
  }

  lateinit var tabSwitcher: TabSwitcher
  private lateinit var fullScreenHelper: FullScreenHelper
  lateinit var toolbar: Toolbar

  var addSessionListener = createAddSessionListener()
  private var termService: TermuxService? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val SDCARD_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1

    NeoPermission.initAppPermission(this, NeoPermission.REQUEST_APP_PERMISSION)

    if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE,
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), SDCARD_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
    }

    val fullscreen = NeoPreference.isFullScreenEnabled()
    if (fullscreen) {
      window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
      )
    }

    setContentView(R.layout.ui_main)

    toolbar = findViewById(R.id.terminal_toolbar)
    setSupportActionBar(toolbar)

    fullScreenHelper = FullScreenHelper.injectActivity(this, fullscreen, peekRecreating())
    fullScreenHelper.setKeyBoardListener(object : FullScreenHelper.KeyBoardListener {
      override fun onKeyboardChange(isShow: Boolean, keyboardHeight: Int) {
        if (tabSwitcher.selectedTab is TermTab) {
          // isShow -> toolbarHide
          toggleToolbar((tabSwitcher.selectedTab as? TermTab)?.toolbar, !isShow)
        }
      }
    })

    tabSwitcher = findViewById(R.id.tab_switcher)
    tabSwitcher.decorator = NeoTabDecorator(this)
    ViewCompat.setOnApplyWindowInsetsListener(tabSwitcher, createWindowInsetsListener())
    tabSwitcher.showToolbars(false)

    val serviceIntent = Intent(this, TermuxService::class.java)
    startService(serviceIntent)
    bindService(serviceIntent, this, 0)
  }

  private fun toggleToolbar(toolbar: Toolbar?, visible: Boolean) {
    if (toolbar == null) {
      return
    }

    if (NeoPreference.isFullScreenEnabled()
      || NeoPreference.isHideToolbarEnabled()
    ) {
      val toolbarHeight = toolbar.height.toFloat()
      val translationY = if (visible) 0.toFloat() else -toolbarHeight
      if (visible) {
        toolbar.visibility = View.VISIBLE
        toolbar.animate()
          .translationY(translationY)
          .start()
      } else {
        toolbar.animate()
          .translationY(translationY)
          .withEndAction {
            toolbar.visibility = View.GONE
          }
          .start()
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)

    TabSwitcher.setupWithMenu(tabSwitcher, toolbar.menu) {
      if (!tabSwitcher.isSwitcherShown) {
        val imm = this@TermuxActivity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive && tabSwitcher.selectedTab is TermTab) {
          (tabSwitcher.selectedTab as? TermTab)?.requireHideIme()
        }
        toggleSwitcher(showSwitcher = true)
      } else {
        toggleSwitcher(showSwitcher = false)
      }
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    return when (item?.itemId) {
      R.id.menu_item_settings -> {
        startActivity(Intent(this, SettingActivity::class.java))
        true
      }
      R.id.menu_item_package_settings -> {
        startActivity(Intent(this, PackageManagerActivity::class.java))
        true
      }
      R.id.menu_item_new_session -> {
        addNewSession()
        true
      }
      R.id.menu_item_new_rescue_session -> {
        addNewRescueSession("Rescue Shell")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onPause() {
    super.onPause()
    val tab = tabSwitcher.selectedTab as NeoTab?
    tab?.onPause()
  }

  override fun onResume() {
    super.onResume()
    PreferenceManager.getDefaultSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(this)
    tabSwitcher.addListener(object : TabSwitcherListener {
      override fun onSwitcherShown(tabSwitcher: TabSwitcher) {
        toolbar.setNavigationIcon(R.drawable.ic_add_box_white_24dp)
        toolbar.setNavigationOnClickListener(addSessionListener)
        toolbar.setBackgroundResource(android.R.color.transparent)
        toolbar.animate().alpha(0f).setDuration(300).withEndAction {
          toolbar.alpha = 1f
        }.start()
      }

      override fun onSwitcherHidden(tabSwitcher: TabSwitcher) {
        toolbar.navigationIcon = null
        toolbar.setNavigationOnClickListener(null)
        toolbar.setBackgroundResource(R.color.colorPrimary)
      }

      override fun onSelectionChanged(tabSwitcher: TabSwitcher, selectedTabIndex: Int, selectedTab: Tab?) {
        if (selectedTab is TermTab && selectedTab.termData.termSession != null) {
          NeoPreference.storeCurrentSession(selectedTab.termData.termSession!!)
        }
      }

      override fun onTabAdded(tabSwitcher: TabSwitcher, index: Int, tab: Tab, animation: Animation) {
        update_colors()
      }

      override fun onTabRemoved(tabSwitcher: TabSwitcher, index: Int, tab: Tab, animation: Animation) {
        SessionRemover.removeSession(termService, tab as TermTab)
      }

      override fun onAllTabsRemoved(tabSwitcher: TabSwitcher, tabs: Array<out Tab>, animation: Animation) {
      }
    })
    val tab = tabSwitcher.selectedTab as NeoTab?
    tab?.onResume()
  }

  override fun onStart() {
    super.onStart()
    EventBus.getDefault().register(this)
    val tab = tabSwitcher.selectedTab as NeoTab?
    tab?.onStart()
  }

  override fun onStop() {
    super.onStop()
    // After stopped, window locations may changed
    // Rebind it at next time.
    forEachTab<TermTab> { it.resetAutoCompleteStatus() }
    val tab = tabSwitcher.selectedTab as NeoTab?
    tab?.onStop()
    EventBus.getDefault().unregister(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    val tab = tabSwitcher.selectedTab as NeoTab?
    tab?.onDestroy()
    PreferenceManager.getDefaultSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(this)

    if (termService != null) {
      if (termService!!.sessions.isEmpty()) {
        termService!!.stopSelf()
      }
      termService = null
    }
    unbindService(this)
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    val tab = tabSwitcher.selectedTab as NeoTab?
    tab?.onWindowFocusChanged(hasFocus)
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_BACK -> {
        if (event?.action == KeyEvent.ACTION_DOWN && tabSwitcher.isSwitcherShown && tabSwitcher.count > 0) {
          toggleSwitcher(showSwitcher = false)
          return true
        }
      }
      KeyEvent.KEYCODE_MENU -> {
        if (toolbar.isOverflowMenuShowing) {
          toolbar.hideOverflowMenu()
        } else {
          toolbar.showOverflowMenu()
        }
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      NeoPermission.REQUEST_APP_PERMISSION -> {
        if (grantResults.isEmpty()
          || grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
          AlertDialog.Builder(this).setMessage(R.string.permission_denied)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
              finish()
            }
            .show()
        }
        return
      }
    }
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    if (key == getString(R.string.key_ui_fullscreen)) {
      setFullScreenMode(NeoPreference.isFullScreenEnabled())
    } else if (key == getString(R.string.key_customization_color_scheme)) {
      if (tabSwitcher.count > 0) {
          (tabSwitcher.selectedTab as? TermTab)?.updateColorScheme()
      }
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

    if (!isRecreating()) {
      enterMain()
      update_colors()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_SETUP -> {
        when (resultCode) {
          RESULT_OK -> enterMain()
          RESULT_CANCELED -> {
            addNewSession()
          }
        }
      }
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    // When rotate the screen, extra keys may get updated.
    forEachTab<NeoTab> {
      it.onConfigurationChanged(newConfig)
      if (it is TermTab) {
        it.resetStatus()
      }
    }
  }

  private fun enterMain() {

    if (termService!!.sessions.isNotEmpty()) {
      val lastSession = getStoredCurrentSessionOrLast()

      for (session in termService!!.sessions) {
        addNewSessionFromExisting(session)
      }

      if (intent?.action == Intent.ACTION_RUN) {
        // app shortcuts
        addNewSession(
          createRevealAnimation()
        )
      } else {
        switchToSession(lastSession)
      }

    } else {
      toggleSwitcher(showSwitcher = true)
      // Fore system shell mode to be disabled.
      addNewSession(createRevealAnimation())
    }
  }

  override fun recreate() {
    NeoPreference.store(KEY_NO_RESTORE, true)
    super.recreate()
  }

  private fun isRecreating(): Boolean {
    val result = peekRecreating()
    if (result) {
      NeoPreference.store(KEY_NO_RESTORE, !result)
    }
    return result
  }

  private fun peekRecreating(): Boolean {
    return NeoPreference.loadBoolean(KEY_NO_RESTORE, false)
  }

  private fun setFullScreenMode(fullScreen: Boolean) {
    fullScreenHelper.fullScreen = fullScreen
    if (tabSwitcher.selectedTab is TermTab) {
      (tabSwitcher.selectedTab as? TermTab)?.requireHideIme()
      (tabSwitcher.selectedTab as? TermTab)?.onFullScreenModeChanged()
    }
    NeoPreference.store(R.string.key_ui_fullscreen, fullScreen)
    this@TermuxActivity.recreate()
  }

  private fun addNewSession() = addNewSessionWithProfile()

  //private fun addNewSession(sessionName: String?, animation: Animation) =
    //addNewSessionWithProfile(sessionName, animation)
  private fun addNewSession(animation: Animation) =
    addNewSessionWithProfile(null, animation)

  private fun addNewSessionWithProfile() {
    if (!tabSwitcher.isSwitcherShown) {
      toggleSwitcher(showSwitcher = true)
    }
    addNewSessionWithProfile(
      null,
      createRevealAnimation()
    )
  }

  private fun addNewSessionWithProfile(
    sessionName: String?,
    animation: Animation
  ) {
    val sessionCallback = TermSessionCallback()
    val viewClient = TermViewClient(this)

    val parameter = ShellParameter()
      .callback(sessionCallback)
    val session = termService!!.createTermSession(parameter)

    session.mSessionName = sessionName ?: generateSessionName("NeoTerm")

    val tab = createTab(session.mSessionName) as TermTab
    tab.termData.initializeSessionWith(session, sessionCallback, viewClient)

    addNewTab(tab, animation)
    switchToSession(tab)
  }

  private fun addNewSessionFromExisting(session: TerminalSession?) {
    if (session == null) {
      return
    }

    // Do not add the same session again
    // Or app will crash when rotate
    val tabCount = tabSwitcher.count
    (0..(tabCount - 1))
      .map { tabSwitcher.getTab(it) }
      .filter { it is TermTab && it.termData.termSession == session }
      .forEach { return }

    val sessionCallback = session.sessionChangedCallback as TermSessionCallback
    val viewClient = TermViewClient(this)

    val tab = createTab(session.title) as TermTab
    tab.termData.initializeSessionWith(session, sessionCallback, viewClient)

    addNewTab(tab, createRevealAnimation())
    switchToSession(tab)
  }

  private fun addNewRescueSession(sessionName: String?) {
    val sessionCallback = TermSessionCallback()
    val viewClient = TermViewClient(this)

    val parameter = ShellParameter()
      .callback(sessionCallback)
      .executablePath("/system/bin/sh")
    val session = termService!!.createTermSession(parameter)

    session.mSessionName = sessionName ?: generateSessionName("Rescue Shell")

    val tab = createTab(session.mSessionName) as TermTab
    tab.termData.initializeSessionWith(session, sessionCallback, viewClient)

    addNewTab(tab, createRevealAnimation())
    switchToSession(tab)
  }

  private fun generateSessionName(prefix: String): String {
    return "$prefix #${termService!!.sessions.size}"
  }

  private fun switchToSession(session: TerminalSession?) {
    if (session == null) {
      return
    }

    for (i in 0 until tabSwitcher.count) {
      val tab = tabSwitcher.getTab(i) as TermTab
      if (tab.termData.termSession == session) {
        switchToSession(tab)
        break
      }
    }
  }

  private fun switchToSession(tab: Tab?) {
    if (tab == null) {
      return
    }
    tabSwitcher.selectTab(tab)
  }

  private fun addNewTab(tab: Tab, animation: Animation) {
    tabSwitcher.addTab(tab, 0, animation)
  }

  private fun getStoredCurrentSessionOrLast(): TerminalSession? {
    val stored = NeoPreference.getCurrentSession(termService)
    if (stored != null) return stored
    val numberOfSessions = termService!!.sessions.size
    if (numberOfSessions == 0) return null
    return termService!!.sessions[numberOfSessions - 1]
  }

  private fun createAddSessionListener(): View.OnClickListener {
    return View.OnClickListener {
      addNewSession()
    }
  }

  private fun createTab(tabTitle: String?): Tab {
    return postTabCreated(TermTab(tabTitle ?: "NeoTerm"))
  }

  private fun <T : NeoTab> postTabCreated(tab: T): T {
    // We must create a Bundle for each tab
    // tabs can use them to store status.
    tab.parameters = Bundle()

    tab.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_background_color))
    tab.setTitleTextColor(ContextCompat.getColor(this, R.color.tab_title_text_color))
    return tab
  }

  private fun createRevealAnimation(): Animation {
    var x = 0f
    var y = 0f
    val view = getNavigationMenuItem()

    if (view != null) {
      val location = IntArray(2)
      view.getLocationInWindow(location)
      x = location[0] + view.width / 2f
      y = location[1] + view.height / 2f
    }

    return RevealAnimation.Builder().setX(x).setY(y).create()
  }

  private fun getNavigationMenuItem(): View? {
    val toolbars = tabSwitcher.toolbars

    if (toolbars != null) {
      val toolbar = if (toolbars.size > 1) toolbars[1] else toolbars[0]
      val size = toolbar.childCount

      (0 until size)
        .map { toolbar.getChildAt(it) }
        .filterIsInstance<ImageButton>()
        .forEach { return it }
    }

    return null
  }

  private fun createWindowInsetsListener(): OnApplyWindowInsetsListener {
    return OnApplyWindowInsetsListener { _, insets ->
      tabSwitcher.setPadding(
        insets.systemWindowInsetLeft,
        insets.systemWindowInsetTop, insets.systemWindowInsetRight,
        insets.systemWindowInsetBottom
      )
      insets
    }
  }

  private fun toggleSwitcher(showSwitcher: Boolean) {
    if (showSwitcher) {
      tabSwitcher.showSwitcher()
    } else {
      tabSwitcher.hideSwitcher()
    }
  }

  private inline fun <reified T> forEachTab(callback: (T) -> Unit) {
    (0 until tabSwitcher.count)
      .map { tabSwitcher.getTab(it) }
      .filterIsInstance(T::class.java)
      .forEach(callback)
  }

  @Suppress("unused")
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onTabCloseEvent(tabCloseEvent: TabCloseEvent) {
    val tab = tabCloseEvent.termTab
    toggleSwitcher(showSwitcher = true)
    tabSwitcher.removeTab(tab)

    if (tabSwitcher.count > 1) {
      var index = tabSwitcher.indexOf(tab)
      if (NeoPreference.isNextTabEnabled()) {
        // 关闭当前窗口后，向下一个窗口切换
        if (--index < 0) index = tabSwitcher.count - 1
      } else {
        // 关闭当前窗口后，向上一个窗口切换
        if (++index >= tabSwitcher.count) index = 0
      }
      switchToSession(tabSwitcher.getTab(index))
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onToggleFullScreenEvent(toggleFullScreenEvent: ToggleFullScreenEvent) {
    val fullScreen = fullScreenHelper.fullScreen
    setFullScreenMode(!fullScreen)
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onToggleImeEvent(toggleImeEvent: ToggleImeEvent) {
    if (!tabSwitcher.isSwitcherShown) {
      val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
      imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onTitleChangedEvent(titleChangedEvent: TitleChangedEvent) {
    if (!tabSwitcher.isSwitcherShown) {
      toolbar.title = titleChangedEvent.title
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onCreateNewSessionEvent(createNewSessionEvent: CreateNewSessionEvent) {
    addNewSession()
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onSwitchSessionEvent(switchSessionEvent: SwitchSessionEvent) {
    if (tabSwitcher.count < 2) {
      return
    }

    val rangedInt = RangedInt(tabSwitcher.selectedTabIndex, (0 until tabSwitcher.count))
    val nextIndex = if (switchSessionEvent.toNext) rangedInt.inc() else rangedInt.dec()
    if (!tabSwitcher.isSwitcherShown) {
      tabSwitcher.showSwitcher()
    }
    switchToSession(tabSwitcher.getTab(nextIndex))
  }

  @Suppress("unused", "UNUSED_PARAMETER")
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onSwitchIndexedSessionEvent(switchIndexedSessionEvent: SwitchIndexedSessionEvent) {
    val nextIndex = switchIndexedSessionEvent.index - 1
    if (nextIndex in (0 until tabSwitcher.count) && nextIndex != tabSwitcher.selectedTabIndex) {
      // Do not show animation here, users may get tired
      switchToSession(tabSwitcher.getTab(nextIndex))
    }
  }

  fun update_colors() {
    // Simple fix to bug on custom color
    Handler().postDelayed({

      if (tabSwitcher.count > 0) {
        (tabSwitcher.selectedTab as? TermTab)?.updateColorScheme()
      }

    }, 500)
  }

}
