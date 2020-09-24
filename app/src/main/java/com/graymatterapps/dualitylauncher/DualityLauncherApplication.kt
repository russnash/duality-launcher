package com.graymatterapps.dualitylauncher

import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.graymatterapps.graymatterutils.GrayMatterUtils
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraMailSender
import org.acra.data.StringFormat
import java.io.File
import java.io.FileOutputStream
import java.util.*

lateinit var appWidgetManager: AppWidgetManager
lateinit var appWidgetHost: AppWidgetHost
lateinit var settingsPreferences: SharedPreferences
lateinit var prefs: SharedPreferences
lateinit var appList: AppList
lateinit var appContext: Context
lateinit var widgetDB: WidgetDB
lateinit var replicator: Replicator
lateinit var dragAndDropData: DragAndDropData
lateinit var dualWallpaper: DualWallpaper
lateinit var dualityLauncherApplication: DualityLauncherApplication
lateinit var mainScreen: View
lateinit var mainContext: Context
lateinit var dualScreen: View

@AcraCore(buildConfigClass = org.acra.BuildConfig::class, reportFormat = StringFormat.JSON)
@AcraMailSender(mailTo = "russnash37@gmail.com", reportAsFile = true)
@AcraDialog(resText = R.string.acra_dialog_text)
class DualityLauncherApplication: Application() {
    val TAG = javaClass.simpleName

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        ACRA.DEV_LOGGING = true
        ACRA.init(this)
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        dualityLauncherApplication = this
        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        /* try {
            val editor = prefs.edit()
            editor.remove("widgetSizes")
            editor.apply()
        } catch (e: Exception) {
            // Do nothing
        } */
        appList = AppList(applicationContext)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetHost = AppWidgetHost(applicationContext, 1)
        appWidgetHost.startListening()
        widgetDB = WidgetDB(this)
        replicator = Replicator()
        dragAndDropData = DragAndDropData()
        dualWallpaper = DualWallpaper(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        appWidgetHost.stopListening()
    }

    fun wideShot() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays

        if(displays.size == 1) {
            displayOff()
        } else {
            if(displays[1].displayId != 1) {
                displayOff()
            } else {
                val wallpaperManager = WallpaperManager.getInstance(mainContext)
                val mainWall: BitmapDrawable = wallpaperManager.drawable as BitmapDrawable
                val wallBitmap = mainWall.bitmap
                val bitmapMain = GrayMatterUtils.getScreenshotOfRoot(mainScreen)
                val bitmapDual = GrayMatterUtils.getScreenshotOfRoot(dualScreen)

                val bitmap = Bitmap.createBitmap(
                    bitmapMain.width * 2,
                    bitmapMain.height,
                    Bitmap.Config.RGB_565
                )
                var canvas = Canvas(bitmap)
                canvas.drawBitmap(bitmapDual, 0F, 0F, null)
                canvas.drawBitmap(wallBitmap, bitmapDual.width.toFloat(), 0F, null)
                canvas.drawBitmap(bitmapMain, bitmapDual.width.toFloat(), 0F, null)

                val now = Date()
                DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)

                val contentResolver = applicationContext.contentResolver
                val contentValues = ContentValues()
                contentValues.put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    "DualityLauncher" + now + ".png"
                )
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES
                )
                val imageUri: Uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )!!
                val outputStream =
                    contentResolver.openOutputStream(Objects.requireNonNull(imageUri))
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Objects.requireNonNull(outputStream)!!.close()
                GrayMatterUtils.shortToast(applicationContext, "Wide screenshot saved to gallery")
            }
        }
    }

    fun displayOff() {
        GrayMatterUtils.longToast(this, "Dual screen not detected!")
    }
}