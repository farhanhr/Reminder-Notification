package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReminderViewModel(context: Context) : ViewModel() {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE)

    private val _isReminderEnabled = MutableLiveData<Boolean>().apply {
        value = sharedPreferences.getBoolean("isReminderEnabled", false)
    }
    val isReminderEnabled: LiveData<Boolean> = _isReminderEnabled

    private val _reminderTimeString = MutableLiveData<String>().apply {
        value = sharedPreferences.getString("reminderTime", "Click to set the time")
    }

    val reminderTimeString: LiveData<String> = _reminderTimeString

    private val _reminderTime = MutableLiveData<Pair<Int, Int>>().apply {
        val hour = sharedPreferences.getInt("selectedHour", -1)
        val minute = sharedPreferences.getInt("selectedMinute", -1)
        value = Pair(hour, minute)
    }
    val reminderTime: LiveData<Pair<Int, Int>> = _reminderTime

    fun saveReminderState(isEnabled: Boolean) {
        _isReminderEnabled.value = isEnabled
        sharedPreferences.edit().putBoolean("isReminderEnabled", isEnabled).apply()
    }

    fun saveReminderTimeString(time: String) {
        _reminderTimeString.value = time
        sharedPreferences.edit().putString("reminderTime", time).apply()
    }

    fun saveReminderTime(hour: Int, minute: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt("selectedHour", hour)
        editor.putInt("selectedMinute", minute)
        editor.apply()
        _reminderTime.value = Pair(hour, minute)
    }

}