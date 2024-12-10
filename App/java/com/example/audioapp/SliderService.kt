package com.example.audioapp

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.os.HandlerCompat
import android.car.Car
import java.util.concurrent.Executors
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager

class SliderService : Service() {

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private val rotaryPropertyId = 591397138
    @Volatile private var isMonitoring = true

    private val carServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

           
            Log.d("SliderService", "Car service connected")
            try {
            car = Car.createCar(applicationContext)
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager       
            if (carPropertyManager != null) {
                    initializeCarProperties()
                } else {
                    Log.e("SliderService", "CarPropertyManager is null")
                }
            } catch (e: Exception) {
                Log.e("SliderService", "Error getting CarPropertyManager", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("SliderService", "Car service disconnected")
            carPropertyManager = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SliderService", "Service started")

        try {
            car = Car.createCar(applicationContext)
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager       
            if (carPropertyManager != null) {
                    initializeCarProperties()
                } else {
                    Log.e("SliderService", "CarPropertyManager is null")
                }
            // car = Car.createCar(this, carServiceConnection)
        } catch (e: Exception) {
            Log.e("SliderService", "Error initializing Car API", e)
        }

        return START_STICKY
    }

    private fun monitorProperty(propertyId: Int, action: (value: Int?) -> Unit) {
        try {
            val carPropertyValue: CarPropertyValue<*>? = carPropertyManager?.getProperty(
                Integer::class.java, propertyId, 0
            )
            val value = carPropertyValue?.value as? Int
            Log.d("CAR", "Property $propertyId value: $value")
            action(value)
        } catch (e: Exception) {
            Log.e("CAR", "Error monitoring property $propertyId", e)
        }
    }

    private fun initializeCarProperties() {
        executorService.execute {
            while (isMonitoring) {
                monitorProperty(rotaryPropertyId) { value ->
                    if (value == 1) {
                        HandlerCompat.createAsync(mainLooper).post {
                            closeApps()
                        }
                    }
                }

               // Use postDelayed instead of Thread.sleep
                HandlerCompat.createAsync(mainLooper).postDelayed({
                    initializeCarProperties()
                }, 100)
            }
        }
    }

    override fun onDestroy() {
        Log.d("SliderService", "Service destroyed")
        isMonitoring = false
        executorService.shutdownNow()
        car?.disconnect()
        unbindService(carServiceConnection)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun closeApps() {
        try {
            val intent = packageManager?.getLaunchIntentForPackage("com.example.audioapp")
            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(it)
            }
        } catch (e: Exception) {
            Log.e("SliderService", "Error launching app", e)
        }
    }
}
