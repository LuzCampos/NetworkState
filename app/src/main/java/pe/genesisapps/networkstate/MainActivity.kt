package pe.genesisapps.networkstate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pe.genesisapps.networkstate.ui.theme.NetworkStateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkStateTheme {
                NetworkScreen()
            }
        }
        requestPermission()
        startService()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ isGranted : Boolean ->
        if(isGranted){
            Toast.makeText(this,"Permission allowed", Toast.LENGTH_LONG).show()
        }
        else{
            Toast.makeText(this,"Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                startService()
                // Permission is granted
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) -> {
                // Additional rationale should be triggered
                // For now simply ask for Notification Permission for testing purpose
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> {
                // Permission has not been asked yet
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startService(){
        val serviceIntent = Intent(this, NetworkConnectionService::class.java)
        startService(serviceIntent)
    }
}



@Composable
fun NetworkScreen() {
    val connectionState by rememberConnectivityState()

    val isConnected by remember(connectionState) {
        derivedStateOf {
            connectionState === NetworkConnectionState.Available
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (isConnected) "Connected" else "Unavailable",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

//Esta interfaz define dos posibles estados de la red: Available y Unavailable
sealed interface NetworkConnectionState {
    data object Available : NetworkConnectionState
    data object Unavailable : NetworkConnectionState
    data object AvailableInternet : NetworkConnectionState
}


fun networkCallback(callback: (NetworkConnectionState) -> Unit): ConnectivityManager.NetworkCallback =
    object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            callback(NetworkConnectionState.Available)
        }

        override fun onLost(network: Network) {
            callback(NetworkConnectionState.Unavailable)
        }
    }

fun getCurrentConnectivityState(connectivityManager: ConnectivityManager): NetworkConnectionState {
    val network = connectivityManager.activeNetwork

    val isConnected = connectivityManager
        .getNetworkCapabilities(network)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false

    val isConnectedAndHaveInternet = connectivityManager
        .getNetworkCapabilities(network)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false

    return if (isConnected) {
        NetworkConnectionState.Available
    } else if (isConnectedAndHaveInternet) {
        NetworkConnectionState.AvailableInternet
    } else {
        NetworkConnectionState.Unavailable
    }

    //return if (isConnected) NetworkConnectionState.Available else NetworkConnectionState.Unavailable
    /*
    val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

    val hasInternetCapability = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    val isValidated = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false

    // Verifica si tiene capacidad de Internet y si la conexión está validada
    return if (hasInternetCapability && isValidated) {
        NetworkConnectionState.Available
    } else {
        NetworkConnectionState.Unavailable
    }
    * */
}

fun Context.observeConnectivityAsFlow(): Flow<NetworkConnectionState> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = networkCallback { connectionState ->
        trySend(connectionState)
    }

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(networkRequest, callback)

    val currentState = getCurrentConnectivityState(connectivityManager)
    trySend(currentState)

    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}

val Context.currentConnectivityState: NetworkConnectionState
    get() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return getCurrentConnectivityState(connectivityManager)
    }

@Composable
fun rememberConnectivityState(): State<NetworkConnectionState> {
    val context = LocalContext.current

    return produceState(initialValue = context.currentConnectivityState) {
        context.observeConnectivityAsFlow().collect {
            value = it
        }
    }
}

