package com.example.taller_3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.taller_3.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import java.io.*
import java.util.*
import kotlin.math.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var initialLatitude: String? = null
    private var initialLongitude: String? = null
    private var firstLatitude: String? = null
    private var firstLongitude: String? = null
    private var elevation : String? = null
    private var latitude : String? = null
    private var longitude : String? = null

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    val PATH_USERS="users/"
    private val RADIUS_OF_EARTH_KM : Float = 6371.01F
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    private lateinit var myRef02: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val personasNotificadas = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val jsonArray = readJSONArrayFromFile()
        var distanceDifference: Double?
        Log.i("JSON", jsonArray.toString())
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        myRef = database.getReference(PATH_USERS+auth.currentUser!!.uid)
        myRef02 = database.getReference(PATH_USERS)
        // agregarListenerDeEventos()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mLocationRequest = createLocationRequest()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        this@MapsActivity,
                        R.raw.style_json
                    )
                )

                val location = locationResult.lastLocation
                // Log.i("LOCATION", "Location update in the callback: $location")
                if (location != null) {
                    elevation = location.altitude.toString()
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()
                }
                distanceDifference = initialLatitude?.let {
                    initialLongitude?.let { it1 ->
                        latitude?.let { it2 ->
                            longitude?.let { it3 ->
                                distance(
                                    it.toDouble(),
                                    it1.toDouble(), it2.toDouble(), it3.toDouble()
                                )
                            }
                        }
                    }
                }
                distanceDifference = distanceDifference?.times(1000)

                if (distanceDifference!! >= 100.0) {
                    mMap.clear()
                    val actualLocation = latitude?.let {
                        location?.let { it1 ->
                            LatLng(
                                it.toDouble(),
                                it1.longitude
                            )
                        }
                    }

                    // Log.i("Location",nameAddressLocation)
                    actualLocation?.let {
                        MarkerOptions().position(it).title("Actual location")
                    }
                        ?.let { mMap.addMarker(it) }
                    actualLocation?.let { CameraUpdateFactory.newLatLngZoom(it, 15F) }
                        ?.let { mMap.moveCamera(it) }
                    initialLongitude = longitude
                    initialLatitude = latitude
                    distanceDifference = 0.0
            }
                if (location != null) {
                    myRef.child("latitud").setValue(location.latitude)
                    myRef.child("longitud").setValue(location.longitude)
                }
                for (i in 0 until jsonArray.length()) {
                    val locationObject = jsonArray.getJSONObject(i)
                    val latitude = locationObject.getDouble("latitude")
                    val longitude = locationObject.getDouble("longitude")
                    val name = locationObject.getString("name")
                    val latLng = LatLng(latitude, longitude)
                    mMap.addMarker(MarkerOptions().position(latLng).title(name))
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near PUJ.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        requestLocationFunction()
    }

    private fun requestLocationFunction ( ) {
        if (!verificarUbicacionHabilitada()) {
            return
        }
        var isFirstLocationFound = false
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // # Add a marker in Colombia and move the camera
            if (!isFirstLocationFound) {
                firstLatitude = location.latitude.toString()
                firstLongitude = location.longitude.toString()
                isFirstLocationFound = true
            }
            initialLatitude = location.latitude.toString()
            initialLongitude = location.longitude.toString()
            latitude = initialLatitude
            longitude = initialLongitude
            val actualLocation = LatLng(location.latitude, location.longitude)

            mMap.addMarker(MarkerOptions().position(actualLocation).title("Actual Location"))
            // mMap.moveCamera(CameraUpdateFactory.newLatLng(actualLocation))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(actualLocation, 15F))
            // mMap.moveCamera(CameraUpdateFactory.zoomTo(15F))
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)

    }

    private fun newDistanceForInteraction ( newLat:Double , newLng:Double ) {
        var newDistance:Double? = 0.0
        val baseLatitudeForDistance = latitude?.toDouble()
        val baseLongitudeForDistance = longitude?.toDouble()
        newDistance = baseLatitudeForDistance?.let {
            baseLongitudeForDistance?.let { it1 ->
                newLat?.let { it2 ->
                    newLng?.let { it3 ->
                        distance(
                            it, it1, it2, it3
                        )
                    }
                }
            }
        }
        newDistance = newDistance?.times(1000)
        // Log.i("New Distance",newDistance)
        Toast.makeText(this,"La nueva distancia es: $newDistance.",Toast.LENGTH_LONG).show()
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create()
            .setInterval(10000)
            .setFastestInterval(5000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lngDistance / 2) * sin(lngDistance / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c
        return (result * 100.0).roundToInt() / 100.0
    }


    private fun verificarUbicacionHabilitada(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // La ubicación no está habilitada
            AlertDialog.Builder(this)
                .setMessage("Para mostrar la ubicación del usuario, necesitamos que habilite la ubicación del dispositivo")
                .setPositiveButton("Habilitar ubicación") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancelar") { _, _ -> }
                .show()
            return false
        }
        return true
    }

    private fun readJSONArrayFromFile (): JSONArray {
        val inputStream = resources.openRawResource(R.raw.locations)
        val reader = InputStreamReader(inputStream)
        val jsonString = StringBuilder()
        var data = reader.read()
        while (data != -1) {
            jsonString.append(data.toChar())
            data = reader.read()
        }
        return JSONArray(jsonString.toString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_logout -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                true
            }
            R.id.menu_disponible -> {
                myRef.child("disponible").setValue(true)
                true
            }
            R.id.menu_desconectado -> {
                myRef.child("disponible").setValue(false)
                myRef.child("toastMostrado").setValue(false)
                //personasNotificadas.remove(myRef.key)
                true
            }
            R.id.menu_usuarios_disponibles -> {
                val intent = Intent(this, ListaUsuariosActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}

