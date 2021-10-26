package com.griddynamics.arcoremeasurements.demo2.data

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.griddynamics.arcoremeasurements.R
import com.griddynamics.arcoremeasurements.demo.data.Helper
import kotlin.math.acosh

class RectangleModel {
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val midTransformableNodes: MutableMap<String, AnchorNode> = mutableMapOf()
    private val placedLineNodes = ArrayList<Node>()
    private val poolMidRenderables = ArrayList<ViewRenderable>()
    private val rectangleSize = mutableListOf(0, 0, 0, 0)

    private var whatCurrentWorkState: WorkState = WorkState.RECTANGLE

    fun initModel(context: Context) {
        repeat(4) {
            ViewRenderable
                .builder()
                .setView(context, R.layout.ruler_measure_point)
                .build()
                .thenAccept {
                    poolMidRenderables.add(it)
                }
        }
    }

    private fun placeLineNode(context: Context, startNode: AnchorNode, endNode: AnchorNode) {
        val node = Node().apply {
            setParent(startNode)
        }
        placedLineNodes.add(node)
        updateLineBetween(context, node, startNode, endNode)
    }

    private fun updateLineBetween(
        context: Context, lineNode: Node, pos1: AnchorNode, pos2: AnchorNode
    ) {
        val point1: Vector3 = pos1.worldPosition
        val point2: Vector3 = pos2.worldPosition
        val difference = Vector3.subtract(point1, point2)
        val directionFromTopToBottom = difference.normalized()
        val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())

        MaterialFactory.makeOpaqueWithColor(context, Color(0f, 255f, 244f)).thenAccept { material ->
            val model = ShapeFactory.makeCube(
                Vector3(.01f, .01f, difference.length()), Vector3.zero(), material
            )
            lineNode.renderable = model

            lineNode.worldPosition = Vector3.add(point1, point2).scaled(.5f)
            lineNode.worldRotation = rotationFromAToB
        }
    }

    private fun createRectangleMidAnchor(
        sceneFragment: ArFragment, pose: Pose, between: List<Int>
    ) {
        val midKey = "${between[0]}_${between[1]}"
        val anchor = sceneFragment.arSceneView?.session?.createAnchor(pose)

        val anchorNode = AnchorNode(anchor).apply {
            this.isSmoothed = true
            this.renderable = poolMidRenderables[between.first()]
            setParent(sceneFragment.arSceneView?.scene)
        }

        midTransformableNodes[midKey] = anchorNode
        sceneFragment.arSceneView?.scene?.addChild(anchorNode)
    }

    private fun updateRectangleMidAnchor(
        sceneFragment: ArFragment, midAnchor: AnchorNode, pose: Pose
    ) {
        val anchor = sceneFragment.arSceneView?.session?.createAnchor(pose)
        midAnchor.anchor = anchor
    }

    private fun createRectangleNode(
        sceneFragment: ArFragment, hitResult: HitResult, render: Renderable
    ) {
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
            scene.addChild(anchorNode)
        }
        transformableNode.select()
    }

    fun placeAnchor(sceneFragment: ArFragment, model: ModelRenderable, hitResult: HitResult) {
        if (whatCurrentWorkState == WorkState.RECTANGLE) {
            // working with rectangle
            if (placedAnchorNodes.size < 4) {
                createRectangleNode(sceneFragment, hitResult, model)
            } else {
                Toast.makeText(
                    sceneFragment.context, "Rectangle already exist", Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // working with lines inside rectangle
            if (placedAnchorNodes.size == 4) {
                createLine(sceneFragment, hitResult)
            } else {
                Toast.makeText(
                    sceneFragment.context, "Need to finish the rectangles", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun createLine(sceneFragment: ArFragment, hitResult: HitResult) {
        // here will be the magic
    }

    fun onUpdate(sceneFragment: ArFragment) {
        if (whatCurrentWorkState == WorkState.RECTANGLE) {
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

                val placedMidAnchor = midTransformableNodes["${x}_${x + 1}"]
                if (placedMidAnchor == null) {
                    createRectangleMidAnchor(sceneFragment, pose, listOf(x, x + 1))
                } else {
                    updateRectangleMidAnchor(sceneFragment, placedMidAnchor, pose)
                }

                // place line between anchors
                val placedLine = placedLineNodes.getOrNull(x)
                if (placedLine == null) {
                    placeLineNode(sceneFragment.requireContext(), startNode, endNode)
                } else {
                    updateLineBetween(
                        sceneFragment.requireContext(), placedLine, startNode, endNode
                    )
                }

                val textView =
                    ((midTransformableNodes["${x}_${x + 1}"]?.renderable as ViewRenderable?)?.view as TextView)
                        .findViewById<TextView>(R.id.txtDistance)

                textView.text = "$distance inch"
                rectangleSize[x] = distance
            }
        }
    }

    fun changeCurrentWorkState(currentWorkState: WorkState) {
        this.whatCurrentWorkState = currentWorkState
        if (whatCurrentWorkState == WorkState.LINE) {
            placedAnchorNodes.forEach {

            }
        }
    }

    fun clear(sceneFragment: ArFragment) {
        // clear all placed anchors on the AR view
        placedAnchorNodes.forEach { anchorNode ->
            sceneFragment.arSceneView?.scene?.removeChild(anchorNode)
            anchorNode.isEnabled = false
            anchorNode.anchor?.detach()
            anchorNode.setParent(null)
        }
        placedAnchorNodes.clear()
        midTransformableNodes.forEach { (_, value) ->
            sceneFragment.arSceneView?.scene?.removeChild(value)
        }
        midTransformableNodes.clear()
        rectangleSize.fill(0) // replace all values by 0

        placedLineNodes.forEach { node ->
            sceneFragment.arSceneView?.scene?.removeChild(node)
        }
        placedLineNodes.clear()
        poolMidRenderables.clear()
    }
}