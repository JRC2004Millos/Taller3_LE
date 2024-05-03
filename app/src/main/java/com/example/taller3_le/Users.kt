package com.example.taller3_le

import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class Users : AppCompatActivity() {

    private lateinit var adapter: UserAdapter
    private val PATH_USERS = "users/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        val listView = findViewById<ListView>(R.id.usersList)
        adapter = UserAdapter(this, ArrayList())
        listView.adapter = adapter

        // Mostrar usuarios disponibles al inicio
        showUsers { usersList ->
            adapter.addAll(usersList)
        }

        // Escuchar cambios en la base de datos
        listenForUserChanges()
    }

    private fun showUsers(listener: (ArrayList<MyUser>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference(PATH_USERS)

        val usersList = ArrayList<MyUser>()

        // Consulta para obtener usuarios con estado en true
        usersRef.orderByChild("estado").equalTo(true).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val user = snapshot.getValue(MyUser::class.java)
                    if (user != null) {
                        usersList.add(user)
                    }
                }
                listener.invoke(usersList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar errores de Firebase Database si es necesario
                listener.invoke(ArrayList()) // Invocar el listener con una lista vacía en caso de error
            }
        })
    }

    private fun listenForUserChanges() {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference(PATH_USERS)

        // Listener para cambios en la lista de usuarios
        usersRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // No necesitamos hacer nada aquí, ya que el estado de los usuarios no cambió
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Obtener el estado anterior y el nuevo estado del usuario
                val previousUser = snapshot.getValue(MyUser::class.java)
                val newUser = snapshot.getValue(MyUser::class.java)

                // Verificar si el usuario es diferente antes y después del cambio
                if (previousUser != null && newUser != null && previousUser.estado != newUser.estado) {
                    // Verificar si el usuario pasó de no disponible a disponible
                    if (!previousUser.estado && newUser.estado) {
                        // Cuando un usuario cambia su estado a disponible, mostramos el Toast
                        val userName = newUser.name
                        showToast("¡$userName se ha conectado!")
                    }
                }

                // Actualizar la lista de usuarios en cualquier caso
                showUsers { usersList ->
                    adapter.clear()
                    adapter.addAll(usersList)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // No necesitamos hacer nada aquí, ya que los usuarios no se eliminan
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // No necesitamos hacer nada aquí, ya que los usuarios no se mueven
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores si es necesario
            }
        })
    }

    private fun showToast(message: String) {
        Log.d("UserStatus", "Mostrando Toast: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
