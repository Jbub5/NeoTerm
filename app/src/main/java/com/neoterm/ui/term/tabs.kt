package com.neoterm.ui.term

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.Toolbar
import com.neoterm.R
import com.neoterm.app.TermuxActivity
import com.neoterm.component.ComponentManager
import com.neoterm.component.colorscheme.ColorSchemeComponent
import com.neoterm.component.completion.OnAutoCompleteListener
import com.neoterm.component.config.DefaultValues
import com.neoterm.component.config.NeoPreference
import com.neoterm.frontend.session.terminal.*
import com.neoterm.utils.Terminals
import com.termux.view.TerminalView
import com.termux.view.extrakey.ExtraKeysView
import de.mrapp.android.tabswitcher.Tab
import de.mrapp.android.tabswitcher.TabSwitcher
import de.mrapp.android.tabswitcher.TabSwitcherDecorator
import org.greenrobot.eventbus.EventBus

/**
 * @author kiva
 */
open class NeoTab(title: CharSequence) : Tab(title) {
  open fun onPause() {}
  open fun onResume() {}
  open fun onStart() {}
  open fun onStop() {}
  open fun onWindowFocusChanged(hasFocus: Boolean) {}
  open fun onDestroy() {}
  open fun onConfigurationChanged(newConfig: Configuration) {}
}

class NeoTabDecorator(val context: TermuxActivity) : TabSwitcherDecorator() {
  companion object {
    private var VIEW_TYPE_COUNT = 0
    private val VIEW_TYPE_TERM = VIEW_TYPE_COUNT++
  }

  //private fun setViewLayerType(view: View?) = view?.setLayerType(View.LAYER_TYPE_NONE, null)

  override fun onInflateView(inflater: LayoutInflater, parent: ViewGroup?, viewType: Int): View {
    return when (viewType) {
      VIEW_TYPE_TERM -> {
        val view = inflater.inflate(R.layout.ui_term, parent, false)
        val terminalView = view.findViewById<TerminalView>(R.id.terminal_view)
        val extraKeysView = view.findViewById<ExtraKeysView>(R.id.extra_keys)
        Terminals.setupTerminalView(terminalView)
        Terminals.setupExtraKeysView(extraKeysView)

        val colorSchemeManager = ComponentManager.getComponent<ColorSchemeComponent>()
        colorSchemeManager.applyColorScheme(
          terminalView, extraKeysView,
          colorSchemeManager.getCurrentColorScheme()
        )
        view
      }

      else -> {
        throw RuntimeException("Unknown view type")
      }
    }
  }

  override fun onShowTab(
    context: Context, tabSwitcher: TabSwitcher,
    view: View, tab: Tab, index: Int, viewType: Int, savedInstanceState: Bundle?
  ) {
    // TODO: Improve

    val toolbar = this@NeoTabDecorator.context.toolbar
    toolbar.title = if (tabSwitcher.isSwitcherShown) null else tab.title

    val isQuickPreview = tabSwitcher.selectedTabIndex != index

    when (viewType) {
      VIEW_TYPE_TERM -> {
        val termTab = tab as TermTab
        termTab.toolbar = toolbar
        val terminalView = findViewById<TerminalView>(R.id.terminal_view)
        if (isQuickPreview) {
          bindTerminalView(termTab, terminalView, null)
        } else {
          val extraKeysView = findViewById<ExtraKeysView>(R.id.extra_keys)
          bindTerminalView(termTab, terminalView, extraKeysView)
          terminalView.requestFocus()
        }
      }
    }
  }

  private fun bindTerminalView(
    tab: TermTab, view: TerminalView?,
    extraKeysView: ExtraKeysView?
  ) {
    val termView = view ?: return
    val termData = tab.termData

    termData.initializeViewWith(tab, termView, extraKeysView)
    termView.setEnableWordBasedIme(termData.profile?.enableWordBasedIme ?: DefaultValues.enableWordBasedIme)
    termView.setTerminalViewClient(termData.viewClient)
    termView.attachSession(termData.termSession)

    if (NeoPreference.loadBoolean(R.string.key_general_auto_completion, false)) {
      if (termData.onAutoCompleteListener == null) {
        termData.onAutoCompleteListener = createAutoCompleteListener(termView)
      }
      termView.onAutoCompleteListener = termData.onAutoCompleteListener
    }

    if (termData.termSession != null) {
      termData.viewClient?.updateExtraKeys(termData.termSession?.title, true)
    }
  }

  private fun createAutoCompleteListener(view: TerminalView): OnAutoCompleteListener {
    return TermCompleteListener(view)
  }

  override fun getViewTypeCount(): Int {
    return VIEW_TYPE_COUNT
  }

  override fun getViewType(tab: Tab, index: Int): Int {
    //if (tab is TermTab) {
      return VIEW_TYPE_TERM
    //}
    return -1
  }
}

class TermTab(title: CharSequence) : NeoTab(title), TermUiPresenter {
  //companion object {
    //val PARAMETER_SHOW_EKS = "show_eks"
  //}

  var termData = TermSessionData()
  var toolbar: Toolbar? = null

  fun updateColorScheme() {
    val colorSchemeManager = ComponentManager.getComponent<ColorSchemeComponent>()
    colorSchemeManager.applyColorScheme(
      termData.termView, termData.extraKeysView,
      colorSchemeManager.getCurrentColorScheme()
    )
  }

  fun cleanup() {
    termData.cleanup()
    toolbar = null
  }

  fun onFullScreenModeChanged(fullScreen: Boolean) {
    // Window token changed, we need to recreate PopupWindow
    resetAutoCompleteStatus()
  }

  override fun requireHideIme() {
    val terminalView = termData.termView
    if (terminalView != null) {
      val imm = terminalView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      if (imm.isActive) {
        imm.hideSoftInputFromWindow(terminalView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
      }
    }
  }

  override fun requireFinishAutoCompletion(): Boolean {
    return termData.onAutoCompleteListener?.onFinishCompletion() ?: false
  }

  override fun requireToggleFullScreen() {
    EventBus.getDefault().post(ToggleFullScreenEvent())
  }

  override fun requirePaste() {
    termData.termView?.pasteFromClipboard()
  }

  override fun requireClose() {
    requireHideIme()
    EventBus.getDefault().post(TabCloseEvent(this))
  }

  override fun requireUpdateTitle(title: String?) {
    if (!title.isNullOrEmpty()) {
      this.title = title
      EventBus.getDefault().post(TitleChangedEvent(title))
      termData.viewClient?.updateExtraKeys(title)
    }
  }

  override fun requireOnSessionFinished() {
    // do nothing
  }

  override fun requireCreateNew() {
    EventBus.getDefault().post(CreateNewSessionEvent())
  }

  override fun requireSwitchToPrevious() {
    EventBus.getDefault().post(SwitchSessionEvent(toNext = false))
  }

  override fun requireSwitchToNext() {
    EventBus.getDefault().post(SwitchSessionEvent(toNext = true))
  }

  override fun requireSwitchTo(index: Int) {
    EventBus.getDefault().post(SwitchIndexedSessionEvent(index))
  }

  fun resetAutoCompleteStatus() {
    termData.onAutoCompleteListener?.onCleanUp()
    termData.onAutoCompleteListener = null
  }

  fun resetStatus() {
    resetAutoCompleteStatus()
    termData.extraKeysView?.updateButtons()
    termData.termView?.updateSize()
    termData.termView?.onScreenUpdated()
  }
}
