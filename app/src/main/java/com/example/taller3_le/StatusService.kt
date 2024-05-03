package com.example.taller3_le

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StatusService : Service() {

    private lateinit var database: FirebaseDatabase
    private lateinit var userRef: DatabaseReference
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance()
        userRef = database.getReference("users")

        // Listener para escuchar cambios en la lista de usuarios disponibles
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Recorrer todos los usuarios
                dataSnapshot.children.forEach { userSnapshot ->
                    val user = userSnapshot.getValue(MyUser::class.java)
                    if (user != null && user.estado) {
                        // Usuario disponible, mostrar Toast
                        showToast("¡${user.name} se ha conectado!")
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar errores si es necesario
            }
        }

        // Agregar el listener a la referencia de la base de datos
        userRef.addValueEventListener(valueEventListener)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eliminar el listener al destruir el servicio
        userRef.removeEventListener(valueEventListener)
    }

    private fun showToast(message: String) {
        // Mostrar Toast en el hilo principal
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}