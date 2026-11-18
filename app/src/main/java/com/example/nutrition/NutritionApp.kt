package com.example.nutrition

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.nutrition.data.local.db.NutritionDatabase
import com.example.nutrition.data.repository.RoomLocalStorageRepository
import com.example.nutrition.domain.constants.NutrientConstants
import com.example.nutrition.domain.repository.LocalStorageRepository
import com.example.nutrition.domain.usecase.BackupManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NutritionApp : Application() {

    val database: NutritionDatabase by lazy { NutritionDatabase.getInstance(this) }
    val repository: LocalStorageRepository by lazy { RoomLocalStorageRepository(database) }
    val backupManager: BackupManager by lazy { BackupManager(repository) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 启动时初始化默认数据并迁移 schema/应用版本号
        appScope.launch {
            try {
                initializeOrMigrateAppState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NutritionApp", "Unable to initialize or migrate application state", e)
            }
        }

        // 启动时静默检测版本更新（遵循设置开关，离线/超时静默失败）
        appScope.launch {
            try {
                com.example.nutrition.updater.UpdateManager.getInstance(this@NutritionApp).checkUpdate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d("NutritionApp", "Silent update check failed or skipped", e)
            }
        }
    }

    /**
     * 首次安装补齐默认目标与元信息，老版本同步迁移 schemaVersion 与 version
     */
    private suspend fun initializeOrMigrateAppState() {
        if (repository.getTargets().first() == null) {
            repository.setTargets(NutrientConstants.getDefaultTargets())
        }
        val meta = repository.getMeta().first()
        if (meta == null) {
            repository.setMeta(NutrientConstants.getDefaultMeta())
            return
        }
        val latestSchema = NutrientConstants.SCHEMA_VERSION
        val latestVersion = NutrientConstants.APP_VERSION
        if (meta.schemaVersion < latestSchema || meta.version != latestVersion) {
            repository.setMeta(
                meta.copy(
                    schemaVersion = maxOf(meta.schemaVersion, latestSchema),
                    version = latestVersion
                )
            )
        }
    }

    companion object {
        lateinit var instance: NutritionApp
            private set

        /** 便捷 ViewModel 工厂 —— 消除各 Screen 重复的 viewModelFactory 样板代码 */
        inline fun <reified VM : ViewModel> appViewModelFactory(
            crossinline create: (LocalStorageRepository, BackupManager) -> VM
        ) = viewModelFactory {
            initializer { create(instance.repository, instance.backupManager) }
        }
    }
}
