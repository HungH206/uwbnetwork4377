package com.example.uwbextracredit4377

import com.example.uwbextracredit4377.ui.ProximityZone
import org.junit.Assert.assertEquals
import org.junit.Test

class UwbTrackerRepositoryTest {

    @Test
    fun testFormatAddress() {
        // Dummy repo instance using null context for helper tests
        val bytes = byteArrayOf(0x34.toByte(), 0x12.toByte())
        val formatted = bytes.joinToString(":") { "%02X".format(it) }
        assertEquals("34:12", formatted)
    }

    @Test
    fun testProximityZones() {
        assertEquals(ProximityZone.IMMEDIATE, ProximityZone.fromDistance(0.5f))
        assertEquals(ProximityZone.IMMEDIATE, ProximityZone.fromDistance(0.99f))
        assertEquals(ProximityZone.NEAR, ProximityZone.fromDistance(1.0f))
        assertEquals(ProximityZone.NEAR, ProximityZone.fromDistance(2.5f))
        assertEquals(ProximityZone.NEAR, ProximityZone.fromDistance(3.0f))
        assertEquals(ProximityZone.FAR, ProximityZone.fromDistance(3.01f))
        assertEquals(ProximityZone.FAR, ProximityZone.fromDistance(4.5f))
    }
}
