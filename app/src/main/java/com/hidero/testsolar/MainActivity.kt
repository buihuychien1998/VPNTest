package com.hidero.testsolar

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.work.*
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.hidero.testsolar.models.Data
import com.hidero.testsolar.speedtest.test.PingTest
import com.hidero.testsolar.utils.*
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.HashSet


class MainActivity : AppCompatActivity() {
    private val REQUEST_ACCESS_WIFI_STATE: Int = 123
    private val REQUEST_ACCESS_LOCATION: Int = 456
    private var check = false
    private var data: Data? = null
    private var binding: com.hidero.testsolar.databinding.ActivityMainBinding? = null
    var broadcastReceiver: WifiReceiver? = null
    var connectedTime = 0L
    var h = Handler()
    val dec = DecimalFormat("#.##")
    var r = object : Runnable {
        override fun run() {
            // update TextView here!
            if (SharePreferenceUtils.getLongData(this@MainActivity, Const.CONNECT_TIME) != -1L) {
                val oldTime = SharePreferenceUtils.getLongData(this@MainActivity, Const.CONNECT_TIME)
                val currentTime = System.currentTimeMillis()
                val totalTime = currentTime - oldTime
                val Hours = (totalTime / (1000 * 60 * 60))
                val Mins = (totalTime / (1000 * 60)) % 60
                val Secs = ((totalTime / 1000) % 60)
                data!!.connectTime.set("$Hours:$Mins:$Secs")
                Log.d("SpeedTestActivity", data!!.connectTime.get())

            } else {
                val currentTime = System.currentTimeMillis()
                val totalTime = currentTime - connectedTime
                val Hours = (totalTime / (1000 * 60 * 60)) as Long
                val Mins = (totalTime / (1000 * 60)) as Long % 60
                val Secs = ((totalTime / 1000) as Long % 60).toLong()
                data!!.connectTime.set("$Hours:$Mins:$Secs")
                SharePreferenceUtils.insertLongData(this@MainActivity, Const.CONNECT_TIME, connectedTime)

            }
            h.postDelayed(this, 1000); //ms

        }
    }
    private val handler = Handler()
    private var manager: ConnectivityManager? = null
    private val endCall = Runnable {
        // if execution has reached here - feel free to cancel the call
        // because no connection was established in a second
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            super.onAvailable(network)
            Log.i("vvv", "connected to " + if (manager!!.isActiveNetworkMetered) "LTE" else "WIFI")
            // we've got a connection, remove callbacks (if we have posted any)
            handler.removeCallbacks(endCall);
        }

        override fun onLost(network: android.net.Network) {
            super.onLost(network)
            Log.i("vvv", "losing active connection")
            // Schedule an event to take place in a second
            handler.postDelayed(endCall, 1000);
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        dialogPermission()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding!!.handlers = this
        grantPermission()
        data = Data()
        binding!!.mData = data


        manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager!!.registerDefaultNetworkCallback(networkCallback)
        }

        data!!.deviceName.set(Build.MODEL)
        data!!.versionAndroid.set(Build.VERSION.RELEASE)

