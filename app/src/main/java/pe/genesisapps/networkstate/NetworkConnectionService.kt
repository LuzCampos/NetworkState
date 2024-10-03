package pe.genesisapps.networkstate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class NetworkConnectionService : Service() {

    private val CHANNEL_ID = "NetworkChannel"

    override fun onCreate() {
        super.onCreate()
        // Crear el canal de notificación
        createNotificationChannel()
        startNetworkServiceInForeground("Verificando estado de red...")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Aquí puedes empezar a observar el estado de la red
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = networkCallback { connectionState ->
            // Actualiza la notificación cuando cambia el estado de la red
            val status = when (connectionState) {
                is NetworkConnectionState.Available -> {
                    "Connected"
                }

                is NetworkConnectionState.AvailableInternet -> {
                    "Connected + Internet"
                }

                else -> {
                    "No Internet Connection"
                }
            }
            updateNotification(status)
        }

        // Registrar el callback para monitorear la red
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, callback)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No es necesario implementar el binding
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Network Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun startNetworkServiceInForeground(content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Network Service")
            .setContentText(content)
            .setSmallIcon(R.drawable.baseline_network_check)
            .build()

        startForeground(1, notification)
    }

    private fun updateNotification(content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Network Service")
            .setContentText(content)
            .setSmallIcon(R.drawable.baseline_network_check)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification) // Actualiza la notificación
    }
}