package com.autoarchery.app.vision

import org.junit.Assert.*
import org.junit.Test

class FrameGeometryTest {
    private val analyzer = TargetVisionAnalyzer()
    @Test fun rotatesCoordinatesIntoSensorSpace() {
        assertEquals(.25f to .75f, analyzer.sensorCoordinates(.25f,.75f,0))
        assertEquals(.75f to .75f, analyzer.sensorCoordinates(.25f,.75f,90))
        assertEquals(.75f to .25f, analyzer.sensorCoordinates(.25f,.75f,180))
        assertEquals(.25f to .25f, analyzer.sensorCoordinates(.25f,.75f,270))
    }
    @Test fun rejectsLargeSceneMovementButAllowsUniformExposure() {
        val baseline = FloatArray(96*96) { 150f }
        assertTrue(analyzer.sceneIsStable(baseline, FloatArray(96*96) { 120f }))
        val moved = baseline.copyOf()
        for (i in 0 until 1000) moved[i] = 40f
        assertFalse(analyzer.sceneIsStable(baseline, moved))
    }
}
