package com.example.hotspotbubble

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: TextView
    private lateinit var lp: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var showLive = true
    private var lastBytes = 0L
    private var lastTime = 0L
    private var speed = 0.0
    private var fallbackTotal = 0L

    private val tick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val bytes = TrafficReader.mobileBytes()
            if (lastTime != 0L && bytes >= lastBytes) {
                val d = bytes - lastBytes
                speed = d * 1000.0 / (now - lastTime)
                fallbackTotal += d
            }
            lastBytes = bytes; lastTime = now
            render()
            handler.postDelayed(this, 1000)
        }
    }

    private fun render() {
        if (showLive) {
            bubble.text = "↓↑\n" + TrafficReader.fmtSpeed(speed)
        } else {
            val prefs = getSharedPreferences("p", MODE_PRIVATE)
            val since = prefs.getLong("since", System.currentTimeMillis())
            val t = TrafficReader.tetherBytes(this, since)
            bubble.text = "Total\n" + TrafficReader.fmtBytes(if (t >= 0) t else fallbackTotal)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        if (!prefs.contains("since")) prefs.edit().putLong("since", System.currentTimeMillis()).apply()
        startForeground(1, notification())
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = (92 * resources.displayMetrics.density).toInt()
        bubble = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 12f
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xE6246BFD.toInt()) }
        }
        lp = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 300 }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt(); val dy = (e.rawY - sy).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    lp.x = ox + dx; lp.y = oy + dy; wm.updateViewLayout(bubble, lp)
                }
                MotionEvent.ACTION_UP -> if (!moved) { showLive = !showLive; render() }
            }
            true
        }
        wm.addView(bubble, lp)
        handler.post(tick)
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("hb", "Hotspot Bubble", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, "hb")
            .setContentTitle("Hotspot Bubble running")
            .setSmallIcon(android.R.drawable.stat_notify_sync).build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if (::bubble.isInitialized) wm.removeView(bubble)
        super.onDestroy()
    }
    override fun onBind(i: Intent?): IBinder? = null
}
