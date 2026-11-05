package com.example.motionpulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import com.example.motionpulse.data.repository.PreferenceRepository
import com.example.motionpulse.databinding.ActivityMainBinding
import com.example.motionpulse.ui.notifications.HabitReminderReceiver
import com.example.motionpulse.util.LocaleHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        // Create Notification Channel for Habit Reminders
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            HabitReminderReceiver.CHANNEL_ID,
            getString(R.string.notifications_title),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for habit reminders"
        }
        notificationManager.createNotificationChannel(channel)

        // Apply saved locale before first layout inflation
        val preferenceRepository = PreferenceRepository(applicationContext)
        val savedSettings = preferenceRepository.getUserSettings()
        val targetLocale = LocaleHelper.resolveSupportedLocaleTag(savedSettings.language)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetLocale))

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Attach custom MotionPulseBottomNav View to NavController
        binding.bottomNav.attachNavController(navController)

        // Destination listener to toggle bottom nav visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboard,
                R.id.mood,
                R.id.habits,
                R.id.community,
                R.id.stats,
                R.id.profile -> {
                    binding.bottomNav.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNav.visibility = View.GONE
                }
            }
        }
    }
}
