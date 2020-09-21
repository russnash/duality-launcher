package com.graymatterapps.dualitylauncher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ClipData
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import com.graymatterapps.graymatterutils.GrayMatterUtils

class WidgetContainer(
    val parentActivity: MainActivity,
    var appWidgetId: Int,
    private var appWidgetProviderInfo: AppWidgetProviderInfo
) : FrameLayout(parentActivity, null), ResizeFrame.ResizeInterface {

    lateinit var parentLayout: HomeLayout
    var neededWidth: Int = 0
    var neededHeight: Int = 0
    private lateinit var listener: WidgetInterface
    private lateinit var interceptGestureDetector: GestureDetectorCompat
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var resizeFrame: ResizeFrame
    private var eventHistory = ArrayList<MotionEvent>()
    private val touchSlop: Int = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private var resizing: Boolean = false
    val TAG = javaClass.simpleName

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return if (interceptGestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        parentLayout = this.parent as HomeLayout

        interceptGestureDetector = GestureDetectorCompat(parentActivity, object :
            GestureDetector.OnGestureListener {
            override fun onDown(p0: MotionEvent?): Boolean {
                Log.d(TAG, "iOnDown()")
                return true
            }

            override fun onShowPress(p0: MotionEvent?) {
                Log.d(TAG, "iOnShowPress()")
                // Do nothing
            }

            override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                Log.d(TAG, "iOnSingleTapUp()")
                return true
            }

            override fun onScroll(
                p0: MotionEvent?,
                p1: MotionEvent?,
                p2: Float,
                p3: Float
            ): Boolean {
                Log.d(TAG, "iOnScroll()")
                return true
            }

            override fun onLongPress(p0: MotionEvent?) {
                Log.d(TAG, "iOnLongPress()")
                // Do nothing
            }

            override fun onFling(
                p0: MotionEvent?,
                p1: MotionEvent?,
                p2: Float,
                p3: Float
            ): Boolean {
                Log.d(TAG, "iOnFling()")
                return true
            }
        })

        gestureDetector =
            GestureDetectorCompat(parentActivity, object : GestureDetector.OnGestureListener {
                override fun onDown(event: MotionEvent?): Boolean {
                    Log.d(TAG, "OnDown()")
                    if (event != null) {
                        eventHistory.add(MotionEvent.obtain(event))
                    }
                    return true
                }

                override fun onShowPress(event: MotionEvent?) {
                    Log.d(TAG, "onShowPress()")
                    if (event != null) {
                        eventHistory.add(MotionEvent.obtain(event))
                    }
                }

                override fun onSingleTapUp(event: MotionEvent?): Boolean {
                    Log.d(TAG, "onSingleTapUp()")
                    if (event != null) {
                        eventHistory.add(MotionEvent.obtain(event))
                        if (resizeFrame.isResizing()) {
                            eventHistory.forEach {
                                resizeFrame.dispatchTouchEvent(it)
                            }
                            eventHistory.clear()
                        } else {
                            val hostView =
                                widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView
                            eventHistory.forEach {
                                hostView.dispatchTouchEvent(it)
                            }
                            eventHistory.clear()
                        }
                    } else {
                        Log.d(TAG, "onSingleTapUp() event = null!")
                    }
                    return false
                }

                override fun onScroll(
                    eventOld: MotionEvent?,
                    event: MotionEvent?,
                    p2: Float,
                    p3: Float
                ): Boolean {
                    Log.d(TAG, "onScroll()")
                    if (event != null) {
                        eventHistory.add(MotionEvent.obtain(event))
                        if (resizeFrame.isResizing()) {
                            eventHistory.forEach {
                                resizeFrame.dispatchTouchEvent(it)
                            }
                            eventHistory.clear()
                            if (eventOld != null) {
                                val distance = GrayMatterUtils.getDistance(
                                    event.x,
                                    event.y,
                                    eventOld.x,
                                    eventOld.y
                                )
                                Log.d(TAG, "onScroll() distance: $distance")
                                if (distance > touchSlop
                                ) {
                                    resizeFrame.setResize(false)
                                    eventHistory.clear()
                                    startDrag()
                                }
                            }
                        } else {
                            val hostView =
                                widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView
                            eventHistory.forEach {
                                hostView.dispatchTouchEvent(it)
                            }
                            eventHistory.clear()
                        }
                    }
                    return false
                }

                override fun onLongPress(p0: MotionEvent?) {
                    Log.d(TAG, "onLongPress()")
                    eventHistory.clear()
                    GrayMatterUtils.vibrate(parentActivity, 50)
                    resizeFrame.bringToFront()
                    resizeFrame.setResize(true)
                    resizing = true
                }

                override fun onFling(
                    eventOld: MotionEvent?,
                    event: MotionEvent?,
                    p2: Float,
                    p3: Float
                ): Boolean {
                    Log.d(TAG, "onFling()")
                    if (event != null) {
                        eventHistory.add(MotionEvent.obtain(event))
                        if (resizeFrame.isResizing()) {
                            eventHistory.forEach {
                                resizeFrame.dispatchTouchEvent(it)
                            }
                            eventHistory.clear()
                        } else {
                            val hostView =
                                widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView
                            eventHistory.forEach {
                                hostView.dispatchTouchEvent(it)
                            }
                            eventHistory.clear()
                        }
                    }
                    return false
                }
            })
    }

    fun resetResize() {
        resizeFrame.setResize(false)
        resizing = false
        eventHistory.clear()
    }

    fun setListener(ear: WidgetInterface) {
        listener = ear
    }

    private fun startDrag() {
        val id = System.currentTimeMillis().toString()
        var widgetInfo = WidgetInfo(appWidgetId, appWidgetProviderInfo, null)
        dragAndDropData.addWidget(widgetInfo, id)
        val clipData = ClipData.newPlainText("widget", id)
        val dsb = WidgetDragShadowBuilder(this)
        if (!this.startDragAndDrop(clipData, dsb, this, 0)) {
            widgetInfo = dragAndDropData.retrieveWidgetId(id)
            appWidgetHost.deleteAppWidgetId(widgetInfo.getAppWidgetId())
        }
        this.convertToIcon()
    }

    fun addWidgetView() {
        Log.d(TAG, "addWidgetView()")
        appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        listener.updateWidgets(appWidgetProviderInfo)
        val widgetIndex = widgetDB.getWidgetIndex(appWidgetId)
        widgetDB.widgets[widgetIndex].appWidgetHostView.setPadding(0, 0, 0, 0)
        val minWidth =
            appWidgetProviderInfo.minWidth + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingLeft + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingRight
        val minHeight =
            appWidgetProviderInfo.minHeight + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingTop + widgetDB.widgets[widgetIndex].appWidgetHostView.paddingBottom

        val containerParams = this.layoutParams as HomeLayout.LayoutParams
        val sizes = widgetDB.getWidgetSize(appWidgetId)
        if (sizes.getInt("columnSpan") != 0) {
            neededWidth = sizes.getInt("columnSpan") * parentLayout.getCellWidth()
            neededHeight = sizes.getInt("rowSpan") * parentLayout.getCellHeight()
            containerParams.columnSpan = sizes.getInt("columnSpan")
            containerParams.rowSpan = sizes.getInt("rowSpan")
        } else {
            neededWidth = parentLayout.widthToCells(minWidth) * parentLayout.getCellWidth()
            neededHeight = parentLayout.heightToCells(minHeight) * parentLayout.getCellHeight()
            containerParams.columnSpan = parentLayout.widthToCells(neededWidth)
            containerParams.rowSpan = parentLayout.heightToCells(neededHeight)
        }
        containerParams.width = neededWidth
        containerParams.height = neededHeight
        this.layoutParams = containerParams

        val viewParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        viewParams.width = neededWidth
        viewParams.height = neededHeight
        viewParams.gravity = Gravity.CENTER
        widgetDB.widgets[widgetIndex].appWidgetHostView.layoutParams = viewParams
        this.removeAllViews()
        try {
            val oldContainer =
                widgetDB.widgets[widgetIndex].appWidgetHostView.parent as WidgetContainer
            oldContainer.pleaseRemove(widgetDB.widgets[widgetIndex].appWidgetHostView)
        } catch (e: Exception) {
            Log.d(TAG, "Attempt at pleaseRemove() failed!")
        }
        this.addView(
            widgetDB.widgets[widgetIndex].appWidgetHostView,
            viewParams
        )
        val widthSpec = MeasureSpec.makeMeasureSpec(neededWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(neededHeight, MeasureSpec.EXACTLY)
        widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView.measure(
            widthSpec,
            heightSpec
        )
        widgetDB.widgets[widgetIndex].appWidgetHostView.updateAppWidgetSize(
            null,
            neededWidth,
            neededHeight,
            neededWidth,
            neededHeight
        )
        resizeFrame = ResizeFrame(parentActivity, null)
        resizeFrame.setListener(this)
        resizeFrame.layoutParams = viewParams
        this.addView(resizeFrame, viewParams)
        resizeFrame.measure(widthSpec, heightSpec)
        if (resizing) {
            resizeFrame.setResize(true)
            resizeFrame.bringToFront()
        }
        listener.onWidgetChanged()
        this.bringToFront()
        widgetDB.updateWidgetSize(appWidgetId, containerParams.rowSpan, containerParams.columnSpan)
    }

    fun pleaseRemove(widget: AppWidgetHostView) {
        this.removeView(widget)
    }

    private fun convertToIcon() {
        val icon = Icon(parentActivity, null)
        val launchInfo = LaunchInfo()
        icon.setLaunchInfo(launchInfo)
        icon.setBlankOnDrag(true)
        val params = this.layoutParams as HomeLayout.LayoutParams
        params.columnSpan = 1
        params.rowSpan = 1
        icon.layoutParams = params
        parentLayout.addView(icon, params)
        this.removeAllViews()
        parentLayout.removeView(this)
        listener.onWidgetChanged()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(TAG, "onLayout()")
        if (this.childCount == 0) {
            val i = widgetDB.allocateWidget(appWidgetId, appWidgetProviderInfo, this)
            if(widgetDB.widgets[i].initialized) {
                addWidgetView()
            }
        }
        val widthSpec = MeasureSpec.makeMeasureSpec(neededWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(neededHeight, MeasureSpec.EXACTLY)
        widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView.measure(
            widthSpec,
            heightSpec
        )
        try {
            resizeFrame.measure(widthSpec, heightSpec)
        } catch (e: Exception) {
            // Do nothing
        }
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        parentLayout = this.parent as HomeLayout
        val params = this.layoutParams as HomeLayout.LayoutParams
        params.rowSpan = parentLayout.heightToCells(neededHeight)
        params.columnSpan = parentLayout.widthToCells(neededWidth)
        this.layoutParams = params
        setMeasuredDimension(neededWidth, neededHeight)
    }

    interface WidgetInterface {
        fun onWidgetChanged()
        fun updateWidgets(widgetInfo: AppWidgetProviderInfo)
    }

    private fun adjustLayout() {
        val widgetIndex = widgetDB.getWidgetIndex(appWidgetId)
        val containerParams = this.layoutParams as HomeLayout.LayoutParams
        neededWidth = containerParams.columnSpan * parentLayout.getCellWidth()
        neededHeight = containerParams.rowSpan * parentLayout.getCellHeight()
        containerParams.width = neededWidth
        containerParams.height = neededHeight
        this.layoutParams = containerParams

        val viewParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        viewParams.width = neededWidth
        viewParams.height = neededHeight
        viewParams.gravity = Gravity.CENTER
        widgetDB.widgets[widgetIndex].appWidgetHostView.layoutParams = viewParams
        val widthSpec = MeasureSpec.makeMeasureSpec(neededWidth, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(neededHeight, MeasureSpec.EXACTLY)
        widgetDB.widgets[widgetDB.getWidgetIndex(appWidgetId)].appWidgetHostView.measure(
            widthSpec,
            heightSpec
        )
        widgetDB.widgets[widgetIndex].appWidgetHostView.updateAppWidgetSize(
            null,
            neededWidth,
            neededHeight,
            neededWidth,
            neededHeight
        )
        widgetDB.updateWidgetSize(appWidgetId, containerParams.rowSpan, containerParams.columnSpan)
        resizeFrame.layoutParams = viewParams
        resizeFrame.measure(widthSpec, heightSpec)
        listener.onWidgetChanged()
    }

    override fun onTopPlus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.row > 0) {
            params.rowSpan++
            params.row--
        }
        this.layoutParams = params
        adjustLayout()
    }

    override fun onBottomPlus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.row + params.rowSpan < parentLayout.getRows()) {
            params.rowSpan++
            this.layoutParams = params
            adjustLayout()
        }
    }

    override fun onLeftPlus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.column > 0) {
            params.columnSpan++
            params.column--
            this.layoutParams = params
            adjustLayout()
        }
    }

    override fun onRightPlus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.column + params.columnSpan < parentLayout.getColumns()) {
            params.columnSpan++
            this.layoutParams = params
            adjustLayout()
        }
    }

    override fun onTopMinus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.rowSpan > 1) {
            params.rowSpan--
            params.row++
            adjustLayout()
        }
    }

    override fun onBottomMinus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.rowSpan > 1) {
            params.rowSpan--
            adjustLayout()
        }
    }

    override fun onLeftMinus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.columnSpan > 1) {
            params.columnSpan--
            params.column++
            adjustLayout()
        }
    }

    override fun onRightMinus() {
        val params = this.layoutParams as HomeLayout.LayoutParams
        if (params.columnSpan > 1) {
            params.columnSpan--
            adjustLayout()
        }
    }
}