package com.meelano.builder.util

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper

/** Finds MeeLano Builder servers on the local Wi-Fi (mDNS/Zeroconf). */
object NsdHelper {
    const val TYPE = "_meelano-builder._tcp."

    /**
     * Discovers servers for [timeoutMs]. [onFound] is called (on a binder
     * thread — marshal to main!) with the growing URL list.
     */
    fun discover(
        ctx: Context,
        timeoutMs: Long = 9000,
        onFound: (List<String>) -> Unit,
    ) {
        val nsd = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
        val found = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(si: NsdServiceInfo, ec: Int) {}
            override fun onServiceResolved(si: NsdServiceInfo) {
                val ip = si.host?.hostAddress ?: return
                val url = "http://$ip:${si.port}"
                if (seen.add(url)) {
                    found += url
                    onFound(found.toList())
                }
            }
        }
        val disc = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String) {}
            override fun onStartDiscoveryFailed(t: String, ec: Int) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onStopDiscoveryFailed(t: String, ec: Int) {}
            override fun onServiceFound(si: NsdServiceInfo) {
                try {
                    nsd.resolveService(si, resolveListener)
                } catch (_: Exception) { /* ignore */ }
            }
            override fun onServiceLost(si: NsdServiceInfo) {}
        }
        nsd.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, disc)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                nsd.stopServiceDiscovery(disc)
            } catch (_: Exception) { /* ignore */ }
        }, timeoutMs)
    }
}
