package com.neoterm.ui.other

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.neoterm.R


class OpenSourceLicensesDialog : DialogFragment() {
  @SuppressLint("CommitTransaction")
  fun showLicenses(activity: AppCompatActivity) {
    val fragmentManager = activity.supportFragmentManager
    val fragmentTransaction = fragmentManager.beginTransaction()
    val previousFragment = fragmentManager.findFragmentByTag("dialog_licenses")
    if (previousFragment != null) {
      fragmentTransaction.remove(previousFragment)
    }
    fragmentTransaction.addToBackStack(null)

    show(fragmentManager, "dialog_licenses")
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val webView = WebView(requireActivity())
    webView.loadUrl("file:///android_asset/licenses.html")

    return AlertDialog.Builder(requireActivity())
      .setTitle(R.string.about_libraries_label)
      .setView(webView)
      .setPositiveButton(android.R.string.yes) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
      .create()
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
      val version = packageManager.getPackageInfo(packageName, 0).versionName
      (findViewById<TextView>(R.id.app_version)).text = version
    } catch (ignored: PackageManager.NameNotFoundException) {
    }

    findViewById<View>(R.id.about_licenses_view).setOnClickListener {
      OpenSourceLicensesDialog().showLicenses(this)
    }

    findViewById<View>(R.id.about_source_code_view).setOnClickListener {
      openUrl("https://github.com/NeoTerm/NeoTerm")
    }

    findViewById<View>(R.id.about_reset_app_view).setOnClickListener {
      AlertDialog.Builder(this)
        .setMessage(R.string.reset_app_warning)
        .setPositiveButton(android.R.string.yes) { _, _ ->
          resetApp()
        }
        .setNegativeButton(android.R.string.no, null)
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

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item?.itemId) {
      android.R.id.home ->
        finish()
    }
    return super.onOptionsItemSelected(item)
  }
}
