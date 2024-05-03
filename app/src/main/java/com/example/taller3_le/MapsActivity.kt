package com.example.taller3_le

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
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

class MapsActivity : AppCompatActivity(), SensorEventListener {
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private var lastLocation: GeoPoint = GeoPoint(latitud, longitud)
    private lateinit var lastLocation1: MyLocationNewOverlay
    var check: Boolean = false
    private lateinit var map: MapView
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
        setContentView(R.layout.activity_maps)

        currentUserUid = intent.getStringExtra("current_user_uid")!!

        checkLocationPermission()

        Configuration.getInstance().setUserAgentValue(BuildConfig.BUILD_TYPE)
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        setUpSensorStuff()

        val jsonString = readJsonFromFile("locations.json")
        if (jsonString != null) {
            readLocationsFromJson(jsonString)
        } else {
            Log.e("MapsActivity", "Error al leer el archivo JSON")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_taller, menu)

        // Obtener el ítem de menú "menuStatus"
        val menuStatus = menu.findItem(R.id.menuStatus)

        // Llamar a getStatus para obtener el estado actual
        getStatus { isOnline ->
            // Cambiar el ícono del elemento de menú según el estado obtenido
            if (isOnline) {
                menuStatus.setIcon(R.drawable.online)
            } else {
                menuStatus.setIcon(R.drawable.offline)
            }
        }

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
                Log.e(TAG, "Error al obtener el estado del usuario: ${error.message}")
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
                    Log.i(TAG, "Encontró usuario: " + currentUser?.name)
                    Log.i(TAG, "UID usuario actual: $currentUserUid")

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
                Log.e(TAG, "Error al obtener el estado del usuario: ${error.message}")
                listener.invoke(false) // Invocar el listener con un estado predeterminado en caso de error
            }
        })
    }


    private fun readJsonFromFile(fileName: String): String? {
        return try {
            val inputStream = assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun readLocationsFromJson(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val locationsObject = jsonObject.getJSONObject("locations")

            // Leer los objetos individuales dentro del objeto "locations"
            for (i in 0 until locationsObject.length()) {
                val locationObject = locationsObject.getJSONObject(i.toString())
                val latitude = locationObject.getDouble("latitude")
                val longitude = locationObject.getDouble("longitude")
                val name = locationObject.getString("name")

                // Agregar marcador al mapa
                val marker = Marker(map)
                marker.position = GeoPoint(latitude, longitude)
                marker.title = name
                map.overlays.add(marker)
            }

            // Leer los objetos dentro del array "locationsArray"
            val locationsArray = jsonObject.getJSONArray("locationsArray")
            for (i in 0 until locationsArray.length()) {
                val locationObject = locationsArray.getJSONObject(i)
                val latitude = locationObject.getDouble("latitude")
                val longitude = locationObject.getDouble("longitude")
                val name = locationObject.getString("name")

                // Agregar marcador al mapa
                val marker = Marker(map)
                marker.position = GeoPoint(latitude, longitude)
                marker.title = name
                map.overlays.add(marker)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
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
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        val mapController = map.controller
        setupLocationOverlay(mapController)
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

    private fun setupLocationOverlay(mapController: IMapController) {
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
                    if (!check) {
                        check = true
                        lastLocation = GeoPoint(latitude, longitude)
                        lastLocation1 = locationOverlay
                        map.overlays.add(locationOverlay)
                        mapController.setZoom(15.0)
                        mapController.setCenter(locationOverlay.myLocation)
                        marker.position = lastLocation
                        marker.title = "Mi Ubicación"
                        map.overlays.add(marker)
                    } else {
                        lastLocation = GeoPoint(latitude, longitude)
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
        map.overlays.add(locationOverlay)
    }

    private fun setUpSensorStuff() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}