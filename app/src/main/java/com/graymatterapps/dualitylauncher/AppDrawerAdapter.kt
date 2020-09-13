package com.graymatterapps.dualitylauncher

import android.content.ClipData
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.graymatterapps.graymatterutils.GrayMatterUtils.colorPrefToColor


class AppDrawerAdapter(
    private val parentActivity: MainActivity,
    private val apps: MutableList<AppList.AppListDataType>
) : RecyclerView.Adapter<AppDrawerAdapter.AppDrawerHolder>(), Icon.IconInterface {

    private lateinit var listener: DrawerAdapterInterface
    val settingsPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(parentActivity)
    var filteredList: MutableList<AppList.AppListDataType> = ArrayList()
    var filteredWork: Boolean = false
    val TAG = javaClass.simpleName

    fun filterWork(work: Boolean) {
        val currentUser = android.os.Process.myUserHandle()

        if (work) {
            filteredList =
                apps.filter { it.handle != currentUser } as MutableList<AppList.AppListDataType>
        } else {
            filteredList =
                apps.filter { it.handle.equals(currentUser) } as MutableList<AppList.AppListDataType>
        }

        filteredWork = work
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppDrawerHolder {
        val inflatedView = Icon(parentActivity, null)
        return AppDrawerHolder(inflatedView)
    }

    class AppDrawerHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${filteredList.size}")
        return filteredList.size
    }

    override fun onBindViewHolder(holder: AppDrawerHolder, position: Int) {
        val icon = holder.itemView as Icon
        icon.label.setTextColor(
            settingsPreferences.getInt(
                "app_drawer_text",
                Color.WHITE
            )
        )
        val textShadowColor = settingsPreferences.getInt("app_drawer_text_shadow", Color.BLACK)
        icon.label.setShadowLayer(6f, 0f, 0f, textShadowColor)
        icon.setLaunchInfo(
            filteredList[position].activityName,
            filteredList[position].packageName,
            filteredList[position].userSerial
        )
        icon.setListener(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        if (parentActivity is DrawerAdapterInterface) {
            listener = parentActivity
        } else {
            throw ClassCastException(parentActivity.toString() + " must implement DrawerAdapterInterface.")
        }
        super.onAttachedToRecyclerView(recyclerView)
    }

    interface DrawerAdapterInterface {
        fun onDragStarted(view: View, clipData: ClipData)
        fun onLaunch(launchInfo: LaunchInfo, displayId: Int)
    }

    override fun onIconChanged() {
        // Do nothing
    }

    override fun onDragStarted(view: View, clipData: ClipData) {
        listener.onDragStarted(view, clipData)
    }

    override fun onLaunch(launchInfo: LaunchInfo, displayId: Int) {
        listener.onLaunch(launchInfo, displayId)
    }

    override fun onLongClick(view: View) {
        // Do nothing
    }
}