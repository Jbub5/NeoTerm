package com.neoterm.utils

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * @author kiva
 */
object NeoPermission {
  const val REQUEST_APP_PERMISSION = 10086

  fun initAppPermission(context: AppCompatActivity, requestCode: Int) {
    if (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
      )
      != PackageManager.PERMISSION_GRANTED
    ) {

      AlertDialog.Builder(context)
        .setMessage("Please enable Storage permission")
        .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
          doRequestPermission(context, requestCode)
        }
        .show()
    }
  }

  private fun doRequestPermission(context: AppCompatActivity, requestCode: Int) {
    if (Build.VERSION.SDK_INT >= 30) { // if android 11+ request MANAGER_EXTERNAL_STORAGE
      val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
        data = Uri.parse("package:${context.packageName}")
      }

      if (intent.resolveActivity(context.packageManager) != null) {
        (context as? Activity)?.startActivityForResult(intent, requestCode)
      } else {
        // Use fallback if matching Activity did not exist for ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION.
        val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        (context as? Activity)?.startActivityForResult(fallbackIntent, requestCode)
      }
    }

    try {
      ActivityCompat.requestPermissions(
        context,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
        requestCode
      )
    } catch (ignore: ActivityNotFoundException) {
      // for MIUI, we ignore it.
    }
  }
}
