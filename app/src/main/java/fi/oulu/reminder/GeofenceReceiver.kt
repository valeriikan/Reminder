package fi.oulu.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

class GeofenceReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        val geofenceTransition = geofencingEvent.geofenceTransition

        if(geofenceTransition==Geofence.GEOFENCE_TRANSITION_ENTER ||  geofenceTransition==Geofence.GEOFENCE_TRANSITION_ENTER ){
            // Retrieve data from intent
            val uid = intent!!.getIntExtra("uid", 0)
            val text = intent.getStringExtra("message")

            MainActivity.ShowNofitication(context!!,text!!)

            // Remove reminder from db after it was shown
            doAsync {
                val db = Room.databaseBuilder(context, AppDatabase::class.java,"reminders")
                    .build()
                db.reminderDao().delete(uid)
                db.close()
            }

            //Remove geofence
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            MapActivity.removeGeoFences(context, triggeringGeofences)

        }


    }
}