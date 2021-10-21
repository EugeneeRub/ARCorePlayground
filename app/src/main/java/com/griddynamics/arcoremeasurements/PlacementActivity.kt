package com.griddynamics.arcoremeasurements

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class PlacementActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var btnClear: AppCompatButton
    private lateinit var spinner: Spinner

    private val mapOfRenderable = HashMap<Type, ModelRenderable>()
    private val mapOfViewRenderable = HashMap<Type, ViewRenderable>()
    private val mapOfAnchorsNode = HashMap<Type, AnchorNode>()
    private val mapOfChildAnchors = HashMap<Type, Node>()

    private var whatSelected = Type.SOFA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placement)

        initViews()
        initSpinner()
        initRender()
        initListeners()
        initArFragmentListeners()
    }

    private fun initSpinner() {
        val distanceModeAdapter = ArrayAdapter(
            applicationContext,
            android.R.layout.simple_spinner_item,
            arrayListOf(Type.SOFA.title, Type.TABLE.title, Type.CHAIR.title)
        )
        distanceModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = distanceModeAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val spinnerParent = parent as Spinner
                whatSelected = Type.getFromString(spinnerParent.selectedItem as String)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                clearAnchorsOnScreen()
            }
        }
    }

    private fun initListeners() {
        btnClear.setOnClickListener { clearAnchorsOnScreen() }
    }

    private fun initViews() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        btnClear = findViewById(R.id.btn_clear)
        spinner = findViewById(R.id.spinner)
    }

    private fun initRender() {
        renderObject(Type.SOFA)
        renderObject(Type.TABLE)
        renderObject(Type.CHAIR)

        renderView(Type.SOFA)
        renderView(Type.TABLE)
        renderView(Type.CHAIR)
    }

    private fun renderView(type: Type) {
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

    private fun initArFragmentListeners() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if (mapOfAnchorsNode[whatSelected] == null)
                createMainNode(hitResult)
            else
                Toast.makeText(this, "Object already exist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAnchorsOnScreen() {
        mapOfAnchorsNode.forEach { result ->
            val anchorNode = result.value
            arFragment.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor!!.detach()
            anchorNode.setParent(null)
        }

        mapOfAnchorsNode.clear()
        mapOfChildAnchors.clear()
    }

    private fun createMainNode(hitResult: HitResult) {
        val anchor = hitResult.createAnchor()
        val model = getModel()

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment.arSceneView.scene)
        }
        mapOfAnchorsNode[whatSelected] = anchorNode

        val node = TransformableNode(arFragment.transformationSystem);
        node.localScale = Vector3(0.3f, 0.3f, 0.3f) // set default scale

        node.renderable = model
        node.rotationController.isEnabled = true
        node.scaleController.isEnabled = true
        node.translationController.isEnabled = true

        node.scaleController.minScale = 0.1f
        node.scaleController.maxScale = 1f

        node.setOnTapListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP)
                addTextChildNode(node, whatSelected)
        }

        node.setParent(anchorNode)

        arFragment.arSceneView.scene.addChild(anchorNode)
        node.select()
    }

    private fun addTextChildNode(parentNode: TransformableNode, model: Type) {
        if (mapOfChildAnchors[whatSelected] == null) {
            val node = Node()
            node.setParent(parentNode)
            node.worldPosition = Vector3(
                parentNode.worldPosition.x,
                parentNode.worldPosition.y + 0.3f, // move to head of our ar object
                parentNode.worldPosition.z
            )
            mapOfViewRenderable[whatSelected]?.view?.findViewById<TextView>(R.id.textChildTitle)
                ?.apply {
                    text = model.title
                }
            node.setOnTapListener { _, _ ->
                showDialogWithNode(model)
            }
            node.renderable = mapOfViewRenderable[whatSelected]
            mapOfChildAnchors[whatSelected] = node
        } else {
            val node = mapOfChildAnchors.remove(whatSelected)
            parentNode.removeChild(node)
        }
    }

    private fun showDialogWithNode(model: Type) {
        val alertDialog: AlertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(model.title)
        alertDialog.setMessage("You click on AR nameplate, cool")
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Close") { dialog, which ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun getModel(): Renderable = mapOfRenderable[whatSelected]!!

    private fun renderObject(type: Type) {
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

    enum class Type(val title: String, val value: String) {
        SOFA("Sofa", "file:///android_asset/sofa.glb"),
        TABLE("Table", "file:///android_asset/table.glb"),
        CHAIR("Chair", "file:///android_asset/chair.glb");

        companion object {
            fun getFromString(title: String) = when (title) {
                "Sofa" -> SOFA
                "Table" -> TABLE
                "Chair" -> CHAIR
                else -> SOFA
            }
        }
    }
}