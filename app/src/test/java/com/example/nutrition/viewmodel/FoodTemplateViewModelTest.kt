package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealMicro
import com.example.nutrition.domain.usecase.BackupManagerTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodTemplateViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanUp() { store.clear() }

    @Test
    fun editingTemplatePreservesMicronutrientsSourceAndCreatedAt() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val original = FoodTemplate(
            id = "tpl-1",
            name = "牛奶",
            calories = 54.0,
            protein = 3.0,
            fat = 3.2,
            carbs = 4.7,
            micronutrients = listOf(MealMicro("calcium", 104.0)),
            tags = listOf("蛋奶"),
            isPreset = true,
            source = "每100g",
            createdAt = "2026-01-01T00:00:00Z"
        )
        repository.saveFoodTemplate(original)
        val viewModel = FoodTemplateViewModel(repository)
        store.put("templates", viewModel)
        runCurrent()

        viewModel.openEditDialog(original)
        viewModel.onCaloriesInput("60")
        viewModel.onNewTagInput("饮品")
        viewModel.saveTemplate()
        runCurrent()

        val saved = repository.getAllFoodTemplates().first().single { it.id == "tpl-1" }
        assertEquals(60.0, saved.calories, 0.001)
        assertEquals(original.micronutrients, saved.micronutrients)
        assertEquals("每100g", saved.source)
        assertEquals("2026-01-01T00:00:00Z", saved.createdAt)
        assertTrue(saved.isPreset)
        assertEquals(setOf("蛋奶", "饮品"), saved.tags.toSet())
    }

    @Test
    fun invalidMacrosOrCaloriesAreRejectedWithoutSaving() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val viewModel = FoodTemplateViewModel(repository)
        store.put("templates", viewModel)
        runCurrent()

        viewModel.openAddDialog()
        viewModel.onNameInput("测试食物")
        viewModel.onCaloriesInput("100")
        viewModel.onProteinInput("-5")
        viewModel.saveTemplate()
        runCurrent()
        assertTrue(repository.getAllFoodTemplates().first().isEmpty())

        viewModel.onProteinInput("10")
        viewModel.onFatInput("NaN")
        viewModel.saveTemplate()
        runCurrent()
        assertTrue(repository.getAllFoodTemplates().first().isEmpty())
    }

    @Test
    fun deletingTemplatePrunesStaleSelectedFilterTags() = runTest {
        val templatesFlow = kotlinx.coroutines.flow.MutableStateFlow(
            listOf(FoodTemplate(id = "custom-1", name = "自制餐", calories = 200.0, tags = listOf("专属标签")))
        )
        val repository = object : com.example.nutrition.domain.repository.LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getAllFoodTemplates() = templatesFlow
        }
        val viewModel = FoodTemplateViewModel(repository)
        store.put("templates", viewModel)
        runCurrent()

        viewModel.toggleFilterTag("专属标签")
        assertTrue("专属标签" in viewModel.uiState.value.selectedFilterTags)

        templatesFlow.value = emptyList()
        runCurrent()
        assertFalse("专属标签" in viewModel.uiState.value.selectedFilterTags)
    }
}
