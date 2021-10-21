package com.griddynamics.arcoremeasurements

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnRuler).setOnClickListener {
            val intent = Intent(this, RulerActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnPlacement).setOnClickListener {
            val intent = Intent(this, PlacementActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnDemo).setOnClickListener {
            val intent = Intent(this, DemoActivity::class.java)
            startActivity(intent)
        }
    }
}