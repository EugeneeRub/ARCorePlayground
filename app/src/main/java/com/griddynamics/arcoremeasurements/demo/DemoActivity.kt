package com.griddynamics.arcoremeasurements.demo

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.griddynamics.arcoremeasurements.R
import com.griddynamics.arcoremeasurements.demo.data.InventoryType

class DemoActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var containerFoundedItems: ConstraintLayout

    private val mapOfRenderable = HashMap<InventoryType, ModelRenderable>()
    private val mapOfViewRenderable = HashMap<InventoryType, ViewRenderable>()
    private val mapOfAnchorsNode = HashMap<InventoryType, AnchorNode>()
    private val mapOfChildAnchors = HashMap<InventoryType, Node>()

    private var whatInventorySelected: InventoryType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        setupViews()
        setupListeners()
        setupArFragmentListeners()
        setupInventoryRender()
    }

    private fun setupViews() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
    }

    private fun setupArFragmentListeners() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if (whatInventorySelected == null) showBottomView(true) //need to update after
        }
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btn_sofa).setOnClickListener {
            whatInventorySelected = InventoryType.SOFA
        }
        findViewById<Button>(R.id.btn_chair).setOnClickListener {
            whatInventorySelected = InventoryType.CHAIR
        }
        findViewById<Button>(R.id.btn_lamp).setOnClickListener {
            whatInventorySelected = InventoryType.LAMP
        }
        findViewById<Button>(R.id.btn_table).setOnClickListener {
            whatInventorySelected = InventoryType.TABLE
        }
    }

    private fun showBottomView(isVisible: Boolean) {
        findViewById<ConstraintLayout>(R.id.container_founded_items).isVisible = isVisible
    }

    private fun setupInventoryRender() {
        val listOfInventory = listOf(
            InventoryType.LAMP, InventoryType.CHAIR, InventoryType.TABLE, InventoryType.SOFA,
        )

        listOfInventory.forEach {
            renderInventoryModel(it)
            renderBannerView(it)
        }
    }

    private fun renderInventoryModel(type: InventoryType) {
        val source1 = RenderableSource.builder()
            .setSource(this, Uri.parse(type.value), RenderableSource.SourceType.GLB)
            .setRecenterMode(RenderableSource.RecenterMode.ROOT)
            .build()

        ModelRenderable.builder()
            .setSource(this, source1)
            .setRegistryId(type.value)
            .build()
            .thenAccept {
                Toast.makeText(this, "Model prepared", Toast.LENGTH_SHORT).show()
                mapOfRenderable[type] = it
            }
    }

    private fun renderBannerView(type: InventoryType) {
        ViewRenderable
            .builder()
            .setView(this, R.layout.custom_banner)
            .build()
            .thenAccept {
                mapOfViewRenderable[type] = it
                mapOfViewRenderable[type]?.isShadowCaster = false
                mapOfViewRenderable[type]?.isShadowReceiver = false
            }
    }
}