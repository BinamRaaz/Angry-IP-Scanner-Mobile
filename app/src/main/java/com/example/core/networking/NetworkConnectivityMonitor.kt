package com.example.core.networking

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitors live device connectivity events using Android's [ConnectivityManager.NetworkCallback].
 * Emits real-time network reachability states and handles disconnections.
 */
class NetworkConnectivityMonitor(
    private val context: Context,
    private val networkInfoProvider: NetworkInfoProvider = AndroidNetworkInfoProvider(context)
) {
    val connectivityState: Flow<LocalNetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            trySend(networkInfoProvider.getCurrentNetworkState())
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(networkInfoProvider.getCurrentNetworkState())
            }

            override fun onLost(network: Network) {
                trySend(LocalNetworkState(isConnected = false))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(networkInfoProvider.getCurrentNetworkState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial network state
        trySend(networkInfoProvider.getCurrentNetworkState())

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }.distinctUntilChanged()
}
