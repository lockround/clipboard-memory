package com.clipboardmemory.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.clipboardmemory.ClipboardApp
import com.clipboardmemory.MainActivity
import com.clipboardmemory.R
import com.clipboardmemory.data.ClipboardEntry
import com.clipboardmemory.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ClipboardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var lastSeenHash = ""

    override fun onCreate() {
        super.onCreate()
        Preferences.setServiceRunning(this, true)
        createNotificationChannel()
        startForegroundCompat()
        startMonitoring()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        monitorJob = scope.launch {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val initialContent = try {
                clipboard.primaryClip?.getItemAt(0)?.coerceToText(this@ClipboardService)
                    ?.toString()
            } catch (_: Exception) {
                null
            }
            lastSeenHash = initialHash(initialContent)

            while (isActive) {
                val clip = clipboard.primaryClip
                val text = try {
                    clip?.getItemAt(0)?.coerceToText(this@ClipboardService)?.toString()
                } catch (_: Exception) {
                    null
                }?.trim()?.takeIf { it.isNotEmpty() }

                val currentHash = text?.hashCode()?.toString().orEmpty()

                if (text != null && currentHash.isNotEmpty() && currentHash != lastSeenHash) {
                    lastSeenHash = currentHash
                    saveEntry(text, clip?.description?.label?.toString())
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun initialHash(initialContent: String?): String =
        initialContent?.trim()?.takeIf { it.isNotEmpty() }?.hashCode()?.toString().orEmpty()

    private suspend fun saveEntry(text: String, appLabel: String?) {
        val app = appLabel?.takeIf { it.isNotBlank() } ?: "Other app"
        val db = (application as ClipboardApp).database
        db.clipboardDao().insert(
            ClipboardEntry(
                content = text,
                timestamp = System.currentTimeMillis(),
                appPackage = app
            )
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clipboard)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob?.isActive != true) startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        Preferences.setServiceRunning(this, false)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "clipboard_monitor"
        private const val NOTIFICATION_ID = 42
        private const val POLL_INTERVAL_MS = 500L

        fun start(context: Context) {
            Preferences.setServiceEnabled(context, true)
            ContextCompat.startForegroundService(
                context,
                Intent(context, ClipboardService::class.java)
            )
        }
    }
}