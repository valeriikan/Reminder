package fi.oulu.reminder

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Flag for fabs opening state
        var fabOpened = false

        // Perform actions when fab is clicked
        fab.setOnClickListener {

            if (!fabOpened) {
                // Display two more fabs
                fabOpened = true
                fab_map.animate().translationY(-resources.getDimension(R.dimen.standard_66))
                fab_time.animate().translationY(-resources.getDimension(R.dimen.standard_116))

            } else {
                // Hide fabs
                fabOpened = false
                fab_map.animate().translationY(0f)
                fab_time.animate().translationY(0f)
            }
        }

        // Open activity for setting up time-based reminder
        fab_time.setOnClickListener {
            startActivity(Intent(applicationContext, TimeActivity::class.java))
        }

        // Open activity for setting up location-based reminder
        fab_map.setOnClickListener {
            startActivity(Intent(applicationContext, MapActivity::class.java))
        }

        // Listener that performs action on row element click
        list.onItemClickListener = AdapterView.OnItemClickListener{ _, _, position, _ ->

            // Retrieve Reminder corresponding to the clicked item
            val selected = list.adapter.getItem(position) as Reminder

            // Show AlertDialog to delete the reminder
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Delete reminder?")
                .setMessage(selected.message)
                .setPositiveButton("Delete") { _, _ ->

                    // Cancel scheduled reminder with AlarmManager
                    if (selected.time != null) {
                        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val intent = Intent(this@MainActivity, ReminderReceiver::class.java)
                        val pending = PendingIntent.getBroadcast(this@MainActivity,
                            selected.uid!!, intent, PendingIntent.FLAG_ONE_SHOT)
                        manager.cancel(pending)
                    }

                    // Remove reminder from db
                    doAsync {
                        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java,"reminders")
                            .build()
                        db.reminderDao().delete(selected.uid!!)
                        db.close()

                        // Update UI
                        refreshList()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    // Do nothing
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Update UI
        refreshList()
    }

    // Retrieve the latest actual list of Reminders and refresh UI
    private fun refreshList() {

        doAsync {
            val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "reminders")
                .build()
            val reminders = db.reminderDao().getReminders()
            db.close()

            uiThread {
                if (reminders.isNotEmpty()) {
                    val adapter = ReminderAdapter(applicationContext, reminders)
                    list.adapter = adapter
                } else {
                    list.adapter = null
                    toast("No reminders yet")
                }
            }
        }
    }
}
