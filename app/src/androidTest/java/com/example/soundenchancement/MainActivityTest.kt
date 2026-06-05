package com.example.soundenhancement

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("eq_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Before
    fun setUp() = clearPrefs()

    @After
    fun tearDown() = clearPrefs()


    @Test
    fun onLaunch_statusShouldShowOn() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Sound effect: ON", activity.statusLabel.text.toString())
            }
        }
    }

    @Test
    fun onLaunch_stopButtonShouldTurnOffStatus() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertEquals("Sound effect: OFF", activity.statusLabel.text.toString())
            }
        }
    }


    @Test
    fun onLaunch_eqPanelIsEnabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(1f, activity.eqPanel.alpha, 0.01f)
                assertTrue(activity.sliderBaseLevel.isEnabled)
            }
        }
    }

    @Test
    fun stopButton_greysOutEqPanel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertEquals(0.38f, activity.eqPanel.alpha, 0.01f)
            }
        }
    }

    @Test
    fun stopButton_disablesSlidersAndLabels() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertFalse(activity.sliderBaseLevel.isEnabled)
                for (i in 0 until 5) {
                    assertFalse(
                        "Band slider $i should be disabled",
                        activity.bandSliders[i]?.isEnabled ?: true
                    )
                }
            }
        }
    }

    @Test
    fun startButton_reEnablesEqPanel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                activity.btnStart.performClick()
                assertTrue(activity.isBassActive)
                assertEquals(1f, activity.eqPanel.alpha, 0.01f)
                assertTrue(activity.sliderBaseLevel.isEnabled)
            }
        }
    }

    @Test
    fun buildConfigFromSliders_returnsDefaultConfig_onFreshLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = activity.buildConfigFromSliders()
                assertEquals(700, config.baseLevel)
                assertEquals(1.40, config.multipliers[0], 0.01)
                assertEquals(1.20, config.multipliers[1], 0.01)
                assertEquals(0.60, config.multipliers[2], 0.01)
                assertEquals(0.90, config.multipliers[3], 0.01)
                assertEquals(1.10, config.multipliers[4], 0.01)
            }
        }
    }

    @Test
    fun buildConfigFromSliders_reflectsChangedSliderPosition() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.sliderBaseLevel.progress = 1000
                activity.bandSliders[0]?.progress = 150
                val config = activity.buildConfigFromSliders()
                assertEquals(1000, config.baseLevel)
                assertEquals(1.50, config.multipliers[0], 0.01)
            }
        }
    }


    @Test
    fun baseLevelSlider_updatesLabelOnChange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.sliderBaseLevel.progress = 1200
                assertTrue(activity.labelBaseLevel.text.contains("1200"))
            }
        }
    }

    @Test
    fun bandSlider_updatesLabelOnChange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.bandSliders[2]?.progress = 130
                val label = activity.bandLabels[2]?.text.toString()
                assertTrue("Expected '1.30' in label, got: $label", label.contains("1.30"))
            }
        }
    }


    @Test
    fun isBassActive_isTrueOnFreshLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertTrue(activity.isBassActive) }
        }
    }

    @Test
    fun isBassActive_isFalseAfterStop_TrueAfterStart() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                assertFalse(activity.isBassActive)
                activity.btnStart.performClick()
                assertTrue(activity.isBassActive)
            }
        }
    }

    @Test
    fun doubleStop_doesNotChangeState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnStop.performClick()
                activity.btnStop.performClick()
                assertFalse(activity.isBassActive)
                assertEquals("Sound effect: OFF", activity.statusLabel.text.toString())
            }
        }
    }


    @Test
    fun sliderValues_areRestoredAfterRelaunch() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(1100)
        prefs.saveBandProgress(0, 180)
        prefs.saveBandProgress(3, 60)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(1100, activity.sliderBaseLevel.progress)
                assertEquals(180, activity.bandSliders[0]?.progress)
                assertEquals(60, activity.bandSliders[3]?.progress)
            }
        }
    }


    @Test
    fun sliderLabels_reflectRestoredValues_onRelaunch() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(900)
        prefs.saveBandProgress(1, 160)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    "Base level label should show 900, got: ${activity.labelBaseLevel.text}",
                    activity.labelBaseLevel.text.contains("900")
                )
                val band1Label = activity.bandLabels[1]?.text.toString()
                assertTrue(
                    "Band 1 label should show 1.60, got: $band1Label",
                    band1Label.contains("1.60")
                )
            }
        }
    }

    @Test
    fun isActiveState_isRestoredAfterRelaunch_whenOff() {
        EqPreferences(ApplicationProvider.getApplicationContext()).saveIsActive(false)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isBassActive)
                assertEquals("Sound effect: OFF", activity.statusLabel.text.toString())
                assertEquals(0.38f, activity.eqPanel.alpha, 0.01f)
            }
        }
    }

    @Test
    fun isActiveState_isRestoredAfterRelaunch_whenOn() {
        EqPreferences(ApplicationProvider.getApplicationContext()).saveIsActive(true)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isBassActive)
                assertEquals("Sound effect: ON", activity.statusLabel.text.toString())
                assertEquals(1f, activity.eqPanel.alpha, 0.01f)
            }
        }
    }

    @Test
    fun restoreSliderState_loadsAllNineSlidersFromPrefs() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(300)
        for (i in 0 until 5) prefs.saveBandProgress(i, 50 + i * 10)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(300, activity.sliderBaseLevel.progress)
                for (i in 0 until 5) {
                    assertEquals(
                        "Band $i progress should be ${50 + i * 10}",
                        50 + i * 10,
                        activity.bandSliders[i]?.progress
                    )
                }
            }
        }
    }

    @Test
    fun buildConfigFromSliders_matchesRestoredPrefs() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(500)
        prefs.saveBandProgress(0, 120)
        prefs.saveBandProgress(4, 95)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val config = activity.buildConfigFromSliders()
                assertEquals(500, config.baseLevel)
                assertEquals(1.20, config.multipliers[0], 0.01)
                assertEquals(0.95, config.multipliers[4], 0.01)
            }
        }
    }

    @Test
    fun resetButton_restoresSlidersToDefaultProgress() {

        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(1400)
        for (i in 0 until 5) prefs.saveBandProgress(i, 200)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnReset.performClick()

                assertEquals(EqPreferences.DEFAULT_BASE_LEVEL, activity.sliderBaseLevel.progress)
                for (i in 0 until 5) {
                    assertEquals(
                        "Band $i should be reset to default ${EqPreferences.DEFAULT_BAND_PROGRESS[i]}",
                        EqPreferences.DEFAULT_BAND_PROGRESS[i],
                        activity.bandSliders[i]?.progress
                    )
                }
            }
        }
    }

    @Test
    fun resetButton_updatesLabelsToDefaultValues() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->

                activity.sliderBaseLevel.progress = 1400
                activity.bandSliders[0]?.progress = 200

                activity.btnReset.performClick()

                assertTrue(
                    "Base level label should contain 700 after reset, got: ${activity.labelBaseLevel.text}",
                    activity.labelBaseLevel.text.contains("700")
                )
                val band0Label = activity.bandLabels[0]?.text.toString()
                assertTrue(
                    "Band 0 label should contain 1.40 after reset, got: $band0Label",
                    band0Label.contains("1.40")
                )
            }
        }
    }

    @Test
    fun resetButton_persistsDefaultValuesToPrefs() {
        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(1400)
        for (i in 0 until 5) prefs.saveBandProgress(i, 200)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.btnReset.performClick()
            }
        }


        val prefs2 = EqPreferences(ApplicationProvider.getApplicationContext())
        assertEquals(EqPreferences.DEFAULT_BASE_LEVEL, prefs2.loadBaseLevel())
        for (i in 0 until 5) {
            assertEquals(
                "Saved band $i should be default after reset",
                EqPreferences.DEFAULT_BAND_PROGRESS[i],
                prefs2.loadBandProgress(i)
            )
        }
    }

    @Test
    fun resetButton_restoredDefaultsSurviveRelaunch() {

        val prefs = EqPreferences(ApplicationProvider.getApplicationContext())
        prefs.saveBaseLevel(1400)
        for (i in 0 until 5) prefs.saveBandProgress(i, 200)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.btnReset.performClick() }
        }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(EqPreferences.DEFAULT_BASE_LEVEL, activity.sliderBaseLevel.progress)
                assertTrue(
                    "Label should show default 700 after relaunch, got: ${activity.labelBaseLevel.text}",
                    activity.labelBaseLevel.text.contains("700")
                )
            }
        }
    }

    @Test
    fun resetButton_buildConfigMatchesDefaults() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.sliderBaseLevel.progress = 1500
                for (i in 0 until 5) activity.bandSliders[i]?.progress = 200

                activity.btnReset.performClick()

                val config = activity.buildConfigFromSliders()
                assertEquals(EqPreferences.DEFAULT_BASE_LEVEL, config.baseLevel)
                assertEquals(1.40, config.multipliers[0], 0.01)
                assertEquals(1.20, config.multipliers[1], 0.01)
                assertEquals(0.60, config.multipliers[2], 0.01)
                assertEquals(0.90, config.multipliers[3], 0.01)
                assertEquals(1.10, config.multipliers[4], 0.01)
            }
        }
    }
}