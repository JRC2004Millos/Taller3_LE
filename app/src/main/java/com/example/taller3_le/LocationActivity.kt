package com.example.taller3_le

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.health.connect.datatypes.ExerciseRoute
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.library.BuildConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.util.Date
import kotlin.math.roundToInt

class LocationActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var map: MapView
    private var latitud2: Double = 4.660557
    private var longitud2: Double = -74.090749
    private var lastLocation: GeoPoint = GeoPoint(latitud2, longitud2)
    private lateinit var lastLocation1: MyLocationNewOverlay
    var check: Boolean = false
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var auth: FirebaseAuth
    private val PATH_USERS = "users/"
    private lateinit var currentUserUid: String

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        currentUserUid = intent.getStringExtra("current_user_uid")!!

        checkLocationPermission()

        Configuration.getInstance().setUserAgentValue(BuildConfig.BUILD_TYPE)
        map = findViewById(R.id.map2)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        setUpSensorStuff()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_taller, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        getStatus { isOnline ->
            // Aquí puedes manejar el estado obtenido
            if (isOnline) {
                // El estado es verdadero
                item.setIcon(R.drawable.online)
                println("El usuario está activo")
            } else {
                // El estado es falso
                item.setIcon(R.drawable.offline)
                println("El usuario está inactivo")
            }
        }
        // Handle item selection
        return when (item.itemId) {
            R.id.menuLogOut -> {
                auth = Firebase.auth
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                true
            }
            R.id.menuStatus -> {

                // Obtener la UID del usuario actualmente autenticado
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

                // Llamada a getStatus() con la UID del usuario actual y un listener para obtener el estado
                if (currentUserUid != null) {
                    getStatusAndModify { isOnline ->
                        // Aquí puedes manejar el estado obtenido
                        if (isOnline) {
                            // El estado es verdadero
                            item.setIcon(R.drawable.offline)
                            println("El usuario está activo")
                        } else {
                            // El estado es falso
                            item.setIcon(R.drawable.online)
                            println("El usuario está inactivo")
                        }
                    }
                }
                else{
                    Toast.makeText(this, "No se encontró el usuario", Toast.LENGTH_SHORT).show()
                }


                true
            }
            R.id.menuUsers -> {
                startActivity(Intent(this, Users::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun getStatus(listener: (Boolean) -> Unit) {
        val database = Firebase.database
        val myRef = database.getReference(PATH_USERS)

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var status = false // Valor predeterminado del estado

                // Iterar sobre los hijos (en caso de múltiples usuarios)
                for (singleSnapshot in dataSnapshot.children) {
                    val currentUser = singleSnapshot.getValue(MyUser::class.java)

                    // Verificar si es el usuario actual
                    if (singleSnapshot.key == currentUserUid) {
                        // Si la clave coincide con la UID del usuario actual, obtenemos su estado
                        status = currentUser?.estado ?: false
                        break // No es necesario continuar buscando una vez que se encuentra el usuario
                    }
                }

                // Llamar al listener con el estado obtenido
                listener.invoke(status)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores si es necesario
                Log.e(ContentValues.TAG, "Error al obtener el estado del usuario: ${error.message}")
                listener.invoke(false) // Invocar el listener con un estado predeterminado en caso de error
            }
        })
    }


    private fun getStatusAndModify(listener: (Boolean) -> Unit) {
        val database = Firebase.database
        val myRef = database.getReference(PATH_USERS)

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var status = false // Valor predeterminado del estado

                // Iterar sobre los hijos (en caso de múltiples usuarios)
                for (singleSnapshot in dataSnapshot.children) {
                    val currentUser = singleSnapshot.getValue(MyUser::class.java)
                    Log.i(ContentValues.TAG, "Encontró usuario: " + currentUser?.name)
                    Log.i(ContentValues.TAG, "UID usuario actual: $currentUserUid")

                    // Verificar si es el usuario actual
                    if (singleSnapshot.key == currentUserUid) {
                        // Si la clave coincide con la UID del usuario actual, obtenemos su estado
                        status = currentUser?.estado ?: false
                        // Cambiar el estado en la base de datos
                        singleSnapshot.ref.child("estado").setValue(!status)
                        break // No es necesario continuar buscando una vez que se encuentra el usuario
                    }
                }

                // Llamar al listener con el estado obtenido
                listener.invoke(status)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores si es necesario
                Log.e(ContentValues.TAG, "Error al obtener el estado del usuario: ${error.message}")
                listener.invoke(false) // Invocar el listener con un estado predeterminado en caso de error
            }
        })
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido
                } else {
                    // Permiso denegado
                    Toast.makeText(this, "Permiso de ubicación necesario para continuar.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Otros casos
            }
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        val mapController = map.controller
        setRoute(mapController)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                val lux = event.values[0]
                if (lux < 1000) {
                    map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                } else {
                    map.overlayManager.tilesOverlay.setColorFilter(null)
                }
            }
        }
    }

    private fun setRoute(mapController: IMapController) {
        map.overlays.clear()

        val endPoint = GeoPoint(latitud2, longitud2)
        val locationProvider = GpsMyLocationProvider(this)
        locationOverlay = MyLocationNewOverlay(locationProvider, map)
        locationOverlay.enableMyLocation()

        locationOverlay.runOnFirstFix {
            val currentLocation = locationOverlay.myLocation
            val latitude = currentLocation.latitude
            val longitude = currentLocation.longitude
            val marker = Marker(map)

            runOnUiThread {
                mapController.setCenter(locationOverlay.myLocation)
                if (locationOverlay.myLocation != null) {
                    val startPoint = GeoPoint(latitude, longitude)
                    if (!check) {
                        check = true
                        lastLocation = startPoint
                        lastLocation1 = locationOverlay
                        map.overlays.add(locationOverlay)
                        mapController.setZoom(15.0)
                        mapController.setCenter(locationOverlay.myLocation)
                        marker.position = lastLocation
                        marker.title = "Mi Ubicación"
                        map.overlays.add(marker)

                        // Create a Polyline
                        val line = Polyline()
                        line.addPoint(lastLocation) // Use lastLocation as startPoint
                        line.addPoint(endPoint)
                        line.color = Color.RED
                        map.overlays.add(line)

                        map.invalidate() // Refresh the map
                    } else {
                        lastLocation = startPoint
                        lastLocation1 = locationOverlay
                        map.overlays.add(locationOverlay)
                        mapController.setZoom(15.0)
                        mapController.setCenter(locationOverlay.myLocation)
                        marker.position = lastLocation
                        marker.title = "Mi Ubicación"
                        map.overlays.add(marker)
                    }
                }
            }
        }

        // Add end marker
        val endMarker = Marker(map)
        endMarker.position = endPoint
        endMarker.title = "Bryan"
        map.overlays.add(endMarker)
    }

    private fun setUpSensorStuff() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}