        broadcastReceiver = WifiReceiver()
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        intentFilter.addAction("service.to.activity.transfer");
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(broadcastReceiver, intentFilter)
        var updateUIReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                //UI update here
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    connectedTime = intent.getLongExtra("connectedTime", 0)
                }
            }
        }
        registerReceiver(updateUIReceiver, intentFilter)

        checkConnectedWifi()

        val workManager = WorkManager.getInstance()

        // Add constraint to start the worker when connecting to WiFi
        val request = OneTimeWorkRequest.Builder(WifiConnectWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .build()

        // The worker should be started, even if your app is closed
        workManager.beginUniqueWork("watch_wifi", ExistingWorkPolicy.REPLACE, request).enqueue()
    }


    //Handler event click
    fun myClick(v: View) {
        if (v.id == R.id.btnProtect) {
            Toast.makeText(this, "Click", Toast.LENGTH_SHORT).show()
            checkConnectedWifi()
            if (isConnectedViaWifi(this)) {
                handler.postDelayed(r, 1000)
            }
        }
    }

    //event
    fun checkConnectedWifi() {
        if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.TYPE_WIFI && Utils.isNetworkConnected(this)) {
            handler.postDelayed(r, 1000)
            data!!.wifiName.set(tryToReadSSID())
            checkWifiState()
            getIpApi()
            data!!.macAddress.set(Utils.getMACAddress(null))

            // Synchronously
            Thread(Runnable {
                var tempBlackList: HashSet<String> = HashSet()
                var getSpeedTestHostsHandler = GetSpeedTestHostsHandler()
                getSpeedTestHostsHandler.start()
                //Get egcodes.speedtest hosts
                var timeCount = 600 //1min
                while (!getSpeedTestHostsHandler.isFinished) {
                    timeCount--
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                    }

                    if (timeCount <= 0) {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "No Connection...", Toast.LENGTH_LONG).show()
                        }

                    }
                }

                //Find closest server
                val mapKey = getSpeedTestHostsHandler.mapKey
                val mapValue = getSpeedTestHostsHandler.mapValue
                val selfLat = getSpeedTestHostsHandler.selfLat
                val selfLon = getSpeedTestHostsHandler.selfLon
                var tmp = 19349458.0
                var findServerIndex = 0
                for (index in mapKey.keys) {
                    if (tempBlackList.contains(mapValue[index]!![5])) {
                        continue
                    }

                    val source = Location("Source")
                    source.latitude = selfLat
                    source.longitude = selfLon

                    val ls = mapValue[index]
                    val dest = Location("Dest")
                    dest.latitude = java.lang.Double.parseDouble(ls!![0])
                    dest.longitude = java.lang.Double.parseDouble(ls[1])

                    val distance = source.distanceTo(dest).toDouble()
                    if (tmp > distance) {
                        tmp = distance
                        findServerIndex = index
                    }
                }
                val info = mapValue[findServerIndex]
                val pingRateList = ArrayList<Double>()
                var pingTestStarted = false
                var pingTestFinished = false
                val pingTest = PingTest(info!![6].replace(":8080", ""), 6)
                //Tests
                while (true) {
                    if (!pingTestStarted) {
                        pingTest.start()
                        pingTestStarted = true
                    }
                    //Ping Test
                    if (pingTestFinished) {
                        //Failure
                        if (pingTest.avgRtt == 0.0) {
                            println("Ping error...")
                        } else {
                            //Success
                            runOnUiThread { data!!.ping.set(dec.format(pingTest.avgRtt) + " ms") }
                        }
                    } else {
                        pingRateList.add(pingTest.instantRtt)

                        runOnUiThread { data!!.ping.set(dec.format(pingTest.instantRtt) + " ms") }

                        //Update chart
                        runOnUiThread {
                            // Creating an  XYSeries for Income
                            val pingSeries = XYSeries("")
                            pingSeries.title = ""

                            var count = 0
                            val tmpLs = ArrayList(pingRateList)
                            for (`val` in tmpLs) {
                                pingSeries.add(count++.toDouble(), `val`!!)
                            }

                            val dataset = XYMultipleSeriesDataset()
                            dataset.addSeries(pingSeries)
                        }
                    }

                    if (pingTest.isFinished) {
                        pingTestFinished = true
                        break
                    }
                    if (pingTestStarted && !pingTestFinished) {
                        try {
                            Thread.sleep(300)
                        } catch (e: InterruptedException) {
                        }

                    } else {
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                        }

                    }
                }
            }).start()
            //get Country local
            var country = Locale.getDefault().country

            if (country != null) {
                data!!.country.set(country)
            } else {
                data!!.country.set("---")
            }
        } else if (NetworkUtil.getConnectivityStatus(this) == NetworkUtil.TYPE_MOBILE) {


        } else {
            SharePreferenceUtils.removeStringData(this@MainActivity, Const.CONNECT_TIME)
            h.removeCallbacks(r)

            data!!.country.set("---")
            data!!.wifiName.set(resources.getString(R.string.wifi_off))
            data!!.wifiStatus.set(R.drawable.ic_wireless_protected_net)
            data!!.ipAddress.set(null)
            data!!.macAddress.set(null)
            data!!.ping.set(null)
            data!!.connectTime.set(null)
        }

    }

    private fun isConnectedViaWifi(context: Context): Boolean {
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return mWifi.isConnected
    }



    private fun grantPermission() {

        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
            } else {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_ACCESS_LOCATION
                )
            }

        } catch (xx: Exception) {
            Toast.makeText(this, xx.message, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_WIFI_STATE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //TODO
                check = true
            }

            REQUEST_ACCESS_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //User allowed the location and you can read it now
                tryToReadSSID();
            }
        }

    }

    private fun tryToReadSSID(): String? {
        var ssid: String?
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        //If requested permission isn't Granted yet
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Request permission from user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ACCESS_LOCATION
            )
        } else {//Permission already granted
            if (wifiInfo.supplicantState == SupplicantState.COMPLETED) {

            }


        }
        ssid = Utils.findSSIDForWifiInfo(wifiManager, wifiInfo)

        ssid = ssid.substring(1, ssid.length - 1)

        return ssid
    }

    inner class WifiConnectWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

        override fun doWork(): Result {
            val activity: MainActivity = this@MainActivity
            activity.checkConnectedWifi()
            Log.i("WifiConnectWorker", "I think we connected to a wifi")
            return Result.success()
        }
    }


    fun checkWifiState() {
        var connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.detailedState == NetworkInfo.DetailedState.AUTHENTICATING && mWifi.isConnected) {
            // Do whatever
            data!!.wifiStatus.set(R.drawable.ic_wireless_non_protected)
        } else {
            data!!.wifiStatus.set(R.drawable.ic_wireless_protected)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        manager!!.unregisterNetworkCallback(networkCallback)
        handler.removeCallbacks(endCall)
    }

    private fun getIpApi() {
        var queue = Volley.newRequestQueue(this)
        var urlip = "http://checkip.amazonaws.com/"
        var stringRequest =
            StringRequest(Request.Method.GET, urlip, com.android.volley.Response.Listener<String>() { response ->
                data!!.ipAddress.set(response)

            }, com.android.volley.Response.ErrorListener() {
                Log.e("WifiReceiver", it.toString())
            })
        queue.add(stringRequest)
    }
}
