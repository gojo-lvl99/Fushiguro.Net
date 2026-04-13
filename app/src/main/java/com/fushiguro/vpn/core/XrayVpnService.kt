package com.fushiguro.vpn.core

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*

class XrayVpnService : VpnService() {
    
    private val binder = VpnBinder()
    private var xrayCore: XrayCore? = null
    private var vpnScope = CoroutineScope(Dispatchers.Main + Job())
    
    companion object {
        const val TAG = "XrayVpnService"
    }
    
    inner class VpnBinder : Binder() {
        fun getService(): XrayVpnService = this@XrayVpnService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        xrayCore = XrayCore(this)
    }
    
    fun startVpn(config: VpnConfig) {
        vpnScope.launch {
            try {
                xrayCore?.initialize(config)
                setupVpnInterface(config)
                xrayCore?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun stopVpn() {
        vpnScope.launch {
            xrayCore?.stop()
            stopSelf()
        }
    }
    
    private fun setupVpnInterface(config: VpnConfig) {
        val builder = Builder()
        builder.setSession("Fushiguro.Net")
        builder.addRoute("0.0.0.0", 0)
        builder.addDnsServer("8.8.8.8")
        builder.addDnsServer("8.8.4.4")
        
        val fd = builder.establish() ?: return
        xrayCore?.setVpnFd(fd.fileDescriptor)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        vpnScope.cancel()
        xrayCore?.stop()
    }
}