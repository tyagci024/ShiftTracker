package com.example.shifttracker

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.example.shifttracker.model.TimeValue
import com.example.shifttracker.viewmodel.LocationMonitoringService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class LocationServicesStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            if (!LocationMonitoringService.isServiceRunning) {
                // Servisi başlat
                val serviceIntent = Intent(context, LocationMonitoringService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}