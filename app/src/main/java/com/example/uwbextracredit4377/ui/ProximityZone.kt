package com.example.uwbextracredit4377.ui

import androidx.compose.ui.graphics.Color

enum class ProximityZone(
    val label: String,
    val color: Color,
    val containerColor: Color,
    val description: String
) {
    IMMEDIATE(
        label = "Immediate",
        color = Color(0xFF1B5E20),       // Forest Green
        containerColor = Color(0xFFC8E6C9),
        description = "< 1.0 m (Very Close)"
    ),
    NEAR(
        label = "Near",
        color = Color(0xFFE65100),       // Deep Orange / Amber
        containerColor = Color(0xFFFFE0B2),
        description = "1.0 m – 3.0 m (Approaching)"
    ),
    FAR(
        label = "Far",
        color = Color(0xFFB71C1C),       // Deep Red / Crimson
        containerColor = Color(0xFFFFCDD2),
        description = "> 3.0 m (Out of Range)"
    );

    companion object {
        fun fromDistance(meters: Float): ProximityZone {
            return when {
                meters < 1.0f -> IMMEDIATE
                meters <= 3.0f -> NEAR
                else -> FAR
            }
        }
    }
}
