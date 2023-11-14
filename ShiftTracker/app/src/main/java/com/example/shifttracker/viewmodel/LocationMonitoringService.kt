package com.example.shifttracker.viewmodel
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.work.WorkManager
import com.example.shifttracker.R
import com.example.shifttracker.model.TimeValue
import com.example.shifttracker.view.ProfilePage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class LocationMonitoringService : Service() /*{

    private lateinit var locationManager: LocationManager
    private lateinit var auth: FirebaseAuth


    override fun onCreate() {
        super.onCreate()
        auth = FirebaseAuth.getInstance()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        checkLocationServiceStatus()
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle("Location Monitoring")
            .setContentText("Konum servisleri izleniyor...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return notificationBuilder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "channel_id",
                "Background Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun checkLocationServiceStatus() {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            recordLocationServiceDisabled()
        }
    }
    private suspend fun getNtpTime(): Long {
        return withContext(Dispatchers.IO) {
            // Ağ işlemleri burada yapılır
            try {
                val client = NTPUDPClient()
                val address = InetAddress.getByName("pool.ntp.org")
                val info: TimeInfo = client.getTime(address)
                info.computeDetails()
                info.message.receiveTimeStamp.time
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        }
    }

    private fun recordLocationServiceDisabled() {
        CoroutineScope(Dispatchers.IO).launch {
            val time = getNtpTime()

            val date = Date(time)

            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

            val formattedDate = dateFormat.format(date)

            println("Formatlanmış tarih: $formattedDate")
            withContext(Dispatchers.Main) {

                val db = FirebaseFirestore.getInstance()
                val guncelZaman = formattedDate
                val user = auth.currentUser

                val email = user?.email ?: ""

                val userCollectionRef = db.collection(email)
                userCollectionRef.add(TimeValue(guncelZaman.toString(),"çıkış",System.currentTimeMillis()))
                    .addOnSuccessListener { documentReference ->
                        println("Firestore'a eklendi")
                    }
                    .addOnFailureListener { e ->
                        println("Firestore'a eklenirken hata oluştu: $e")
                    }

            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}*/ {
    private lateinit var auth: FirebaseAuth
    private lateinit var locationManager: LocationManager
    companion object {
        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        auth = FirebaseAuth.getInstance()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            isServiceRunning = true
            val notification = createNotification()
            startForeground(1, notification)

            // Konum servislerinin durumunu izle
            observeLocationServiceStatus()
        }

        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                ""
            }

        val notificationBuilder = Notification.Builder(applicationContext, channelId)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("Service is running in background")
            .setContentText("Konum servisleri izleniyor...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(Notification.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        return notification
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }
        return channelId
    }

    private fun observeLocationServiceStatus() {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled) {
            // Konum servisleri kapalı, Firebase Firestore'a kaydet
            recordLocationServiceDisabled()
            stopService()
        }
    }

    private fun recordLocationServiceDisabled() {
        CoroutineScope(Dispatchers.IO).launch {
            val time = getNtpTime()

            val date = Date(time)

            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

            val formattedDate = dateFormat.format(date)

            println("Formatlanmış tarih: $formattedDate")
            withContext(Dispatchers.Main) {

                val db = FirebaseFirestore.getInstance()
                val guncelZaman = formattedDate
                val user = auth.currentUser

                val email = user?.email ?: ""

                val userCollectionRef = db.collection(email)
                userCollectionRef.add(TimeValue(guncelZaman.toString(),"çıkış",System.currentTimeMillis()))
                    .addOnSuccessListener { documentReference ->
                        println("Firestore'a eklendi")
                    }
                    .addOnFailureListener { e ->
                        println("Firestore'a eklenirken hata oluştu: $e")
                    }

            }
        }
        WorkManager.getInstance(applicationContext).cancelUniqueWork("locationCheck")

    }

    fun stopService() {
        stopForeground(true)
        stopSelf()
    }
    private suspend fun getNtpTime(): Long {
        return withContext(Dispatchers.IO) {
            // Ağ işlemleri burada yapılır
            try {
                val client = NTPUDPClient()
                val address = InetAddress.getByName("pool.ntp.org")
                val info: TimeInfo = client.getTime(address)
                info.computeDetails()
                info.message.receiveTimeStamp.time
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    override fun onDestroy() {
        isServiceRunning = false
        // Servis durdurulduğunda yapılacak işlemler
        super.onDestroy()
    }
}