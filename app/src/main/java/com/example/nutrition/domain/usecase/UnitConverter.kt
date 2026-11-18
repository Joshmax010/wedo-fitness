package com.example.nutrition.domain.usecase

import java.util.Locale
import kotlin.math.roundToLong

/**
 * 单位换算工具 —— 千卡与千焦互转、数值格式化
 *
 * 供录入表单（kcal/kJ 双向联动）与身体数据回填共用，
 * 替代原先散落在各 ViewModel 中的重复实现
 */
object UnitConverter {

    /** 1 千卡 = 4.184 千焦 */
    const val KJ_PER_KCAL = 4.184

    /** 千卡输入 → 千焦显示文本（输入为空或非法时返回空串） */
    fun caloriesToKiloJoules(value: String): String {
        return value.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
            ?.let { formatForInput(it * KJ_PER_KCAL) } ?: ""
    }

    /** 千焦输入 → 千卡显示文本（输入为空或非法时返回空串） */
    fun kiloJoulesToCalories(value: String): String {
        return value.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
            ?.let { formatForInput(it / KJ_PER_KCAL) } ?: ""
    }

    /** 千卡数值 → 千焦数值 */
    fun kcalToKj(kcal: Double): Double = kcal * KJ_PER_KCAL

    /** 千焦数值 → 千卡数值 */
    fun kjToKcal(kj: Double): Double = kj / KJ_PER_KCAL

    /** 格式化营养素数值展示（整数不带小数点，小数保留1位） */
    fun formatNutrient(value: Double): String = formatForInput(value)

    /** 格式化为输入框友好文本：先四舍五入到 1 位小数，整数不带小数点，非整数强制使用小数点格式 */
    fun formatForInput(value: Double): String {
        if (!value.isFinite()) return ""
        val roundedTenths = (value * 10.0).roundToLong()
        return if (roundedTenths % 10L == 0L) {
            (roundedTenths / 10L).toString()
        } else {
            String.format(Locale.US, "%.1f", roundedTenths / 10.0)
        }
    }
}
