package fi.oulu.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_map.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.util.*


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var gMap: GoogleMap
    lateinit var fusedLocationClient: FusedLocationProviderClient

    var selectedLocation: LatLng? = null
    var selectedLocationAddress = ""
    val LOCATION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        (map_fragment as SupportMapFragment).getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        map_create.setOnClickListener {


            if (selectedLocation == null){

                toast("Select a location ")
                return@setOnClickListener
            }

            if (reminder_message.text.toString().isEmpty()){

                toast("Enter reminder message")
                return@setOnClickListener

            }
            var message = reminder_message.text.toString()

            message=String.format("%s @ %s",message,selectedLocationAddress)

            val reminder = Reminder(
                uid = null,
                time = null,
                location = String.format("%.3f,%.3f", selectedLocation?.latitude,selectedLocation?.longitude ) ,
                message = message
            )

            doAsync {
                val db =
                    Room.databaseBuilder(applicationContext, AppDatabase::class.java, "reminders")
                        .build()

                db.reminderDao().insert(reminder)
                db.close()
            }

            finish()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        gMap = map ?: return

        if (!isLocationPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                LOCATION_REQUEST_CODE
            )
        } else {
            gMap.isMyLocationEnabled = true


            //Zoom to last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    with(gMap) {
                        val latLong = LatLng(location.latitude, location.longitude)
                        animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 13f))
                        //addMarker(MarkerOptions().position(latLong))
                    }
                }

            }

            // Add marker on map after click

            gMap.setOnMapClickListener { location: LatLng ->

                with(gMap) {
                    clear()
                    val latLong = LatLng(location.latitude, location.longitude)
                    animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 13f))

                    val geocoder = Geocoder(applicationContext, Locale.getDefault())
                    var title = ""
                    var city=""
                    try {

                        val addresses =
                            geocoder.getFromLocation(latLong.latitude, latLong.longitude, 1)
                        val address = addresses.get(0).getAddressLine(0)
                        city=addresses.get(0).locality.toUpperCase()
                        title = address.toString()
                    } catch (e: Exception) {
                    }

                    val marker = addMarker(MarkerOptions().position(latLong).snippet(title).title(city))
                    marker.showInfoWindow()

                    selectedLocation = latLong
                    selectedLocationAddress= String.format("%s (%s)",city,title)

                }

            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (permissions.size == 1 &&
                permissions[0] == Manifest.permission.ACCESS_COARSE_LOCATION &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                gMap.setMyLocationEnabled(true);

            } else if (permissions.size > 1 &&
                (permissions[0] == Manifest.permission.ACCESS_COARSE_LOCATION &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) ||
                (permissions[1] == Manifest.permission.ACCESS_FINE_LOCATION &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                gMap.setMyLocationEnabled(true);
            } else {
                val toast = Toast.makeText(
                    this,
                    "App needs location permision to function",
                    Toast.LENGTH_LONG
                )
                toast.show()
            }
        }
    }


    fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

}
