package com.example.nutrition.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.nutrition.domain.model.NutritionTargets
import com.example.nutrition.domain.usecase.DateUtils
import com.example.nutrition.domain.model.DayRecords
import com.example.nutrition.domain.model.FoodTemplate
import com.example.nutrition.domain.model.MealKey
import com.example.nutrition.domain.model.MealRecord
import com.example.nutrition.domain.model.Resource
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManagerTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun cleanUp() { store.clear() }

    @Test
    fun historicalHomeSelectionWritesToThatDateAndMeal() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val date = "2026-09-28"
        repository.setDayRecords(date, DayRecords(date, dinner = listOf(MealRecord(calories = 250.0))))
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.DINNER, date)
        runCurrent()
        assertEquals(date, viewModel.uiState.value.currentDate)
        assertEquals(MealKey.DINNER, viewModel.uiState.value.currentMeal)
        assertEquals(1, viewModel.uiState.value.recordList.size)
        viewModel.onNameInput("新晚餐")
        viewModel.onCaloriesInput("300")
        viewModel.saveRecord()
        runCurrent()
        assertEquals(2, repository.getDayRecords(date).first().dinner.size)
    }

    @Test
    fun successfulTodaySave_reportsGoalEligibility() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        repository.setTargets(NutritionTargets(2000.0, 120.0, 60.0, 250.0, emptyList()))
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST, DateUtils.today())
        runCurrent()
        viewModel.onCaloriesInput("2000")
        viewModel.onProteinInput("120")
        viewModel.onFatInput("60")
        viewModel.onCarbsInput("250")
        viewModel.saveRecord()
        runCurrent()
        assertEquals(UIEvent.SaveSuccess(DateUtils.today(), true, true), viewModel.events.first())
    }

    @Test
    fun successfulHistoricalSave_doesNotRequestGoalFeedback() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        repository.setTargets(NutritionTargets(2000.0, 120.0, 60.0, 250.0, emptyList()))
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST, "2020-01-01")
        runCurrent()
        viewModel.onCaloriesInput("2000")
        viewModel.onProteinInput("120")
        viewModel.onFatInput("60")
        viewModel.onCarbsInput("250")
        viewModel.saveRecord()
        runCurrent()
        assertEquals(UIEvent.SaveSuccess(), viewModel.events.first())
    }

    @Test
    fun optionalFeedbackReadFailure_doesNotReportACompletedWriteAsFailed() = runTest {
        val backing = BackupManagerTest.FakeRepository()
        val repository = object : LocalStorageRepository by backing {
            override fun getTargets() = flow<NutritionTargets?> { error("feedback read failed") }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST, DateUtils.today())
        runCurrent()
        viewModel.onCaloriesInput("300")
        viewModel.saveRecord()
        runCurrent()
        assertEquals(1, backing.getDayRecords(DateUtils.today()).first().breakfast.size)
        assertEquals(UIEvent.SaveSuccess(), viewModel.events.first())
    }

    @Test
    fun autocomplete_reusesOneSubscription_andRespondsToTemplateEdits() = runTest {
        val milk = FoodTemplate(id = "milk", name = "牛奶", calories = 60.0)
        val templates = MutableStateFlow(listOf(milk))
        var subscriptions = 0
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getAllFoodTemplates() = flow {
                subscriptions++
                emitAll(templates)
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        runCurrent()
        viewModel.onNameInput("牛")
        runCurrent()
        assertEquals(listOf(milk), viewModel.uiState.value.templateSuggestions)
        val updatedMilk = milk.copy(calories = 80.0)
        templates.value = listOf(updatedMilk)
        runCurrent()
        assertEquals(listOf(updatedMilk), viewModel.uiState.value.templateSuggestions)
        assertEquals(1, subscriptions)
    }

    @Test
    fun restoringPage_andSelectingSameMeal_preserveDraft() = runTest {
        val viewModel = RecordViewModel(BackupManagerTest.FakeRepository())
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST)
        viewModel.onNameInput("未保存的早餐")
        viewModel.onCaloriesInput("300")
        viewModel.initWithMeal(MealKey.BREAKFAST)
        viewModel.refreshMealByTime()
        viewModel.switchMeal(MealKey.BREAKFAST)
        runCurrent()
        assertEquals(MealKey.BREAKFAST, viewModel.uiState.value.currentMeal)
        assertEquals("未保存的早餐", viewModel.uiState.value.foodName)
        assertEquals("300", viewModel.uiState.value.calories)
    }

    @Test
    fun delayedSave_doesNotResetNewDraft() = runTest {
        val saved = CompletableDeferred<Unit>()
        var written: MealRecord? = null
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override suspend fun addRecord(dateStr: String, mealKey: MealKey, record: MealRecord): Resource<Unit> {
                written = record
                saved.await()
                return Resource.success()
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.onNameInput("第一餐")
        viewModel.onCaloriesInput("300")
        viewModel.saveRecord()
        runCurrent()
        viewModel.onNameInput("下一餐")
        viewModel.onCaloriesInput("400")
        saved.complete(Unit)
        runCurrent()
        assertEquals("第一餐", written?.name)
        assertEquals("下一餐", viewModel.uiState.value.foodName)
        assertEquals("400", viewModel.uiState.value.calories)
    }
    @Test
    fun changingDate_clearsPreviousRecords_beforeDelayedReadCompletes() = runTest {
        val loaded = CompletableDeferred<Unit>()
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getDayRecords(dateStr: String) = flow {
                if (dateStr == "2026-09-28") loaded.await()
                emit(DayRecords(dateStr, breakfast = if (dateStr == "2026-09-28") emptyList() else listOf(MealRecord(calories = 300.0))))
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST)
        viewModel.pickDate("2026-09-27")
        runCurrent()
        assertEquals(1, viewModel.uiState.value.recordList.size)
        viewModel.pickDate("2026-09-28")
        assertTrue(viewModel.uiState.value.recordList.isEmpty())
        assertTrue(viewModel.uiState.value.isLoadingRecords)
        runCurrent()
        loaded.complete(Unit)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoadingRecords)
        assertTrue(viewModel.uiState.value.recordList.isEmpty())
    }

    @Test
    fun readFailure_isVisible_andChangingDateCanRecover() = runTest {
        val repository = object : LocalStorageRepository by BackupManagerTest.FakeRepository() {
            override fun getDayRecords(dateStr: String) = flow {
                if (dateStr == "2026-09-28") error("test read failure")
                emit(DayRecords(dateStr))
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        runCurrent()
        viewModel.pickDate("2026-09-28")
        runCurrent()
        assertNotNull(viewModel.uiState.value.dataError)
        assertFalse(viewModel.uiState.value.isLoadingRecords)
        viewModel.pickDate("2026-09-29")
        runCurrent()
        assertNull(viewModel.uiState.value.dataError)
        assertFalse(viewModel.uiState.value.isLoadingRecords)
    }

    @Test
    fun applyTemplateAndEditRecordKeepSuggestionsClosedUntilTypedAgain() = runTest {
        val milk = FoodTemplate(id = "milk", name = "牛奶", calories = 60.0, protein = 3.0)
        val today = DateUtils.today()
        val backing = BackupManagerTest.FakeRepository()
        backing.saveFoodTemplate(milk)
        val dayRecordsFlow = MutableStateFlow(DayRecords(today))
        val repository = object : LocalStorageRepository by backing {
            override fun getDayRecords(dateStr: String) = dayRecordsFlow
            override suspend fun addRecord(dateStr: String, mealKey: MealKey, record: MealRecord): Resource<Unit> {
                val res = backing.addRecord(dateStr, mealKey, record)
                dayRecordsFlow.value = backing.getDayRecords(dateStr).first()
                return res
            }
        }
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        runCurrent()

        viewModel.onNameInput("牛")
        runCurrent()
        assertEquals(listOf(milk), viewModel.uiState.value.templateSuggestions)

        viewModel.applyTemplate(milk)
        runCurrent()
        assertTrue(viewModel.uiState.value.templateSuggestions.isEmpty())
        assertEquals("牛奶", viewModel.uiState.value.foodName)

        viewModel.onWeightInput("0")
        viewModel.saveRecord()
        runCurrent()
        assertTrue(backing.getDayRecords(viewModel.uiState.value.currentDate).first().isEmpty())

        viewModel.applyTemplate(milk)
        runCurrent()
        assertEquals("100", viewModel.uiState.value.weight)
        assertEquals("60", viewModel.uiState.value.calories)

        viewModel.saveRecord()
        runCurrent()
        val savedId = backing.getDayRecords(viewModel.uiState.value.currentDate).first()
            .getMeal(viewModel.uiState.value.currentMeal).single().id

        viewModel.editRecord(savedId)
        runCurrent()
        assertEquals(savedId, viewModel.uiState.value.editingId)
        assertTrue(viewModel.uiState.value.templateSuggestions.isEmpty())

        viewModel.onNameInput("牛")
        runCurrent()
        assertEquals(listOf(milk), viewModel.uiState.value.templateSuggestions)
    }

    @Test
    fun switchingDateWhileEditingAndDeletingEditingRecordResetForm() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val today = DateUtils.today()
        val existing = MealRecord(id = "rec-1", name = "早餐麦片", calories = 300.0)
        repository.setDayRecords(today, DayRecords(today, breakfast = listOf(existing)))
        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST, today)
        runCurrent()

        viewModel.editRecord("rec-1")
        assertEquals("rec-1", viewModel.uiState.value.editingId)

        viewModel.prevDay()
        runCurrent()
        assertNull(viewModel.uiState.value.editingId)
        assertEquals("", viewModel.uiState.value.foodName)

        viewModel.nextDay()
        runCurrent()
        viewModel.editRecord("rec-1")
        viewModel.requestDelete("rec-1")
        viewModel.confirmDelete()
        runCurrent()
        assertNull(viewModel.uiState.value.editingId)
        assertEquals("", viewModel.uiState.value.foodName)
        assertTrue(repository.getDayRecords(today).first().isEmpty())
    }

    @Test
    fun initWithMealExplicitNavigationOverridesEditAndRestoringWithoutArgsPreservesDraft() = runTest {
        val repository = BackupManagerTest.FakeRepository()
        val today = DateUtils.today()
        val yesterday = DateUtils.prevDay(today)
        repository.setDayRecords(today, DayRecords(today, breakfast = listOf(MealRecord(id = "r-today", name = "今日蛋", calories = 150.0))))
        repository.setDayRecords(yesterday, DayRecords(yesterday, dinner = listOf(MealRecord(id = "r-yest", name = "昨日面", calories = 400.0))))

        val viewModel = RecordViewModel(repository)
        store.put("record", viewModel)
        viewModel.initWithMeal(MealKey.BREAKFAST, today)
        runCurrent()

        viewModel.editRecord("r-today")
        assertEquals("r-today", viewModel.uiState.value.editingId)

        // 无参切回（模拟底部 Tab 切回）应保留草稿/编辑态
        viewModel.initWithMeal(null, null)
        runCurrent()
        assertEquals(today, viewModel.uiState.value.currentDate)
        assertEquals("r-today", viewModel.uiState.value.editingId)

        // 从首页显式点击另一天餐次应切换目标并清除旧 editingId
        viewModel.initWithMeal(MealKey.DINNER, yesterday)
        runCurrent()
        assertEquals(yesterday, viewModel.uiState.value.currentDate)
        assertEquals(MealKey.DINNER, viewModel.uiState.value.currentMeal)
        assertNull(viewModel.uiState.value.editingId)
        assertEquals("", viewModel.uiState.value.foodName)
        assertEquals("r-yest", viewModel.uiState.value.recordList.single().id)
    }

}
