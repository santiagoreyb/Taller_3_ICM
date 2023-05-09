package com.example.taller_3

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller_3.databinding.ActivityMapsPorPersonaBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlin.math.*

class MapsActivityPorPersona : AppCompatActivity(), OnMapReadyCallback {

    private var initialLatitude: String? = null
    private var initialLongitude: String? = null
    private var firstLatitude: String? = null
    private var firstLongitude: String? = null
    private var elevation: String? = null
    private var latitude: String? = null
    private var longitude: String? = null
    private var uid: String? = null
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsPorPersonaBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    val PATH_USERS = "users/"
    private val RADIUS_OF_EARTH_KM: Float = 6371.01F
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    private lateinit var myRef_02: DatabaseReference
    private lateinit var auth: FirebaseAuth
    var previousMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityMapsPorPersonaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        myRef = database.getReference(PATH_USERS)
        myRef_02 = database.getReference(PATH_USERS + auth.currentUser!!.uid)
        var distanceDifference: Double?
        // Obtain the SupportMapFragment and ge notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mLocationRequest = createLocationRequest()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                uid = intent.getStringExtra("uid")
                obtenerPersona(uid.toString())
                mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        this@MapsActivityPorPersona,
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

    private fun obtenerPersona(uid: String) {
        var distanceDifference: Double?
        Log.d("MiTag2", "La ubicación actual es: $uid")
        myRef.orderByChild("uid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Aquí puedes obtener los datos de la persona correspondiente al uid
                    for (snapshot in dataSnapshot.children) {
                        val persona = snapshot.getValue(Persona::class.java)!!

                        val actualLocationP = LatLng(persona.latitud, persona.longitud)
                        distanceDifference = latitude.let {
                            longitude.let { it1 ->
                                persona.latitud.let { it2 ->
                                    persona.longitud.let { it3 ->
                                        it?.let { it4 ->
                                            it1?.let { it5 ->
                                                distance(
                                                    it4.toDouble(),
                                                    it5.toDouble(), it2, it3
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        newDistanceForInteraction(persona.latitud, persona.longitud)

                        val actualMarker = mMap.addMarker(
                            MarkerOptions().position(actualLocationP)
                                .title("Usuario ${persona.nombre}")
                        )

                        // Eliminar el marcador anterior, si existe
                        previousMarker?.remove()

                        // Guardar el nuevo marcador como el anterior
                        previousMarker = actualMarker

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar errores
                }
            })

    }

    private fun requestLocationFunction() {
        uid = intent.getStringExtra("uid")
        obtenerPersona(uid.toString())

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
            uid?.let { obtenerPersona(it) }

        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)

    }

    private fun newDistanceForInteraction(newLat: Double, newLng: Double) {
        var newDistance: Double? = 0.0
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
        val distanciaTextView = findViewById<TextView>(R.id.distancia)
        distanciaTextView.setText("La nueva distancia es: " + newDistance.toString())

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

}
