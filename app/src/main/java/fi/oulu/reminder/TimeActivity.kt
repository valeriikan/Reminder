package fi.oulu.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_time.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.util.*

class TimeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time)

        // Save the entry on button clicked
        time_create.setOnClickListener {

            // Retrieve values from time and date pickers
            val calendar = GregorianCalendar(
                date_picker.year,
                date_picker.month,
                date_picker.dayOfMonth,
                time_picker.currentHour,
                time_picker.currentMinute
            )

            // Check if reminder is valid
            if ((et_message.text.toString() != "") &&
                (calendar.timeInMillis > System.currentTimeMillis())) {

                // Create a reminder instance
                val reminder = Reminder(
                    uid = null,
                    type = "time",
                    trigger = calendar.timeInMillis.toString(),
                    message = et_message.text.toString()
                )

                // Save it to database
                doAsync {
                    val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java,"reminders")
                        .build()

                    // Get the id of the inserted reminder
                    val uid = db.reminderDao().insert(reminder).toInt()
                    db.close()

                    // Schedule a reminder
                    setAlarm(uid, calendar.timeInMillis, reminder.message)

                    // Finish activity
                    finish()
                }

            } else {
                // If reminder is irrelevant
                toast("Reminder cannot be scheduled for the past time and should contain some text")
            }
        }
    }

    // Schedule a reminder via AlarmManager
    private fun setAlarm(uid: Int, time: Long, message: String) {

        // Specify the broadcast receiver via intent
        val intent = Intent(this, ReminderReceiver::class.java)
        intent.putExtra("uid", uid)
        intent.putExtra("message", message)

        // Create a pending intent using the intent; request code should be unique
        val pending = PendingIntent.getBroadcast(this, uid, intent, PendingIntent.FLAG_ONE_SHOT)

        // Get an instance of AlarmManager, set up the alarm
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.setExact(AlarmManager.RTC, time, pending)
        runOnUiThread { toast("Reminder is created") }
    }
}
