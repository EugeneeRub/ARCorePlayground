package com.griddynamics.arcoremeasurements.demo2.data

import com.google.ar.sceneform.math.Vector3
import kotlin.math.pow
import kotlin.math.sqrt

object Helper {

    fun calculateDistance(firstPosition: Vector3, secondPosition: Vector3): Float {
        val x = firstPosition.x - secondPosition.x
        val y = firstPosition.y - secondPosition.y
        val z = firstPosition.z - secondPosition.z

        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }
}