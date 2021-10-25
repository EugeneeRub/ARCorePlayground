package com.griddynamics.arcoremeasurements.demo

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.*
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.griddynamics.arcoremeasurements.R
import com.griddynamics.arcoremeasurements.demo.data.Helper
import com.griddynamics.arcoremeasurements.demo.data.InventoryType

class DemoActivity : AppCompatActivity(), Scene.OnUpdateListener {

    private lateinit var sceneFragment: ArFragment
    private lateinit var textSquare: TextView
    private lateinit var foundedItems: ConstraintLayout
    private lateinit var groupInventory: RadioGroup

    // placement data
    private val mapOfRenderable = HashMap<InventoryType, ModelRenderable>()
    private val mapOfViewRenderable = HashMap<InventoryType, ViewRenderable>()
    private val mapOfAnchorsNode = HashMap<InventoryType, AnchorNode>()
    private val mapOfChildAnchors = HashMap<InventoryType, Node>()

    // ruler data
    private var sphereRenderable: ModelRenderable? = null
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val midTransformableNodes: MutableMap<String, TransformableNode> = mutableMapOf()
    private val placedLineNodes = ArrayList<Node>()
    private val poolMidRenderables = ArrayList<ViewRenderable>()
    private val rectangleSize = mutableListOf(0, 0, 0, 0)

    private val listOfInventory = listOf(
        InventoryType.LAMP, InventoryType.CHAIR, InventoryType.TABLE, InventoryType.SOFA,
    )

