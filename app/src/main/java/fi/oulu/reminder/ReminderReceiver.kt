package fi.oulu.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Retrieve data from intent
        val uid = intent.getIntExtra("uid", 0)
        val text = intent.getStringExtra("message")


        // TODO: Implement trigger event: e.g. notification
        context.toast(text!!)

        MainActivity.ShowNofitication(context,text!!)

        // Remove reminder from db after it was shown
        doAsync {
            val db = Room.databaseBuilder(context, AppDatabase::class.java,"reminders")
                .build()
            db.reminderDao().delete(uid)
            db.close()
        }
    }
}