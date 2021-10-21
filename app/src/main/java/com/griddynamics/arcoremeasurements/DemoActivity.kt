package com.griddynamics.arcoremeasurements

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.ux.ArFragment

class DemoActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        initViews()
    }

    private fun initViews() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
    }
}