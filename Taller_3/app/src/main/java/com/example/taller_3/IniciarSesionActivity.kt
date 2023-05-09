package com.example.taller_3

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller_3.databinding.ActivityIniciarSesionBinding
import com.example.taller_3.databinding.ActivityMapsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class IniciarSesionActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    val REQUEST_CODE_LOCATION_PERMISSION = 1
    private lateinit var binding: ActivityIniciarSesionBinding
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciar_sesion)
        binding = ActivityIniciarSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        val iniciar = findViewById<Button>(R.id.button)
        iniciar.setOnClickListener() {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
            } else {
                val email = binding.editTextTextEmailAddress2.text.toString()
                val password = binding.editTextTextPassword.text.toString()
                signInUser(email, password)
            }
        }
    }

    private fun startMapsActivity(currentUser: FirebaseUser?) {
        val intent = Intent(this, MapsActivity::class.java)
        if (currentUser != null) {
            intent.putExtra("user", currentUser.email)
        }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val email = binding.editTextTextEmailAddress2.text.toString()
                val password = binding.editTextTextPassword.text.toString()
                signInUser(email, password)
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
        } else {
            binding.editTextTextEmailAddress2.setText("")
            binding.editTextTextPassword.setText("")
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
        return valid
    }

    private fun signInUser(email: String, password: String) {
        if (validateForm()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
// Sign in success, update UI
                        Log.d(TAG, "signInWithEmail:success:")
                        val user = auth.currentUser
                        updateUI(auth.currentUser)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            this, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null)
                    }
                }
        }
    }
}