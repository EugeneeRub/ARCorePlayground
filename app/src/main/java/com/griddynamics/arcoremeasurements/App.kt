package com.griddynamics.arcoremeasurements

import android.app.Application
import com.google.android.filament.Filament

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Filament.init()
    }
}