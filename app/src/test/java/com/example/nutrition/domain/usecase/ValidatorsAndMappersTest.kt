package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.constants.PresetFoodTemplates
import com.example.nutrition.domain.model.ActivityLevel
import com.example.nutrition.domain.model.BodyProfile
import com.example.nutrition.domain.model.Gender
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.model.MealRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsAndMappersTest {

    @Test
    fun `MealFormValidator rejects NaN Infinity and invalid weight`() {
        assertNotNull(MealFormValidator.validate("NaN", "", "", ""))
        assertNotNull(MealFormValidator.validate("Infinity", "", "", ""))
        assertNotNull(MealFormValidator.validate("200", "NaN", "", ""))
        assertNotNull(MealFormValidator.validate("200", "", "Infinity", ""))
        assertNotNull(MealFormValidator.validate("200", "", "", "NaN"))
        assertNotNull(MealFormValidator.validate("200", "", "", "", weight = "0"))
        assertNotNull(MealFormValidator.validate("200", "", "", "", weight = "-50"))
        assertNotNull(MealFormValidator.validate("200", "", "", "", weight = "abc"))
        assertNull(MealFormValidator.validate("200", "20", "5", "30", weight = "150"))
        assertNull(MealFormValidator.validate("200", "", "", "", weight = ""))
    }

    @Test
    fun `BodyStatsValidator rejects NaN malformed optional fields and muscle above weight`() {
        assertNotNull(BodyStatsValidator.validate("NaN", "", ""))
        assertNotNull(BodyStatsValidator.validate("70", "abc", ""))
        assertNotNull(BodyStatsValidator.validate("70", "NaN", ""))
        assertNotNull(BodyStatsValidator.validate("70", "", "xyz"))
        assertNotNull(BodyStatsValidator.validate("70", "", "75"))
        assertNull(BodyStatsValidator.validate("70", "18.5", "32.0"))
        assertNull(BodyStatsValidator.validate("70", "", ""))
    }

    @Test
    fun `FoodTemplateMapper normalizes per 100g cleanly and handles invalid weights`() {
        val mapped = FoodTemplateMapper.fromMealRecord(
            MealRecord(
                name = " 鸡胸肉 ",
                calories = 200.0,
                protein = 30.0,
                fat = 5.0,
                carbs = 2.0,
                weightGrams = 150.0,
                micronutrients = listOf(MealMicro("iron", 3.0))
            )
        )
        assertEquals("鸡胸肉", mapped.name)
        assertEquals(133.3, mapped.calories, 0.001)
        assertEquals(20.0, mapped.protein, 0.001)
        assertEquals(3.3, mapped.fat, 0.001)
        assertEquals(1.3, mapped.carbs, 0.001)
        assertEquals(2.0, mapped.micronutrients.single().value, 0.001)
        assertEquals("每100g", mapped.source)

        val fallback = FoodTemplateMapper.fromMealRecord(
            MealRecord(name = "米饭", calories = 116.0, weightGrams = 0.0)
        )
        assertEquals(116.0, fallback.calories, 0.001)
    }

    @Test
    fun `DateUtils isValidIsoDate validates calendar dates strictly`() {
        assertTrue(DateUtils.isValidIsoDate("2026-11-18"))
        assertFalse(DateUtils.isValidIsoDate("2026-02-30"))
        assertFalse(DateUtils.isValidIsoDate("2026-1-1"))
        assertFalse(DateUtils.isValidIsoDate("bad"))
    }

    @Test
    fun `PresetFoodTemplates categories match actual food types`() {
        val byName = PresetFoodTemplates.getAll().associateBy { it.name }
        assertEquals(listOf("水果"), byName.getValue("牛油果").tags)
        assertEquals(listOf("蔬菜"), byName.getValue("油菜").tags)
        assertEquals(listOf("主食"), byName.getValue("油条").tags)
        assertEquals(listOf("豆制品"), byName.getValue("油豆腐").tags)
        assertTrue("豆制品" in byName.getValue("黄豆").tags)
        assertTrue("水果" in byName.getValue("桂圆").tags)
    }

    @Test
    fun `MetabolismCalculator computes consistent BMR and macro targets`() {
        val profile = BodyProfile(
            gender = Gender.MALE,
            age = 28,
            heightCm = 175,
            weightKg = 70.0,
            activityLevel = ActivityLevel.MODERATE
        )
        val result = MetabolismCalculator.calculate(profile, NutrientConstants.getDefaultTargets().micronutrients)
        assertEquals(1658.8, result.bmr, 0.1)
        assertTrue(result.tdee > result.bmr)
        assertTrue(result.targets.isAutoCalculated)
    }
}
