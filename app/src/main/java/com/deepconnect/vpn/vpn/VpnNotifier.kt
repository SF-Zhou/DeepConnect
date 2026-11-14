package com.deepconnect.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.deepconnect.vpn.MainActivity
import com.deepconnect.vpn.R

/** Builds the ongoing VPN notification (channel, actions, live traffic counters). */
class VpnNotifier(private val service: Service) {

    private val manager = service.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            service.getString(R.string.channel_vpn_status_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = service.getString(R.string.channel_vpn_status_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(status: VpnStatus): Notification {
        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        when (status) {
            is VpnStatus.Connecting -> {
                builder.setContentTitle(service.getString(R.string.notif_connecting_title))
                builder.setContentText(status.stage)
            }
            is VpnStatus.Connected -> {
                builder.setContentTitle(status.profileName)
                builder.setContentText(connectedSummary(status))
                builder.addAction(
                    R.drawable.ic_stat_vpn,
                    service.getString(R.string.notif_disconnect),
                    disconnectIntent()
                )
            }
            is VpnStatus.Disconnecting -> {
                builder.setContentTitle("Disconnecting…")
                builder.setContentText(status.profileName)
            }
            is VpnStatus.Failed -> {
                builder.setContentTitle("VPN error")
                builder.setContentText(status.message)
            }
            VpnStatus.Disconnected -> {
                builder.setContentTitle(service.getString(R.string.app_name))
                builder.setContentText("Disconnected")
            }
        }
        return builder.build()
    }

    fun update(status: VpnStatus) {
        runCatching { manager.notify(NOTIFICATION_ID, build(status)) }
    }

    private fun connectedSummary(status: VpnStatus.Connected): String {
        val traffic = "${formatBytes(status.stats.rxBytes)} ↓  ${formatBytes(status.stats.txBytes)} ↑"
        val address = status.ipv4 ?: status.ipv6 ?: status.server
        return "$address • $traffic"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024.0
        var index = 0
        while (value >= 1024 && index < units.size - 1) {
            value /= 1024.0
            index++
        }
        return String.format("%.1f %s", value, units[index])
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(service, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            service, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun disconnectIntent(): PendingIntent {
        val intent = Intent(service, DeepConnectVpnService::class.java).apply {
            action = DeepConnectVpnService.ACTION_DISCONNECT
        }
        return PendingIntent.getService(
            service, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "deepconnect.vpn.status"
        const val NOTIFICATION_ID = 1001
    }
}
