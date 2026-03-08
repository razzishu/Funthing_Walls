package com.aesthetic.funthingwalls

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aesthetic.funthingwalls.databinding.FragmentSettingsBinding
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("FunthingPrefs", Context.MODE_PRIVATE)

        // 1. LOAD CURRENT SETTINGS
        binding.editApiKey.setText(prefs.getString("API_KEY", ""))
        binding.switchBlurHome.isChecked = prefs.getBoolean("BLUR_HOME_SCREEN", false)

        val currentTimer = prefs.getLong("AUTO_TIMER_HOURS", 6L)
        when (currentTimer) {
            1L -> binding.rb1Hour.isChecked = true
            6L -> binding.rb6Hours.isChecked = true
            24L -> binding.rb24Hours.isChecked = true
        }

        // 2. SAVE API KEY
        binding.btnSaveApiKey.setOnClickListener {
            val newKey = binding.editApiKey.text.toString().trim()
            if (newKey.isNotEmpty()) {
                prefs.edit().putString("API_KEY", newKey).apply()
                Toast.makeText(requireContext(), "API Key Updated!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. SAVE BLUR TOGGLE
        binding.switchBlurHome.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("BLUR_HOME_SCREEN", isChecked).apply()
            val msg = if (isChecked) "Blur Enabled for next wallpaper" else "Blur Disabled"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // 4. UPDATE WORKMANAGER TIMER
        binding.timerRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val hours = when (checkedId) {
                R.id.rb1Hour -> 1L
                R.id.rb6Hours -> 6L
                R.id.rb24Hours -> 24L
                else -> 6L
            }

            prefs.edit().putLong("AUTO_TIMER_HOURS", hours).apply()

            val workRequest = PeriodicWorkRequestBuilder<FunthingWorker>(hours, TimeUnit.HOURS).build()
            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "FunthingAutoUpdate",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Toast.makeText(requireContext(), "Auto-Change set to $hours Hours", Toast.LENGTH_SHORT).show()
        }

        // 5. OFFICIAL DEVELOPER LINKS
        binding.btnGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://github.com/razzishu")
            startActivity(intent)
        }

        binding.btnTelegram.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://t.me/Razz_ishu")
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}