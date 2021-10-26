package com.griddynamics.arcoremeasurements.demo2

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.core.HitResult
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.griddynamics.arcoremeasurements.R
import com.griddynamics.arcoremeasurements.demo2.data.RectangleModel
import com.griddynamics.arcoremeasurements.demo2.data.WorkState

class Demo2Activity : AppCompatActivity(), Scene.OnUpdateListener {

    private lateinit var sceneFragment: ArFragment
    private lateinit var fabWorkState: FloatingActionButton

    // base anchor model
    private var sphereRenderableModel: ModelRenderable? = null
    private var listOfRectangles: ArrayList<RectangleModel> =
        arrayListOf(RectangleModel()) // for demo lets create only 1 model

    private var currentWorkState: WorkState = WorkState.RECTANGLE
    private var whatRectangleSelected: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo2)

        setupToolbar()
        setupViews()
        setupListeners()
        setupArFragmentListeners()
        setupRenders()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        sceneFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        fabWorkState = findViewById(R.id.fab_state)

        // need to setup rectangle model and prepare some rendarables
        listOfRectangles[whatRectangleSelected].changeCurrentWorkState(currentWorkState)
        listOfRectangles[whatRectangleSelected].initModel(this)
    }

    private fun setupListeners() {
        fabWorkState.setOnClickListener {
            currentWorkState =
                if (currentWorkState == WorkState.RECTANGLE) WorkState.LINE else WorkState.RECTANGLE
            listOfRectangles.forEach { it.changeCurrentWorkState(currentWorkState) }

            fabWorkState.setImageResource(
                if (currentWorkState == WorkState.RECTANGLE) R.drawable.ic_rectangle else R.drawable.ic_line
            )
            notifyAboutCurrentState(currentWorkState)
        }
    }

    private fun setupArFragmentListeners() {
        sceneFragment.setOnTapArPlaneListener { hitResult, _, _ -> placeAnchor(hitResult) }
        sceneFragment.setOnSessionInitializationListener {
            sceneFragment.arSceneView?.scene?.addOnUpdateListener(this)
        }
    }

    private fun setupRenders() {
        // prepare base anchor model
        MaterialFactory.makeTransparentWithColor(
            this, com.google.ar.sceneform.rendering.Color(Color.RED)
        ).thenAccept { material ->
            sphereRenderableModel = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material).apply {
                isShadowCaster = false
                isShadowReceiver = false
            }
        }
    }

    private fun placeAnchor(hitResult: HitResult?) {
        if (sphereRenderableModel == null || hitResult == null) return
        listOfRectangles[whatRectangleSelected]
            .placeAnchor(sceneFragment, sphereRenderableModel!!, hitResult)
    }

    override fun onUpdate(frameTime: FrameTime?) {
        if (frameTime == null) return
        listOfRectangles[whatRectangleSelected].onUpdate(sceneFragment)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.demo2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                clearWork()
                invalidateForDemo()
            }
        }
        return true
    }

    private fun invalidateForDemo() {
        listOfRectangles.add(RectangleModel())
        whatRectangleSelected = 0

        // do smth when with UI if required "hide" or "show"
    }

    private fun clearWork() {
        listOfRectangles.forEach {
            it.clear(sceneFragment)
        }
        listOfRectangles.clear()
    }

    private fun notifyAboutCurrentState(state: WorkState) {
        Toast.makeText(
            this,
            if (state == WorkState.RECTANGLE) "Rectangle selected" else "Lines selected",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearWork()
    }
}