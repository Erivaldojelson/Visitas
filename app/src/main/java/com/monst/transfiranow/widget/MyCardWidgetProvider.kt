package com.monst.transfiranow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.widget.RemoteViews
import com.monst.transfiranow.MainActivity
import com.monst.transfiranow.R
import com.monst.transfiranow.data.CardStore
import com.monst.transfiranow.data.VisitingCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyCardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val store = CardStore(context)
                val cards = store.uiStateFlow.first().cards
                val card = cards.firstOrNull()

                updateWidgets(context, appWidgetManager, appWidgetIds, card)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, MyCardWidgetProvider::class.java))
                if (ids.isEmpty()) return@launch

                val store = CardStore(context)
                val cards = store.uiStateFlow.first().cards
                val card = cards.firstOrNull()
                updateWidgets(context, appWidgetManager, ids, card)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            card: VisitingCard?
        ) {
            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_my_card)

                if (card == null) {
                    views.setTextViewText(R.id.widget_name, context.getString(R.string.widget_empty))
                    views.setTextViewText(R.id.widget_role, "")
                } else {
                    views.setTextViewText(R.id.widget_name, card.name.ifBlank { context.getString(R.string.widget_empty) })
                    views.setTextViewText(R.id.widget_role, card.role)
                }

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPending = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openPending)
                views.setOnClickPendingIntent(R.id.widget_open, openPending)

                val walletIntent = Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_WIDGET_SAVE_TO_WALLET
                    putExtra(MainActivity.EXTRA_CARD_ID, card?.id.orEmpty())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val walletPending = PendingIntent.getActivity(
                    context,
                    appWidgetId + 10_000,
                    walletIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_wallet, walletPending)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
