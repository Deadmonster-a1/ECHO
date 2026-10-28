package com.spotify.clone.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log

class EchoExpandedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} expanded widgets")
        EchoWidgetManager.updateSingleProvider(context, appWidgetManager, appWidgetIds, isCompact = false)
    }

    companion object {
        private const val TAG = "EchoExpandedWidget"
    }
}
