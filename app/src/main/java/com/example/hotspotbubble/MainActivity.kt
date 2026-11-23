package com.example.hotspotbubble

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        status = TextView(this).apply { textSize = 15f }
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }
        root.addView(TextView(this).apply { text = "Hotspot Bubble"; textSize = 26f })
        root.addView(status)
        root.addView(btn("1. Allow draw over other apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(btn("2. Allow usage access") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        root.addView(btn("Start bubble") {
            if (!Settings.canDrawOverlays(this)) { Toast.makeText(this, "Grant overlay permission first", Toast.LENGTH_SHORT).show() }
            else startForegroundService(Intent(this, OverlayService::class.java))
        })
        root.addView(btn("Stop bubble") { stopService(Intent(this, OverlayService::class.java)) })
        root.addView(btn("Reset total counter") {
            getSharedPreferences("p", MODE_PRIVATE).edit().putLong("since", System.currentTimeMillis()).apply()
            Toast.makeText(this, "Total reset", Toast.LENGTH_SHORT).show()
        })
        root.addView(TextView(this).apply {
            textSize = 13f; setPadding(0, pad, 0, 0)
            text = "Tap the bubble to switch between live speed and total hotspot data.\n\n" +
                "Note: Android doesn't expose per-device usage without root, so this shows the whole hotspot."
        })
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() {
        super.onResume()
        status.text = "Overlay permission: ${if (Settings.canDrawOverlays(this)) "✓" else "✗"}\n" +
            "Usage access: ${if (TrafficReader.hasUsageAccess(this)) "✓" else "✗"}"
    }
}