    private var whatInventorySelected: InventoryType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

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
        textSquare = findViewById(R.id.tv_square)
        foundedItems = findViewById(R.id.container_founded_items)
        groupInventory = findViewById(R.id.rg_inventory)
    }

    private fun setupArFragmentListeners() {
        sceneFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if (sphereRenderable == null) return@setOnTapArPlaneListener
            placeAnchor(hitResult)
        }
    }

    private fun setupListeners() {
        groupInventory.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.btn_sofa -> whatInventorySelected = InventoryType.SOFA
                R.id.btn_lamp -> whatInventorySelected = InventoryType.LAMP
                R.id.btn_chair -> whatInventorySelected = InventoryType.CHAIR
                R.id.btn_table -> whatInventorySelected = InventoryType.TABLE
            }
        }
    }

    private fun bottomViewVisibility(isVisible: Boolean) {
        foundedItems.isVisible = isVisible
    }

    private fun setupRenders() {
        // prepare ruler renders
        MaterialFactory.makeTransparentWithColor(
            this, Color(Color.RED)
        ).thenAccept { material ->
            sphereRenderable = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material).apply {
                isShadowCaster = false
                isShadowReceiver = false
            }
        }
        repeat(16) {
            ViewRenderable
                .builder()
                .setView(this, R.layout.ruler_measure_point)
                .build()
                .thenAccept {
                    poolMidRenderables.add(it)
                }
        }

        // prepare placement renders
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
            .thenAccept { mapOfRenderable[type] = it }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.demo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_placement -> clearPlacement()
            R.id.clear -> {
                clearRuler()
                clearPlacement()
            }
        }
        return true
    }

    override fun onDestroy() {
        clearRuler()
        clearPlacement()
        super.onDestroy()
    }

    override fun onUpdate(frameTime: FrameTime) {
        if (placedAnchorNodes.size < 2) return

        var startNode: AnchorNode?
        var endNode: AnchorNode?

        for (x in 0 until placedAnchorNodes.size) {
            startNode = placedAnchorNodes[x]
            endNode =
                if (x == 3) placedAnchorNodes.getOrNull(0) else placedAnchorNodes.getOrNull(x + 1)

            if (endNode == null) {
                continue
            }

            val distance = (Helper.calculateDistance(
                startNode.worldPosition,
                endNode.worldPosition
            ) * 100 * 0.39).toInt()

            if (distance == 0) {
                continue
            }

            val midPosition = floatArrayOf(
                (startNode.worldPosition.x + endNode.worldPosition.x) / 2,
                (startNode.worldPosition.y + endNode.worldPosition.y) / 2,
                (startNode.worldPosition.z + endNode.worldPosition.z) / 2
            )
            val quaternion = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
            val pose = Pose(midPosition, quaternion)

            if (midTransformableNodes["${x}_${x + 1}"] == null) {
                createRulerMidAnchor(pose, listOf(x, x + 1))
            }

            // place line between anchors
            val placedLine = placedLineNodes.getOrNull(x)
            if (placedLine == null) {
                placeLineNode(startNode, endNode)
            } else {
                updateLineBetween(placedLine, startNode, endNode)
            }

            val textView =
                ((midTransformableNodes["${x}_${x + 1}"]?.renderable as ViewRenderable?)?.view as TextView)
                    .findViewById<TextView>(R.id.txtDistance)

            textView.text = "$distance inch"
            rectangleSize[x] = distance

            if (rectangleSize[3] != 0) {
                showSquareAndWhatFoundPlacement()
                val square = rectangleSize[0] * rectangleSize[1]
                if (square != 0) {
                    textSquare.text = "Square $square"
                }
            }
        }
    }

    private fun showSquareAndWhatFoundPlacement() {
        textSquare.isVisible = true
        bottomViewVisibility(true)
    }

    private fun placeAnchor(hitResult: HitResult) {
        if (placedAnchorNodes.size < 4) {
            createRulerNode(hitResult, sphereRenderable!!)
        } else {
            if (whatInventorySelected == null) return
            if (mapOfAnchorsNode[whatInventorySelected] == null)
                createPlacementNode(hitResult, whatInventorySelected!!)
            else
                Toast.makeText(this, "Object already exist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPlacementNode(hitResult: HitResult, whatInventorySelected: InventoryType) {
        val anchor = hitResult.createAnchor()
        val model = mapOfRenderable[whatInventorySelected]!!

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(sceneFragment.arSceneView.scene)
        }
        mapOfAnchorsNode[whatInventorySelected] = anchorNode

        val node = TransformableNode(sceneFragment.transformationSystem);
        node.localScale = Vector3(0.3f, 0.3f, 0.3f) // set default scale

        node.renderable = model
        node.rotationController.isEnabled = true
        node.scaleController.isEnabled = true
        node.translationController.isEnabled = true

        node.scaleController.minScale = 0.1f
        node.scaleController.maxScale = 1f

        node.setOnTapListener { nodeTapped, event ->
            if (nodeTapped.node == null) return@setOnTapListener
            if (event.action == MotionEvent.ACTION_UP)
                createBannerPlacementChildNode(nodeTapped.node!!, whatInventorySelected)
        }
        node.setParent(anchorNode)

        sceneFragment.arSceneView.scene.addChild(anchorNode)
        node.select()
    }

    private fun createRulerNode(hitResult: HitResult, render: Renderable) {
        val anchorNode = AnchorNode(hitResult.createAnchor()).apply {
            isSmoothed = true
            setParent(sceneFragment.arSceneView?.scene)
        }
        placedAnchorNodes.add(anchorNode)

        val transformableNode = TransformableNode(sceneFragment.transformationSystem).apply {
            rotationController.isEnabled = false
            scaleController.isEnabled = false
            translationController.isEnabled = true
            renderable = render
            setParent(anchorNode)
        }

        sceneFragment.arSceneView?.scene?.also { scene ->
            scene.addOnUpdateListener(this)
            scene.addChild(anchorNode)
        }
        transformableNode.select()
    }

    private fun createRulerMidAnchor(pose: Pose, between: List<Int>) {
        val midKey = "${between[0]}_${between[1]}"
        val anchor = sceneFragment.arSceneView?.session?.createAnchor(pose)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(sceneFragment.arSceneView?.scene)
        }

        val transformableNode = TransformableNode(sceneFragment.transformationSystem)
            .apply {
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.renderable = poolMidRenderables[between.first()]
                setParent(anchorNode)
            }
        midTransformableNodes[midKey] = transformableNode
        sceneFragment.arSceneView?.scene?.addOnUpdateListener(this)
        sceneFragment.arSceneView?.scene?.addChild(anchorNode)
    }

    private fun createBannerPlacementChildNode(parentNode: Node, model: InventoryType) {
        if (mapOfChildAnchors[model] == null) {
            val node = Node()
            node.setParent(parentNode)
            node.worldPosition = Vector3(
                parentNode.worldPosition.x,
                parentNode.worldPosition.y + 0.3f, // move to head of our ar object
                parentNode.worldPosition.z
            )
            mapOfViewRenderable[model]?.view?.findViewById<TextView>(R.id.textChildTitle)
                ?.apply {
                    text = model.title
                }
            node.setOnTapListener { _, _ -> showBannerDialog(model) }
            node.renderable = mapOfViewRenderable[model]
            mapOfChildAnchors[model] = node
        } else {
            val node = mapOfChildAnchors.remove(model)
            sceneFragment.arSceneView.scene.removeChild(node)
            parentNode.removeChild(node)
        }
    }

    private fun showBannerDialog(model: InventoryType) {
        val alertDialog: AlertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Product info")
        alertDialog.setMessage("${model.title}\nPrice 100$")
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Close") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun clearRuler() {
        placedAnchorNodes.forEach { anchorNode ->
            sceneFragment.arSceneView?.scene?.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor?.detach()
            anchorNode.setParent(null)
        }
        placedAnchorNodes.clear()
        val children: List<Node> = ArrayList(sceneFragment.arSceneView?.scene?.children!!)
        for (node in children) {
            if (node is AnchorNode) {
                if (node.anchor != null) {
                    node.anchor?.detach()
                }
            }
            if (node !is Camera && node !is Sun) {
                node.setParent(null)
            }
        }
        midTransformableNodes.clear()
        for (x in 0 until rectangleSize.size) {
            rectangleSize[x] = 0
        }
        textSquare.isVisible = false
    }

    private fun placeLineNode(startNode: AnchorNode, endNode: AnchorNode) {
        val node = Node().apply {
            setParent(startNode)
        }
        placedLineNodes.add(node)
        updateLineBetween(node, startNode, endNode)
    }

    private fun updateLineBetween(lineNode: Node, pos1: AnchorNode, pos2: AnchorNode) {
        val point1: Vector3 = pos1.worldPosition
        val point2: Vector3 = pos2.worldPosition
        val difference = Vector3.subtract(point1, point2)
        val directionFromTopToBottom = difference.normalized()
        val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())

        MaterialFactory.makeOpaqueWithColor(
            applicationContext, Color(0f, 255f, 244f)
        ).thenAccept { material ->
            val model = ShapeFactory.makeCube(
                Vector3(.01f, .01f, difference.length()), Vector3.zero(), material
            )
            lineNode.renderable = model

            lineNode.worldPosition = Vector3.add(point1, point2).scaled(.5f)
            lineNode.worldRotation = rotationFromAToB
        }
    }


    private fun clearPlacement() {
        mapOfAnchorsNode.forEach { result ->
            val anchorNode = result.value
            sceneFragment.arSceneView.scene.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor!!.detach()
            anchorNode.setParent(null)
        }

        mapOfAnchorsNode.clear()
        mapOfChildAnchors.clear()
        placedLineNodes.clear()

        bottomViewVisibility(false)
    }
}