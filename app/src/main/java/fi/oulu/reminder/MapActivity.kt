package fi.oulu.reminder

import android.Manifest
import android.content.pm.PackageManager
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


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var gMap: GoogleMap
    lateinit var fusedLocationClient: FusedLocationProviderClient

    val LOCATION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        (map_fragment as SupportMapFragment).getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        map_create.setOnClickListener {

            // dummy
            val reminder = Reminder(
                uid = null,
                time = null,
                location = "65.059640\n25.466246",
                message = "test"
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

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

                if (location != null) {
                    with(gMap) {
                        val latLong = LatLng(location.latitude, location.longitude)
                        animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 13f))
                        //addMarker(MarkerOptions().position(latLong))
                    }
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
