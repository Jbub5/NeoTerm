package com.neoterm.ui.other

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.neoterm.R

class LicensesDialog : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = Dialog(requireContext())
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(true)

    val view = LayoutInflater.from(context).inflate(R.layout.dialog_third_party_libraries, null)
    dialog.setContentView(view)

    val textViewMessage: TextView = view.findViewById(R.id.textViewMessage)
    val buttonClose: Button = view.findViewById(R.id.buttonClose)

    val text = """
    - <a href='https://github.com/Crixec/ADBToolKitsInstaller'>ADBToolkitInstaller</a> (GPLv3)<br/>
    - <a href='https://github.com/jackpal/Android-Terminal-Emulator'>Android-Terminal-Emulator</a> (Apache 2.0)<br/>
    - <a href='https://github.com/michael-rapp/ChromeLikeTabSwitcher'>ChromeLikeTabSwitcher</a> (Apache 2.0)<br/>
    - <a href='https://github.com/GrenderG/Color-O-Matic'>Color-O-Matic</a> (GPLv3)<br/>
    - <a href='https://github.com/greenrobot/EventBus'>EventBus</a> (Apache 2.0)<br/>
    - <a href='https://github.com/xaverkapeller/ModularAdapter'>ModularAdapter</a> (MIT)<br/>
    - <a href='https://github.com/NeoTerrm/NeoTerm'>NeoTerm</a> (GPLv3)<br/>
    - <a href='https://github.com/tiann/NeoTerm'>NeoTerm [Fork]</a> (GPLv3)<br/>
    - <a href='https://gitlab.com/kalilinux/nethunter/apps/kali-nethunter-term'>NetHunter Terminal</a> (GPLv3)<br/>
    - <a href='https://github.com/nshmura/RecyclerTabLayout'>RecyclerTabLayout</a> (Apache 2.0)<br/>
    - <a href='https://github.com/timusus/RecyclerView-FastScroll'>RecyclerView-FastScroll</a> (Apache 2.0)<br/>
    - <a href='https://github.com/xaverkapeller/SortedListAdapter'>SortedListAdapter</a> (MIT)<br/>
    - <a href='https://github.com/termux/termux-app'>Termux</a> (GPLv3)
    """.trimIndent()

    textViewMessage.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
    textViewMessage.movementMethod = LinkMovementMethod.getInstance()

    buttonClose.setOnClickListener {
      dialog.dismiss()
    }

    return dialog
  }

  fun showLicenses(context: Context) {
    this.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "LicensesDialog")
  }
}

/**
 * @author kiva
 */
class AboutActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.ui_about)
    setSupportActionBar(findViewById(R.id.about_toolbar))
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    try {
      val versionPrefix = getString(R.string.about_version_label)
      val version = packageManager.getPackageInfo(packageName, 0).versionName
      (findViewById<TextView>(R.id.app_version)).text = "$versionPrefix $version"
    } catch (ignored: PackageManager.NameNotFoundException) {
    }

    findViewById<View>(R.id.about_licenses_view).setOnClickListener {
      LicensesDialog().showLicenses(this)
    }

    findViewById<View>(R.id.about_source_code_view).setOnClickListener {
      openUrl("https://github.com/Jbub5/NeoTerm")
    }

    findViewById<View>(R.id.about_reset_app_view).setOnClickListener {
      AlertDialog.Builder(this)
        .setMessage(R.string.reset_app_warning)
        .setPositiveButton(android.R.string.ok) { _, _ ->
          resetApp()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    }
  }

  private fun resetApp() {
    startActivity(Intent(this, SetupActivity::class.java))
  }

  private fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home ->
        finish()
    }
    return super.onOptionsItemSelected(item)
  }
}
