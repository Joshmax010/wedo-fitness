package com.example.nutrition.domain.usecase

import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealRecord
import kotlin.math.roundToInt

/**
 * 记录 → 模板 映射 —— 将一条饮食记录折算为每 100g 基准的食物模板
 *
 * 供"保存为模板"功能使用，替代原先写在 ViewModel 中的换算逻辑
 */
object FoodTemplateMapper {

    private fun roundOne(value: Double): Double {
        if (!value.isFinite()) return 0.0
        return (value * 10.0).roundToInt() / 10.0
    }

    /** 将一条饮食记录折算为每 100g 的模板 */
    fun fromMealRecord(record: MealRecord): FoodTemplate {
        val w = record.weightGrams.takeIf { it.isFinite() && it > 0.0 }?.coerceAtLeast(0.01) ?: 100.0
        val factor = 100.0 / w
        return FoodTemplate(
            name = record.name.trim(),
            calories = roundOne(record.calories.takeIf { it.isFinite() && it >= 0.0 }?.times(factor) ?: 0.0),
            protein = roundOne(record.protein.takeIf { it.isFinite() && it >= 0.0 }?.times(factor) ?: 0.0),
            fat = roundOne(record.fat.takeIf { it.isFinite() && it >= 0.0 }?.times(factor) ?: 0.0),
            carbs = roundOne(record.carbs.takeIf { it.isFinite() && it >= 0.0 }?.times(factor) ?: 0.0),
            micronutrients = record.micronutrients.map { micro ->
                val v = micro.value.takeIf { it.isFinite() && it >= 0.0 }?.times(factor) ?: 0.0
                micro.copy(value = roundOne(v))
            },
            isPreset = false,
            source = "每100g"
        )
    }
}
