package pe.genesisapps.networkstate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class NetworkApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val notificationChannel = NotificationChannel(
            "NetworkChannel",
            "Network Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}








