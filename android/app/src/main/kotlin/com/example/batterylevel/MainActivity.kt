package com.example.batterylevel


import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.BatteryManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel


import com.example.tms.IMyAidlInterface

//import com.trac.serverappnew.IBackendService


class MainActivity : FlutterActivity() {
    private val CHANNEL = "samples.flutter.dev/battery"


    //    private var backendService: IBackendService? = null
    private var myBackendService: IMyAidlInterface? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            backendService = IBackendService.Stub.asInterface(service)
            myBackendService = IMyAidlInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
//            backendService = null
            myBackendService = null
        }
    }


    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)



        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            // This method is invoked on the main thread.
                call, result ->




            if (call.method == "getBatteryLevel") {
                val batteryLevel = getBatteryLevel()

                if (batteryLevel != -1) {
                    result.success(batteryLevel)
                } else {
                    result.error("UNAVAILABLE", "Battery level not available.", null)
                }
            } else if (call.method == "getBackendUrl") {

                val backendUrl = getBackendUrl()
                println("Message: $backendUrl")
                if (backendUrl != null) {
                    result.success(backendUrl)
                } else {
                    result.error("UNAVAILABLE", "Backend URL not available. $backendUrl", null)
                }
            } else {
                result.notImplemented()
            }
        }


    }


    private fun getBatteryLevel(): Int {
        val batteryLevel: Int
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            batteryLevel =
                intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(
                    BatteryManager.EXTRA_SCALE,
                    -1
                )
        }

        return batteryLevel
    }

    private fun getBackendUrl(): String? {
        return try {
            bindToBackendService()
//            backendService?.getBackendUrl()
            myBackendService?.getSavedUrl()
        } catch (e: RemoteException) {
            System.err.println(e.message)
            e.message
        }
    }

    // Bind to the backend service
    @TargetApi(VERSION_CODES.DONUT)
    private fun bindToBackendService() {
//        val intent = Intent("com.trac.serverappnew.IBackendService")
//                intent.setPackage("com.trac.serverappnew")
        val intent = Intent("com.example.tms.IMyAidlInterface")
        intent.setPackage("com.example.tms")

        try {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            Log.e("MainActivity-New", "SecurityException: ${e.message}")
            println("MainActivity-New => SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e("MainActivity-New", "Exception: ${e.message}")
            println("MainActivity-New => Exception: ${e.message}")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}