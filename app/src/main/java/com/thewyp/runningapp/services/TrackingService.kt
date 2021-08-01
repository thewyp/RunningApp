package com.thewyp.runningapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.amap.api.location.*
import com.amap.api.maps2d.model.LatLng
import com.thewyp.runningapp.R
import com.thewyp.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.thewyp.runningapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.thewyp.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.thewyp.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.thewyp.runningapp.other.Constants.NOTIFICATION_CHANNEL_ID
import com.thewyp.runningapp.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.thewyp.runningapp.other.Constants.NOTIFICATION_ID
import com.thewyp.runningapp.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    var isFirstRun = true

    @Inject
    lateinit var converter: CoordinateConverter

    @Inject
    lateinit var mapLocationClient: AMapLocationClient

    companion object {
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
    }

    override fun onCreate() {
        super.onCreate()
        postInitialValues()

        isTracking.observe(this, {
            updateLocationTracking(it)
        })

        mapLocationClient.setLocationListener(mAMapLocationListener)
    }

    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            mapLocationClient.startLocation()
        } else {
            mapLocationClient.stopLocation()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        startForegroundService()
                        Timber.d("Resuming service...")
                    }
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped service")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun pauseService() {
        isTracking.postValue(false)
    }

    private val mAMapLocationListener = AMapLocationListener { amapLocation: AMapLocation? ->
        amapLocation?.let {
            when (amapLocation.errorCode) {
                0 -> {
                    Timber.d(amapLocation.toString())
                    if (isTracking.value!!) {
                        converter.from(CoordinateConverter.CoordType.GPS)
                        val dPoint = DPoint(amapLocation.latitude, amapLocation.longitude).also {
                            converter.coord(it)
                            converter.convert()
                        }
                        val pos = LatLng(dPoint.latitude, dPoint.longitude)
                        pathPoints.value!!.apply {
                            last().add(pos)
                            pathPoints.postValue(this)
                        }
                    }
                }
                else -> {
                    Timber.e("AMapError,location Error, ErrCode:${amapLocation.errorCode}, errInfo:${amapLocation.errorInfo}")
                }
            }
        }
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService() {
        addEmptyPolyline()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("Running App")
            .setContentText("00:00:00")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        FLAG_UPDATE_CURRENT
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}
