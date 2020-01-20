package fi.oulu.reminder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_map.*
import org.jetbrains.anko.doAsync

class MapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // TODO: map stuff
        map_create.setOnClickListener {

            // dummy
            val reminder = Reminder(
                uid = null,
                type = "location",
                trigger = "65.059640\n25.466246",
                message = "test"
            )

            doAsync {
                val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java,"reminders")
                    .build()

                db.reminderDao().insert(reminder)
                db.close()
            }

            finish()
        }
    }
}
