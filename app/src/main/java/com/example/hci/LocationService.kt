package com.example.hci

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var geocoder: Geocoder
    private var destinationLatLng: LatLng? = null

    companion object {
        const val CHANNEL_ID = "LocationServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        geocoder = Geocoder(this, Locale.getDefault())
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        destinationLatLng = Data.DestinationLocationLatng

        if (destinationLatLng == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("주먹구구 대중교통 앱")
            .setContentText("위치 기반 알림이 설정 되어 있습니다")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        startLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            val distance = calculateDistance(currentLatLng, destinationLatLng!!)
            checkShouldAlertAlarm(distance)
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun calculateDistance(latLng1: LatLng, latLng2: LatLng): Float {
        val location1 = Location("").apply {
            latitude = latLng1.latitude
            longitude = latLng1.longitude
        }
        val location2 = Location("").apply {
            latitude = latLng2.latitude
            longitude = latLng2.longitude
        }
        return location1.distanceTo(location2)
    }

    private fun checkShouldAlertAlarm(distance: Float) {
        // 여기에 알람 체크 로직 추가
        var BeforeAlarmDistance = Data.ClosetestAlarmDistance

        if(distance <= Data.AccepRadius)
        {
            showAlarm(true, distance)
            stopSelf()
            return
        }

        if(Data.AlarmUnitDistance == null) {
            return
        }

        var _AlarmUnitDistance = Data.AlarmUnitDistance!!

        val loopI = (distance - Data.AccepRadius) / _AlarmUnitDistance
        val i = Math.floor(loopI.toDouble()).toInt()
        Data.ClosetestAlarmDistance = Data.AccepRadius + i * _AlarmUnitDistance

        if(BeforeAlarmDistance != null && BeforeAlarmDistance != Data.ClosetestAlarmDistance)
        {
            showAlarm(false, BeforeAlarmDistance)
        }
    }

    private fun showAlarm(bisFinal: Boolean, leftDistance: Float) {

        // 이 부분 구현해 주시면 됩니다. 우선은 기본적인 알람만 보이도록 했습니다

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm")
            .setContentText("Distance left: $leftDistance meters. Final: $bisFinal")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }
}
