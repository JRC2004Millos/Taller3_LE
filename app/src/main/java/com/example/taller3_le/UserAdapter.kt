package com.example.taller3_le

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

class UserAdapter(private val context: Context, private val users: ArrayList<MyUser>) :
    ArrayAdapter<MyUser>(context, 0, users) {

    private val almacenamiento = Firebase.storage

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.user_adapter, parent, false)

        val user = users[position]

        val imageView: ImageView = view.findViewById(R.id.user_image)
        val textView: TextView = view.findViewById(R.id.user_name)
        val button: Button = view.findViewById(R.id.view_location_button)

        // Cargar la imagen del usuario desde la URL almacenada en la base de datos utilizando Glide
        if (user.key.isNotEmpty()) {
            Glide.with(context)
                .load(user.key + ".jpeg")
                .placeholder(R.drawable.baseline_account_box_24) // Placeholder en caso de que la URL esté vacía o no se pueda cargar la imagen
                .error(R.drawable.baseline_account_box_24) // Imagen de error en caso de que haya algún problema al cargar la imagen
                .into(imageView)
        } else {
            // Si la URL de la imagen está vacía o nula, mostrar una imagen predeterminada
            imageView.setImageResource(R.drawable.baseline_account_box_24)
        }

        textView.text = user.name

        button.setOnClickListener {
            val intent = Intent(context, LocationActivity::class.java)
            intent.putExtra("user_id", user.id)
            context.startActivity(intent)
        }

        return view
    }

    private fun downloadFile() {
        val localFile = File.createTempFile("images", "jpeg")
        val imageRef = almacenamiento.reference.child("profileImages/image.jpeg")
        imageRef.getFile(localFile)
            .addOnSuccessListener { taskSnapshot ->
// Successfully downloaded data to local file
//...
                Log.i("FBApp", "succesfully downloaded")
// Update UI using the localFile
            }.addOnFailureListener { exception ->
// Handle failed download
// ...
            }
    }
}