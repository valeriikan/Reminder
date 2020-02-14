package fi.oulu.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_map.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.w3c.dom.Text
import java.util.*


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var gMap: GoogleMap
    lateinit var fusedLocationClient: FusedLocationProviderClient
    val LOCATION_REQUEST_CODE = 123
    val CAMERA_ZOOM_LEVEL = 14f

    lateinit var geofencingClient: GeofencingClient
    val GEOFENCE_RADIUS = 500.0
    val GEOFENCE_ID = "REMINDER_GEOFENCE_ID"
    val GEOFENCE_EXPIRATION = 180 * 24 * 60 * 60 * 1000
    val GEOFENCE_DWELL_DELAY = 2 * 60 * 1000
    val GEOFENCE_LOCATION_REQUEST_CODE = 12345
    lateinit var geoFenceReminder: Reminder


    var selectedLocation: LatLng? = null
    var selectedLocationAddress = ""


    var autoCompleteList = arrayListOf<String>("")
    lateinit var autoCompletAdaptor: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        (map_fragment as SupportMapFragment).getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        map_create.setOnClickListener {


            if (selectedLocation == null) {

                toast("Select a location ")
                return@setOnClickListener
            }

            if (reminder_message.text.toString().isEmpty()) {

                toast("Enter reminder message")
                return@setOnClickListener

            }
            var message = reminder_message.text.toString()

            message = String.format("%s @ %s", message, selectedLocationAddress)

            val reminder = Reminder(
                uid = null, time = null, location = String.format(
                    "%.3f,%.3f", selectedLocation?.latitude, selectedLocation?.longitude
                ), message = message
            )



            doAsync {
                val db = Room.databaseBuilder(
                    applicationContext, AppDatabase::class.java, "reminders"
                ).build()

                val uuid = db.reminderDao().insert(reminder).toInt()
                db.close()
                reminder.uid = uuid
                geoFenceReminder = reminder
                createGeoFence(selectedLocation!!, geoFenceReminder, geofencingClient)
            }

            finish()
        }



        autoCompletAdaptor = ArrayAdapter<String>(
            this, android.R.layout.simple_dropdown_item_1line, autoCompleteList
        )

        searchAutoComplete.setAdapter(autoCompletAdaptor)



        searchMap.setOnClickListener {

            val geocoder = Geocoder(applicationContext, Locale.getDefault())

            try {
                val searchText = searchAutoComplete.text.toString()
                val addresses = geocoder.getFromLocationName(searchText, 1)
                val address = addresses.get(0).getAddressLine(0)

                val lat = addresses.get(0).latitude
                val long = addresses.get(0).longitude

                selectedLocation = LatLng(lat, long)
                selectedLocationAddress = String.format("%s (%s)", searchText, address)

                with(gMap) {
                    clear()
                    animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            selectedLocation, CAMERA_ZOOM_LEVEL
                        )
                    )
                    val marker = addMarker(
                        MarkerOptions().position(
                            LatLng(
                                lat, long
                            )
                        ).snippet(address).title(searchText)
                    )
                    marker.showInfoWindow()
                    // Instantiates a new CircleOptions object and defines the center and radius

                    addCircle(
                        CircleOptions().center(LatLng(lat, long)).strokeColor(
                            Color.argb(
                                50, 70, 70, 70
                            )
                        ).fillColor(Color.argb(70, 150, 150, 150)).radius(GEOFENCE_RADIUS)
                    )

                }


                autoCompleteList.add(String.format("%s (%s)", searchText, address))
                autoCompletAdaptor.clear()
                autoCompletAdaptor.addAll(autoCompleteList)
                autoCompletAdaptor.notifyDataSetChanged()
            } catch (e: Exception) {
            }
        }


    }

    private fun createGeoFence(selectedLocation: LatLng, reminder: Reminder, geofencingClient: GeofencingClient) {
        val geofence = Geofence.Builder().setRequestId(GEOFENCE_ID).setCircularRegion(
            selectedLocation.latitude, selectedLocation.longitude, GEOFENCE_RADIUS.toFloat()
        ).setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(GEOFENCE_DWELL_DELAY).build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(geofence)
            .build()

        val intent = Intent(this, GeofenceReceiver::class.java).putExtra("uid", reminder.uid)
            .putExtra("location", reminder.location).putExtra("message", reminder.message)

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ), GEOFENCE_LOCATION_REQUEST_CODE
                )
            } else {
                geofencingClient.addGeofences(geofenceRequest, pendingIntent)

            }

        } else {
            geofencingClient.addGeofences(geofenceRequest, pendingIntent)

        }

    }

    override fun onMapReady(map: GoogleMap?) {
        gMap = map ?: return


        if (!isLocationPermissionGranted()) {
            val permissions = mutableListOf<String>()
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), LOCATION_REQUEST_CODE
            )
        } else {
            gMap.isMyLocationEnabled = true


            //Zoom to last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    with(gMap) {
                        val latLong = LatLng(location.latitude, location.longitude)
                        animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, CAMERA_ZOOM_LEVEL))
                        //addMarker(MarkerOptions().position(latLong))
                    }
                }

            }

            // Add marker on map after click
            gMap.setOnMapClickListener { location: LatLng ->

                with(gMap) {
                    clear()
                    val latLong = LatLng(location.latitude, location.longitude)
                    animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, CAMERA_ZOOM_LEVEL))

                    val geocoder = Geocoder(applicationContext, Locale.getDefault())
                    var title = ""
                    var city = ""
                    try {

                        val addresses = geocoder.getFromLocation(
                            latLong.latitude, latLong.longitude, 1
                        )
                        val address = addresses.get(0).getAddressLine(0)
                        city = addresses.get(0).locality.toUpperCase()
                        title = address.toString()
                    } catch (e: Exception) {
                    }

                    val marker = addMarker(
                        MarkerOptions().position(latLong).snippet(title).title(
                            city
                        )
                    )
                    marker.showInfoWindow()

                    addCircle(
                        CircleOptions().center(latLong).strokeColor(
                            Color.argb(
                                50, 70, 70, 70
                            )
                        ).fillColor(Color.argb(100, 150, 150, 150)).radius(GEOFENCE_RADIUS)
                    )

                    selectedLocation = latLong
                    selectedLocationAddress = String.format("%s (%s)", city, title)

                }


            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == GEOFENCE_LOCATION_REQUEST_CODE) {
            if (permissions.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                toast("Reminder Application needs background location to work on Android 10 or higher")
            }
        }

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                gMap.isMyLocationEnabled = true;
                onMapReady(gMap)
            } else {
                val toast = Toast.makeText(
                    this, "The App needs location permision to function", Toast.LENGTH_LONG
                )
                toast.show()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (grantResults.isNotEmpty() && grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                    toast("Reminder Application needs background location to work on Android 10 or higher")
                }
            }

        }
    }


    fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }



    companion object {
        fun removeGeoFences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            var geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }
    }

}
