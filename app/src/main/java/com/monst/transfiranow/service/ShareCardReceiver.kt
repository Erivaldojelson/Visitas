package com.monst.transfiranow.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ShareCardReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SHARE) return

        val name = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
        val role = intent.getStringExtra(EXTRA_USER_ROLE).orEmpty()

        val shareText = buildString {
            appendLine("Meu cartão digital")
            if (name.isNotBlank()) appendLine(name)
            if (role.isNotBlank()) appendLine(role)
        }.trim()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    companion object {
        const val ACTION_SHARE = "com.monst.transfiranow.action.NOWBAR_SHARE"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_ROLE = "extra_user_role"
    }
}

