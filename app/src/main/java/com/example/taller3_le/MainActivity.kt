package com.example.taller3_le

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        login()
        register()
    }

    /*override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("user", currentUser.email)
            startActivity(intent)
        } else {
            email.setText("")
            password.setText("")
        }
    }*/

    private fun validateForm(): Boolean {
        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        var valid = true
        val txtEmail = email.text.toString()
        if (TextUtils.isEmpty(txtEmail)) {
            email.error = "Required."
            valid = false
        } else {
            email.error = null
        }
        val txtPassword = password.text.toString()
        if (TextUtils.isEmpty(txtPassword)) {
            password.error = "Required."
            valid = false
        } else {
            password.error = null
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

    private fun login(){
        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            if(validateForm() && isEmailValid(email.text.toString())){
                auth = Firebase.auth

                auth.signInWithEmailAndPassword(
                    email.text.toString(),
                    password.text.toString()
                )
                    .addOnCompleteListener(this) { task ->
                        // Sign in task
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful)
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful) {
                            Log.w(TAG, "signInWithEmail:failure", task.exception)
                            Toast.makeText(
                                this, "Authentication failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                            email.setText("")
                            password.setText("")
                        }
                        else{
                            Toast.makeText(this, "Authentication done.",Toast.LENGTH_SHORT).show()
                            val currentUserUid = auth.currentUser?.uid

                            // Inicia la siguiente actividad y envía la UID del usuario actual como extra
                            val intent = Intent(this, MapsActivity::class.java)
                            intent.putExtra("current_user_uid", currentUserUid)
                            startActivity(intent)
                        }
                    }
            }
            else{
                email.setText("")
                password.setText("")
            }
        }
    }

    private fun register(){
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener { startActivity(Intent(this, Register::class.java)) }
    }
}