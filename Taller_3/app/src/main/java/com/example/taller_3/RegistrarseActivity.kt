package com.example.taller_3


import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller_3.databinding.ActivityRegistrarseBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.sql.Types.NULL


class RegistrarseActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding:ActivityRegistrarseBinding
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    private lateinit var imagen: ImageView
    val REQUEST_CODE_LOCATION_PERMISSION = 1
    private val REQUEST_CODE_CAMERA_PERMISSION = 2
    private val REQUEST_CODE_IMAGE_CAPTURE = 3
    private val REQUEST_CODE_IMAGE_PICKER = 4
    private val REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 5
    private val storage = Firebase.storage

    val PATH_USERS="users/"

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrarse)
        binding = ActivityRegistrarseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imagen = findViewById<ImageView>(R.id.perfil)
        auth = Firebase.auth
        myRef = FirebaseDatabase.getInstance().reference
        binding.perfil.setOnClickListener {
            val options = arrayOf<CharSequence>("Tomar foto", "Seleccionar de la galería", "Cancelar")
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Elige una opción")
            builder.setItems(options) { dialog, item ->
                when {
                    options[item] == "Tomar foto" -> {
                        // Verificar permisos de cámara y almacenamiento
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA), REQUEST_CODE_CAMERA_PERMISSION)
                        } else {
                            openCamera()
                        }
                    }
                    options[item] == "Seleccionar de la galería" -> {
                        // Verificar permiso de almacenamiento
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
                        } else {
                            openGallery()
                        }
                    }
                    options[item] == "Cancelar" -> {
                        dialog.dismiss()
                    }
                }
            }
            builder.show()
        }

        val registrar = findViewById<Button>(R.id.button)
        registrar.setOnClickListener() {
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                var permissionToRequest = ArrayList<String>()

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    permissionToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }

                ActivityCompat.requestPermissions(this, permissionToRequest.toTypedArray(), REQUEST_CODE_LOCATION_PERMISSION)
            } else {
                val email = binding.editTextTextEmailAddress2.text.toString()
                val password = binding.editTextTextPassword.text.toString()
                createUser(email, password)
            }
        }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICKER)
    }

    private fun startMapsActivity(currentUser: FirebaseUser?) {
        val intent = Intent(this, MapsActivity::class.java)
        if (currentUser != null) {
            intent.putExtra("user", currentUser.email)
        }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                openCamera()
            } else {
                Toast.makeText(this, "Los permisos son necesarios para tomar una foto", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_PERMISSION) {
            if (grantResults.all { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "El permiso es necesario para guardar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val email = binding.editTextTextEmailAddress2.text.toString()
                val password = binding.editTextTextPassword.text.toString()
                createUser(email, password)
            } else {
                Toast.makeText(
                    this,
                    "El permiso es necesario para acceder a la siguiente actividad",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            startMapsActivity(currentUser)
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = binding.editTextTextEmailAddress2.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.editTextTextEmailAddress2.error = "Required."
            valid = false
        } else {
            binding.editTextTextEmailAddress2.error = null
        }
        val password = binding.editTextTextPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.editTextTextPassword.error = "Required."
            valid = false
        } else {
            binding.editTextTextPassword.error = null
        }
        val nombre = binding.nombre.text.toString()
        if (TextUtils.isEmpty(nombre)) {
            binding.nombre.error = "Required."
            valid = false
        } else {
            binding.nombre.error = null
        }
        val apellido = binding.apellido.text.toString()
        if (TextUtils.isEmpty(apellido)) {
            binding.apellido.error = "Required."
            valid = false
        } else {
            binding.apellido.error = null
        }
        val identificacion = binding.identificacion.text.toString()
        if (TextUtils.isEmpty(identificacion)) {
            binding.identificacion.error = "Required."
            valid = false
        } else {
            binding.identificacion.error = null
        }
        return valid
    }

    private fun createUser(email: String, password: String) {
        if (validateForm()) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)
                        val user = auth.currentUser
                        if (user != null) {
                            // Update user info
                            val myUser = Persona()
                            myUser.nombre = binding.nombre.text.toString()
                            myUser.apellido = binding.apellido.text.toString()
                            myUser.numIdentificacion = binding.identificacion.text.toString().toDouble()
                            myUser.latitud = 1.80
                            myUser.longitud = 80.0
                            myUser.disponible = true
                            myUser.toastMostrado = false
                            myUser.uid = auth.currentUser!!.uid
                            myRef = database.getReference(PATH_USERS+auth.currentUser!!.uid)
                            myRef.setValue(myUser)
                            saveImage()
                            updateUI(user)
                        }
                    } else {
                        Toast.makeText(
                            this, "createUserWithEmail:Failure: " + task.exception.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        task.exception?.message?.let { Log.e(TAG, it) }
                    }

                }
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_CODE_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImage() {
        val imageBitmap = (imagen.drawable as BitmapDrawable).bitmap
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues())
        imageUri?.let {
            try {
                val outputStream = contentResolver.openOutputStream(imageUri)
                outputStream?.use {
                    imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    Toast.makeText(this, "Imagen guardada en la galería", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK, Intent().apply { putExtra("imageUri", imageUri) })
                    uploadImage(imageUri)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imagen.setImageBitmap(imageBitmap)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
            }
        } else if (requestCode == REQUEST_CODE_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            imagen.setImageURI(imageUri)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_WRITE_EXTERNAL_PERMISSION)
            }
        }
    }

    private fun uploadImage(imageUri: Uri?) {
        val file = imageUri ?: Uri.parse("android.resource://com.example.taller_3/" + R.drawable.perfil)
        val path = "images/profile/" + auth.currentUser!!.uid + "/image.jpg"
        val imageRef = storage.reference.child(path)
        imageRef.putFile(file)
            .addOnSuccessListener(object : OnSuccessListener<UploadTask.TaskSnapshot> {
                override fun onSuccess(taskSnapshot: UploadTask.TaskSnapshot) {
                    // Get a URL to the uploaded content
                    Log.i("FBApp", "Successfully uploaded image")
                }
            })
            .addOnFailureListener(object : OnFailureListener {
                override fun onFailure(exception: Exception) {
                    // Handle unsuccessful uploads
                    // ...
                }
            })
    }
    
}
