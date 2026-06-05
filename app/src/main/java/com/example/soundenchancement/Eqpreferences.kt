package com.example.soundenhancement

import android.content.Context
import android.content.SharedPreferences


class EqPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)


    fun saveBaseLevel(progress: Int) =
        prefs.edit().putInt(KEY_BASE_LEVEL, progress).apply()

    fun saveBandProgress(band: Int, progress: Int) =
        prefs.edit().putInt(bandKey(band), progress).apply()

    fun saveIsActive(active: Boolean) =
        prefs.edit().putBoolean(KEY_IS_ACTIVE, active).apply()


    fun loadBaseLevel(): Int =
        prefs.getInt(KEY_BASE_LEVEL, DEFAULT_BASE_LEVEL)

    fun loadBandProgress(band: Int): Int =
        prefs.getInt(bandKey(band), DEFAULT_BAND_PROGRESS[band])

    fun loadIsActive(): Boolean =
        prefs.getBoolean(KEY_IS_ACTIVE, true)


    private fun bandKey(band: Int) = "${KEY_BAND_PREFIX}$band"

    companion object {
        private const val PREF_FILE = "eq_settings"
        private const val KEY_BASE_LEVEL = "base_level"
        private const val KEY_BAND_PREFIX = "band_"
        private const val KEY_IS_ACTIVE = "is_active"

        const val DEFAULT_BASE_LEVEL = 700

        val DEFAULT_BAND_PROGRESS = intArrayOf(140, 120, 60, 90, 110)
    }
}