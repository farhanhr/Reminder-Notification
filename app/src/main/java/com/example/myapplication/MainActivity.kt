package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var switchReminder: MaterialSwitch
    private lateinit var tvReminderTime: TextView
    private lateinit var reminderViewModel: ReminderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        switchReminder = findViewById(R.id.switchReminder)
        tvReminderTime = findViewById((R.id.tvReminderTime))

        val factory = ReminderViewModelFactory(applicationContext)
        reminderViewModel = ViewModelProvider(this, factory).get(ReminderViewModel::class.java)

        reminderViewModel.isReminderEnabled.observe(this) { isEnabled ->
            switchReminder.isChecked = isEnabled
        }

        reminderViewModel.reminderTimeString.observe(this) { time ->
            tvReminderTime.text = time
        }

        tvReminderTime.setOnClickListener {
            showTimePicker()
        }

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (reminderViewModel.reminderTime.value == Pair(-1, -1)) {
                    requestExactAlarmPermission()
                } else {
                    reminderViewModel.reminderTime.observe(this) { time ->
                        val (selectedHour, selectedMinute) = time
                        setDailyReminder(selectedHour, selectedMinute)
                    }
                }
            } else {
                cancelReminder()
                reminderViewModel.saveReminderState(false)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimePicker() {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            reminderViewModel.saveReminderTimeString(formattedTime)
            reminderViewModel.saveReminderState(true)
            reminderViewModel.saveReminderTime(selectedHour, selectedMinute)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun setDailyReminder(hour: Int, minute: Int) {

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }
        calendar.timeZone = TimeZone.getDefault()
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Failed to set exact alarm: ${e.message}")
            Toast.makeText(this, "Exact alarm permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent)

    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                switchReminder.isChecked = false
                AlertDialog.Builder(this)
                    .setTitle("Exact Alarm Permission")
                    .setMessage("To schedule alarms at exact times, we need your permission to set exact alarms on your device. Please grant this permission in the settings.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                showTimePicker()
                reminderViewModel.reminderTime.observe(this) { time ->
                    val (selectedHour, selectedMinute) = time
                    setDailyReminder(selectedHour, selectedMinute)
                }
            }
        }
    }

}