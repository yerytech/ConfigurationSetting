package com.yerytech.datapercistence

import android.content.Context
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yerytech.datapercistence.databinding.ActivityPersistenceAppBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class PersistenceApp : AppCompatActivity() {
    companion object {
        const val VOLUME_LEVEL = "volume_level"
        const val DARK_MODE = "dark_mode"
        const val BLUETOOTH_ACTIVATION = "bluetooth_activation"
    }


    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "setting")
    private lateinit var binding: ActivityPersistenceAppBinding
    private var firsTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersistenceAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CoroutineScope(Dispatchers.IO).launch {
            getSettings().filter { firsTime }.collect { settingModel ->
                if (settingModel != null) {
                    runOnUiThread {
                        binding.btnTheme.isChecked = settingModel.darkMode
                        binding.btnBluetooth.isChecked = settingModel.bluetooth
                        binding.rsVolume.setValues(settingModel.volume.toFloat())
                        firsTime = !firsTime
                    }

                }

            }
        }

        initUI()
    }

    private fun initUI() {
        binding.rsVolume.addOnChangeListener { _, value, _ ->
            CoroutineScope(Dispatchers.IO).launch {
                saveVolume(value.toInt())
            }

        }
        binding.btnBluetooth.setOnCheckedChangeListener { _, b ->
            CoroutineScope(Dispatchers.IO).launch {
                saveConfiguration(BLUETOOTH_ACTIVATION, b)
            }
        }
        binding.btnTheme.setOnCheckedChangeListener { _, b ->
            runOnUiThread {
                if (b) {
                    enableDarkMode()
                } else {
                    disableDarkMode()
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                saveConfiguration(DARK_MODE, b)
            }

        }

    }

    private suspend fun saveVolume(value: Int) {
        dataStore.edit { setting ->
            setting[intPreferencesKey(VOLUME_LEVEL)] = value
        }
    }

    private suspend fun saveConfiguration(key: String, value: Boolean) {
        dataStore.edit { setting -> setting[booleanPreferencesKey(key)] = value }

    }

    private fun getSettings(): Flow<SettingModel?> {
        return dataStore.data.map { setting ->
            SettingModel(
                volume = setting[intPreferencesKey(VOLUME_LEVEL)] ?: 50,
                darkMode = setting[booleanPreferencesKey(DARK_MODE)] ?: false,
                bluetooth = setting[booleanPreferencesKey(BLUETOOTH_ACTIVATION)] ?: false
            )

        }

    }

    private fun enableDarkMode() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        delegate.applyDayNight()

    }

    private fun disableDarkMode() {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
        delegate.applyDayNight()
    }
}

