package com.griddynamics.arcoremeasurements.demo.data

enum class InventoryType(val title: String, val value: String) {
    SOFA("Sofa", "file:///android_asset/sofa.glb"),
    TABLE("Table", "file:///android_asset/table.glb"),
    CHAIR("Chair", "file:///android_asset/chair.glb"),
    LAMP("Lamp", "file:///android_asset/lamp.glb");

    companion object {
        fun getFromString(title: String) = when (title) {
            "Sofa" -> SOFA
            "Table" -> TABLE
            "Chair" -> CHAIR
            "Lamp" -> LAMP
            else -> SOFA
        }
    }
}