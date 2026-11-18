package com.example.nutrition.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UnitConverterTest {

    @Test
    fun `formatForInput uses US dot decimal separator regardless of system locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRANCE)
            assertEquals("12.5", UnitConverter.formatForInput(12.5))
            assertEquals("300", UnitConverter.kiloJoulesToCalories("1255.2"))
            assertEquals("1255.2", UnitConverter.caloriesToKiloJoules("300"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `formatForInput rounds before integer check and strips negative zero`() {
        assertEquals("300", UnitConverter.formatForInput(300.00000000000006))
        assertEquals("10", UnitConverter.formatForInput(9.96))
        assertEquals("0", UnitConverter.formatForInput(-0.04))
        assertEquals("-0.5", UnitConverter.formatForInput(-0.5))
    }

    @Test
    fun `formatForInput returns empty string for non-finite numbers`() {
        assertEquals("", UnitConverter.formatForInput(Double.NaN))
        assertEquals("", UnitConverter.formatForInput(Double.POSITIVE_INFINITY))
        assertEquals("", UnitConverter.formatForInput(Double.NEGATIVE_INFINITY))
        assertEquals("", UnitConverter.caloriesToKiloJoules("NaN"))
        assertEquals("", UnitConverter.kiloJoulesToCalories("Infinity"))
    }
}
