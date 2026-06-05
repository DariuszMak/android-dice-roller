package com.example.soundenhancement

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {


    internal var isBassActive = false


    internal lateinit var eqPrefs: EqPreferences


    internal var audioService: AudioEffectService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            audioService = (binder as AudioEffectService.LocalBinder).getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            serviceBound = false
        }
    }


    internal lateinit var statusDot: TextView
    internal lateinit var statusLabel: TextView
    internal lateinit var btnStart: Button
    internal lateinit var btnStop: Button
    internal lateinit var btnReset: Button
    internal lateinit var eqPanel: View

    internal lateinit var sliderBaseLevel: SeekBar
    internal lateinit var labelBaseLevel: TextView

    internal val bandSliders = arrayOfNulls<SeekBar>(5)
    internal val bandLabels = arrayOfNulls<TextView>(5)

    private val bandNames = arrayOf(
        "≤ 100 Hz (Bass)",
        "≤ 500 Hz (Low-mid)",
        "≤ 2000 Hz (Mid)",
        "≤ 8000 Hz (Presence)",
        "> 8000 Hz (Air)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        eqPrefs = EqPreferences(this)

        bindWidgets()
        setupSliderListeners()
        restoreSliderState()


        isBassActive = eqPrefs.loadIsActive()
        if (isBassActive) startService(Intent(this, AudioEffectService::class.java))
        refreshUi()

        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener { onStopClicked() }
        btnReset.setOnClickListener { onResetClicked() }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, AudioEffectService::class.java),
            serviceConnection,
            0
        )
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
            audioService = null
        }
    }


    internal fun onStartClicked() {
        if (!isBassActive) {
            startService(Intent(this, AudioEffectService::class.java))
            bindService(
                Intent(this, AudioEffectService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )
            isBassActive = true
            eqPrefs.saveIsActive(true)
            audioService?.enableEq()
            refreshUi()
        }
    }

    internal fun onStopClicked() {
        if (isBassActive) {
            audioService?.disableEq()
            isBassActive = false
            eqPrefs.saveIsActive(false)
            refreshUi()
        }
    }


    internal fun onResetClicked() {
        sliderBaseLevel.progress = EqPreferences.DEFAULT_BASE_LEVEL
        for (i in 0 until 5) {
            bandSliders[i]?.progress = EqPreferences.DEFAULT_BAND_PROGRESS[i]
        }


        eqPrefs.saveBaseLevel(EqPreferences.DEFAULT_BASE_LEVEL)
        for (i in 0 until 5) {
            eqPrefs.saveBandProgress(i, EqPreferences.DEFAULT_BAND_PROGRESS[i])
        }

        if (isBassActive) audioService?.applyConfig(buildConfigFromSliders())
    }


    internal fun refreshUi() {
        if (isBassActive) {
            statusDot.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_light)
            )
            statusLabel.text = "Sound effect: ON"
            setEqPanelEnabled(true)
        } else {
            statusDot.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_light)
            )
            statusLabel.text = "Sound effect: OFF"
            setEqPanelEnabled(false)
        }
    }

    private fun setEqPanelEnabled(enabled: Boolean) {
        eqPanel.alpha = if (enabled) 1f else 0.38f
        setGroupEnabled(eqPanel, enabled)
    }

    private fun setGroupEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) setGroupEnabled(view.getChildAt(i), enabled)
        }
    }


    private fun bindWidgets() {
        statusDot = findViewById(R.id.statusDot)
        statusLabel = findViewById(R.id.statusLabel)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        eqPanel = findViewById(R.id.eqPanel)

        sliderBaseLevel = findViewById(R.id.sliderBaseLevel)
        labelBaseLevel = findViewById(R.id.labelBaseLevel)

        val sliderIds = intArrayOf(
            R.id.sliderBand0, R.id.sliderBand1, R.id.sliderBand2, R.id.sliderBand3,
            R.id.sliderBand4
        )
        val labelIds = intArrayOf(
            R.id.labelBand0, R.id.labelBand1, R.id.labelBand2, R.id.labelBand3,
            R.id.labelBand4
        )
        for (i in 0 until 5) {
            bandSliders[i] = findViewById(sliderIds[i])
            bandLabels[i] = findViewById(labelIds[i])
        }
    }

    private fun setupSliderListeners() {
        sliderBaseLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                labelBaseLevel.text = "Base Level: $progress mB"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                eqPrefs.saveBaseLevel(sliderBaseLevel.progress)
                if (isBassActive) audioService?.applyConfig(buildConfigFromSliders())
            }
        })

        for (i in 0 until 5) {
            val index = i
            bandSliders[i]?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    bandLabels[index]?.text =
                        "${bandNames[index]}: ${"%.2f".format(progress / 100.0)}×"
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    eqPrefs.saveBandProgress(index, bandSliders[index]?.progress ?: 0)
                    if (isBassActive) audioService?.applyConfig(buildConfigFromSliders())
                }
            })
        }
    }


    internal fun restoreSliderState() {
        sliderBaseLevel.progress = eqPrefs.loadBaseLevel()
        for (i in 0 until 5) {
            bandSliders[i]?.progress = eqPrefs.loadBandProgress(i)
        }
    }


    internal fun buildConfigFromSliders(): EqConfig {
        val baseLevel = sliderBaseLevel.progress
        val multipliers = DoubleArray(5) { i ->
            (bandSliders[i]?.progress ?: EqPreferences.DEFAULT_BAND_PROGRESS[i]) / 100.0
        }
        return EqConfig(baseLevel = baseLevel, multipliers = multipliers)
    }
}