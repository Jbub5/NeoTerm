package com.neoterm.ui.other

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.neoterm.App
import com.neoterm.R



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

    findViewById<View>(R.id.about_version_view).setOnClickListener {
      App.get().easterEgg(this, "Emmmmmm...")
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
