package com.thewyp.runningapp.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.CoordinateConverter
import com.thewyp.runningapp.R
import com.thewyp.runningapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.thewyp.runningapp.other.Constants.FASTEST_LOCATION_INTERVAL
import com.thewyp.runningapp.other.Constants.LOCATION_TIME_OUT
import com.thewyp.runningapp.other.Constants.NOTIFICATION_CHANNEL_ID
import com.thewyp.runningapp.ui.MainActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun provideCoordinateConverter(
        @ApplicationContext app: Context
    ) = CoordinateConverter(app)

    @ServiceScoped
    @Provides
    fun provideAMapLocationClientOption() =
        AMapLocationClientOption().apply {
            locationPurpose = AMapLocationClientOption.AMapLocationPurpose.Sport
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            interval = FASTEST_LOCATION_INTERVAL
            isMockEnable = true
            httpTimeOut = LOCATION_TIME_OUT
        }

    @ServiceScoped
    @Provides
    fun provideAMapLocationClient(
        @ApplicationContext app: Context,
        option: AMapLocationClientOption
    ) = AMapLocationClient(app).apply {
        setLocationOption(option)
    }


    @ServiceScoped
    @Provides
    fun provideMainActivityPendingIntent(
        @ApplicationContext app: Context
    ) = PendingIntent.getActivity(
        app,
        0,
        Intent(app, MainActivity::class.java).also {
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
        },
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context,
        pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
        .setContentTitle("Running App")
        .setContentText("00:00:00")
        .setContentIntent(pendingIntent)
}