package com.erubezhin.arcoremeasurements

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlin.math.pow
import kotlin.math.sqrt

class RulerActivity : AppCompatActivity(), Scene.OnUpdateListener {

    private var sceneFragment: ArFragment? = null
    private var sphereRenderable: ModelRenderable? = null
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val placedLineNodes = ArrayList<Node>()
    private val midTransformableNodes: MutableMap<String, TransformableNode> = mutableMapOf()
    private val poolMidRenderables = ArrayList<ViewRenderable>()
    private lateinit var textSquare: TextView
    private val rectangleSize = mutableListOf(0, 0, 0, 0)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ruler)

        initToolbar()

        textSquare = findViewById(R.id.txtSquare)
        sceneFragment = supportFragmentManager.findFragmentById(R.id.scene_fragment) as ArFragment

        initRenderables()
        sceneFragment?.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (sphereRenderable == null) return@setOnTapArPlaneListener
            if (placedAnchorNodes.size < 4) {
                placeAnchor(hitResult, sphereRenderable!!)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.ruler, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> clear()
        }
        return true
    }

    override fun onDestroy() {
        clear()
        super.onDestroy()
    }

    override fun onUpdate(frameTime: FrameTime) {
        if (placedAnchorNodes.size < 2) {
            return
        }

        var startNode: AnchorNode?
        var endNode: AnchorNode?

        for (x in 0 until placedAnchorNodes.size) {
            startNode = placedAnchorNodes[x]
            endNode =
                if (x == 3) placedAnchorNodes.getOrNull(0) else placedAnchorNodes.getOrNull(x + 1)

            if (endNode == null) {
                continue
            }

            val distance = (calculateDistance(
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
                placeMidAnchor(pose, listOf(x, x + 1))
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
                val square = rectangleSize[0] * rectangleSize[1]
                if (square != 0) {
                    textSquare.text = "Square $square"
                }
            }
        }
    }

    private fun initToolbar() {
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

    private fun initRenderables() {
        MaterialFactory.makeTransparentWithColor(this, Color(android.graphics.Color.RED))
            .thenAccept { material ->
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
    }

    private fun placeAnchor(hitResult: HitResult, render: Renderable) {
        val anchor = hitResult.createAnchor()

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(sceneFragment?.arSceneView?.scene)
        }
        placedAnchorNodes.add(anchorNode)

        val transformableNode = TransformableNode(sceneFragment?.transformationSystem).apply {
            rotationController.isEnabled = false
            scaleController.isEnabled = false
            translationController.isEnabled = true
            renderable = render
            setParent(anchorNode)
        }

        sceneFragment?.arSceneView?.scene?.also { scene ->
            scene.addOnUpdateListener(this)
            scene.addChild(anchorNode)
        }
        transformableNode.select()
    }

    private fun placeMidAnchor(pose: Pose, between: List<Int>) {
        val midKey = "${between[0]}_${between[1]}"
        val anchor = sceneFragment?.arSceneView?.session?.createAnchor(pose)

        val anchorNode = AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(sceneFragment?.arSceneView?.scene)
        }

        val transformableNode = TransformableNode(sceneFragment?.transformationSystem)
            .apply {
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.renderable = poolMidRenderables[between.first()]
                setParent(anchorNode)
            }
        midTransformableNodes[midKey] = transformableNode
        sceneFragment?.arSceneView?.scene?.addOnUpdateListener(this)
        sceneFragment?.arSceneView?.scene?.addChild(anchorNode)
        transformableNode.select()
    }

    private fun calculateDistance(firstPosition: Vector3, secondPosition: Vector3): Float {
        val x = firstPosition.x - secondPosition.x
        val y = firstPosition.y - secondPosition.y
        val z = firstPosition.z - secondPosition.z

        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
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

    private fun clear() {
        placedAnchorNodes.forEach { anchorNode ->
            sceneFragment?.arSceneView?.scene?.removeChild(anchorNode)
            anchorNode.run {
                isEnabled = false
                anchor?.detach()
                anchorNode.setParent(null)
            }
        }
        placedAnchorNodes.clear()
        val children: List<Node> = ArrayList(sceneFragment?.arSceneView?.scene?.children!!)
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

        //clear placed lines
        placedLineNodes.clear()
    }
}