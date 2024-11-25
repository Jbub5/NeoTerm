package com.neoterm.component.font

import android.content.Context
import android.graphics.Typeface
import com.neoterm.R
import com.neoterm.app.TermuxApplication
import com.neoterm.component.NeoComponent
import com.neoterm.component.config.DefaultValues
import com.neoterm.component.config.NeoPreference
import com.neoterm.component.config.NeoTermPath
import com.termux.view.TerminalView
import com.termux.view.extrakey.ExtraKeysView
import com.neoterm.utils.extractAssetsDir
import java.io.File

class FontComponent : NeoComponent {
  private lateinit var DEFAULT_FONT: NeoFont
  private lateinit var fonts: MutableMap<String, NeoFont>

  fun applyFont(terminalView: TerminalView?, extraKeysView: ExtraKeysView?, font: NeoFont?) {
    font?.applyFont(terminalView, extraKeysView)
  }

  fun getCurrentFont(): NeoFont {
    return fonts[getCurrentFontName()]!!
  }

  fun setCurrentFont(fontName: String) {
    NeoPreference.store(R.string.key_customization_font, fontName)
  }

  fun getCurrentFontName(): String {
    val defaultFont = DefaultValues.defaultFont
    var currentFontName = NeoPreference.loadString(R.string.key_customization_font, defaultFont)
    if (!fonts.containsKey(currentFontName)) {
      currentFontName = defaultFont
      NeoPreference.store(R.string.key_customization_font, defaultFont)
    }
    return currentFontName
  }

  fun getFont(fontName: String): NeoFont {
    return if (fonts.containsKey(fontName)) fonts[fontName]!! else getCurrentFont()
  }

  fun getFontNames(): List<String> {
    val list = ArrayList<String>()
    list += fonts.keys
    return list
  }

  fun reloadFonts(): Boolean {
    fonts.clear()
    val typeface: Typeface = Typeface.create("monospace", Typeface.NORMAL)
    fonts["Monospace"] = NeoFont(typeface)
    fonts["Sans Serif"] = NeoFont(Typeface.SANS_SERIF)
    fonts["Serif"] = NeoFont(Typeface.SERIF)
    val fontDir = File(NeoTermPath.FONT_PATH)
    for (file in fontDir.listFiles { pathname -> pathname.name.endsWith(".ttf") }) {
      val fontName = fontName(file)
      val font = NeoFont(file)
      fonts[fontName] = font
    }

    val defaultFont = DefaultValues.defaultFont
    if (fonts.containsKey(defaultFont)) {
      DEFAULT_FONT = fonts[defaultFont]!!
      return true
    }
    return false
  }

  override fun onServiceInit() {
    checkForFiles()
  }

  override fun onServiceDestroy() {
  }

  override fun onServiceObtained() {
    checkForFiles()
  }

  private fun loadDefaultFontFromAsset(context: Context): NeoFont {
    val defaultFont = DefaultValues.defaultFont
    return NeoFont(Typeface.createFromAsset(context.assets, "fonts/$defaultFont.ttf"))
  }

  private fun extractDefaultFont(context: Context): Boolean {
    try {
      context.extractAssetsDir( "fonts", NeoTermPath.FONT_PATH)
      return true
    } catch (e: Exception) {
      return false
    }
  }

  private fun fontFile(fontName: String): File {
    return File("${NeoTermPath.FONT_PATH}/$fontName.ttf")
  }

  private fun fontName(fontFile: File): String {
    return fontFile.nameWithoutExtension
  }

  private fun checkForFiles() {
    File(NeoTermPath.FONT_PATH).mkdirs()
    fonts = mutableMapOf()

    val context = TermuxApplication.get()
    val defaultFont = DefaultValues.defaultFont
    val defaultFontFile = fontFile(defaultFont)

    if (!defaultFontFile.exists()) {
      if (!extractDefaultFont(context)) {
        DEFAULT_FONT = loadDefaultFontFromAsset(context)
        fonts[defaultFont] = DEFAULT_FONT
        return
      }
    }

    if (!reloadFonts()) {
      DEFAULT_FONT = loadDefaultFontFromAsset(context)
      fonts[defaultFont] = DEFAULT_FONT
    }
  }
}

