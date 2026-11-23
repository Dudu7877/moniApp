package com.example.hotspotbubble

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Process

object TrafficReader {
    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    /** Bytes the system attributes to tethering (hotspot) on mobile data since [since]. -1 if unavailable. */
    fun tetherBytes(ctx: Context, since: Long): Long {
        return try {
            val nsm = ctx.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val stats = nsm.queryDetailsForUid(
                NetworkCapabilities.TRANSPORT_CELLULAR, null, since, System.currentTimeMillis(),
                NetworkStats.Bucket.UID_TETHERING
            )
            val b = NetworkStats.Bucket()
            var sum = 0L
            while (stats.hasNextBucket()) { stats.getNextBucket(b); sum += b.rxBytes + b.txBytes }
            stats.close()
            sum
        } catch (e: Exception) { -1L }
    }

    /** Total bytes through the mobile connection since boot (used for live speed). */
    fun mobileBytes(): Long {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()
        return if (rx < 0 || tx < 0) 0 else rx + tx
    }

    fun fmtBytes(b: Long): String = when {
        b >= 1L shl 30 -> "%.2f GB".format(b / 1073741824.0)
        b >= 1L shl 20 -> "%.1f MB".format(b / 1048576.0)
        else -> "%.0f KB".format(b / 1024.0)
    }

    fun fmtSpeed(bps: Double): String = when {
        bps >= 1048576 -> "%.1f MB/s".format(bps / 1048576)
        else -> "%.0f KB/s".format(bps / 1024)
    }
}
