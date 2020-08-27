package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HomePagerAdapter(private val context: Context) :
    RecyclerView.Adapter<HomePagerAdapter.HomePagerHolder>(), Icon.IconInterface {

    var homeIconsGrid = HomeIconsGrid()
    var homeWidgetsGrid = HomeWidgetsGrid()
    var numRows: Int = 0
    var numCols: Int = 0
    private lateinit var listener: HomeIconsInterface
    val TAG = "HomePagerAdapter"

    class HomePagerHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomePagerHolder {
        val inflatedView = parent.inflate(R.layout.home_icons, false)
        return HomePagerHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: HomePagerHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder()")
        val itemView = holder.itemView
        itemView.tag = position
        val homeIconsTable = itemView.findViewById<HomeLayout>(R.id.homeIconsTable)

        itemView.setOnLongClickListener { view ->
            listener.onLongClick()
            true
        }
        homeIconsGrid = HomeIconsGrid()
        depersistGrid(position)

        val numColsString = settingsPreferences.getString("home_grid_columns", "6")
        numCols = Integer.parseInt(numColsString)
        val numRowsString = settingsPreferences.getString("home_grid_rows", "7")
        numRows = Integer.parseInt(numRowsString)
        val textColor = colorPrefToColor(settingsPreferences.getString("home_text_color", "White"))
        homeIconsTable.removeAllViews()
        homeIconsTable.setGridSize(numRows, numCols)

        for (row in 0 until numRows) {
            for (column in 0 until numCols) {
                val appWidgetId = homeWidgetsGrid.get(row, column)
                if (appWidgetId != 0) {
                    var widgetParams = HomeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    widgetParams.row = row
                    widgetParams.column = column
                    widgetParams.rowSpan = 1
                    widgetParams.columnSpan = 1
                    val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                    var widget = WidgetContainer(
                        mainContext,
                        homeWidgetsGrid.get(row, column),
                        appWidgetProviderInfo
                    )
                    homeIconsTable.addView(widget, widgetParams)
                } else {
                    var icon = Icon(context, null)
                    var iconParams = HomeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    iconParams.row = row
                    iconParams.column = column
                    iconParams.rowSpan = 1
                    iconParams.columnSpan = 1
                    icon.layoutParams = iconParams
                    icon.label.maxLines = 1
                    icon.label.setTextColor(textColor)
                    icon.setListener(this)
                    icon.setBlankOnDrag(true)
                    icon.setDockIcon(false)
                    val launchInfo = homeIconsGrid.get(row, column)
                    icon.setLaunchInfo(
                        launchInfo.getActivityName(),
                        launchInfo.getPackageName(),
                        launchInfo.getUserSerial()
                    )
                    homeIconsTable.addView(icon, iconParams)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        val homePagesString = settingsPreferences.getString("home_grid_pages", "1")
        return Integer.parseInt(homePagesString)
    }

    fun depersistGrid(position: Int) {
        Log.d(TAG, "depersistGrid()")
        var loadItJson = prefs.getString("homeIconsGrid" + position, "")
        if (loadItJson != "") {
            homeIconsGrid = loadItJson?.let { Json.decodeFromString(it) }!!
        }
        loadItJson = prefs.getString("homeWidgetsGrid" + position, "")
        if (loadItJson != "") {
            homeWidgetsGrid = loadItJson?.let { Json.decodeFromString(it) }!!
        }
    }

    override fun onIconChanged() {
        listener.onIconChanged()
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        listener.onDragStarted(view, clipData)
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        listener.onLaunch(launchInfo, displayId)
    }

    override fun onLongClick() {
        listener.onLongClick()
    }

    fun setListener(homeIconsInterface: HomeIconsInterface) {
        listener = homeIconsInterface
    }

    interface HomeIconsInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
        fun onIconChanged()
        fun onLongClick()
    }
}