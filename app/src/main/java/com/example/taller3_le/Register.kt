package com.example.taller3_le

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller3_le.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding

    private val REQUEST_CAMERA_PERMISSION = 104

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 100
    }


    private val PATH_USERS = "users/"
    private val almacenamiento = Firebase.storage
    private val almacenamientoRef = almacenamiento.reference
    private var imagen: Bitmap? = null

    //private val storage = Firebase.storage
    //private val storageRef = storage.reference

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                handleImageCapture(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCamara.setOnClickListener { takePictureFromCamera() }
        binding.btnSave.setOnClickListener { register(imagen) }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val name = binding.editName.text.toString()
        if (TextUtils.isEmpty(name)) {
            binding.editName.error = "Required."
            valid = false
        } else {
            binding.editName.error = null
        }

        val lastName = binding.editLastName.text.toString()
        if (TextUtils.isEmpty(lastName)) {
            binding.editLastName.error = "Required."
            valid = false
        } else {
            binding.editLastName.error = null
        }

        val email = binding.editEmail.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.editEmail.error = "Required."
            valid = false
        } else {
            binding.editEmail.error = null
        }

        val password = binding.editPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.editPassword.error = "Required."
            valid = false
        } else {
            binding.editPassword.error = null
        }

        val id = binding.editId.text.toString()
        if (TextUtils.isEmpty(id)) {
            binding.editId.error = "Required."
            valid = false
        } else {
            binding.editId.error = null
        }

        return valid
    }


    private fun isEmailValid(email: String): Boolean {
        if (!email.contains("@") ||
            !email.contains(".") ||
            email.length < 5) {
            Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun register(imagenLista: Bitmap?) {
        val database = Firebase.database
        if (validateForm() && isEmailValid(binding.editEmail.text.toString())) {
            auth = Firebase.auth
            auth.createUserWithEmailAndPassword(binding.editEmail.text.toString(), binding.editPassword.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)
                        val myUser = MyUser()
                        myUser.name = binding.editName.text.toString()
                        myUser.lastName = binding.editLastName.text.toString()
                        myUser.email = binding.editEmail.text.toString()
                        myUser.id = binding.editId.text.toString().toInt()
                        myUser.latitude = getCurrentLatitude()
                        myUser.longitude = getCurrentLongitude()
                        val usuarioRef = database.getReference(PATH_USERS + auth.currentUser!!.uid)
                        myUser.key = auth.currentUser!!.uid
                        usuarioRef.setValue(myUser)
                        val currentUserUid = auth.currentUser?.uid
                        saveImageToFirebase(imagenLista!!)
                        // Inicia la siguiente actividad y envía la UID del usuario actual como extra
                        val intent = Intent(this, MapsActivity::class.java)
                        intent.putExtra("current_user_uid", currentUserUid)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this, "createUserWithEmail:Failure: " + task.exception.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        task.exception?.message?.let { Log.e(TAG, it) }
                    }
                }
        }
        else{
            binding.editEmail.setText("")
            binding.editPassword.setText("")
        }
    }

    // Método para obtener la latitud actual del dispositivo
    private fun getCurrentLatitude(): Double? {
        var latitude: Double? = null
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
            latitude = it.latitude
        }
        return latitude
    }

    // Método para obtener la longitud actual del dispositivo
    private fun getCurrentLongitude(): Double? {
        var longitude: Double? = null
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
            longitude = it.longitude
        }
        return longitude
    }
    private fun takePictureFromCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureLauncher.launch(cameraIntent)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }
    private fun handleImageCapture(imageBitmap: Bitmap) {

        val imageView = findViewById<ImageView>(R.id.btnCamara)
        imageView.setImageBitmap(imageBitmap)
        imagen = imageBitmap

    }
    private fun saveImageToFirebase(imageBitmap: Bitmap) {
        val profileImageRef = almacenamientoRef.child("profileImages/${auth.currentUser!!.uid}")

        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = profileImageRef.putBytes(data)
        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            Toast.makeText(this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePictureFromCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

}