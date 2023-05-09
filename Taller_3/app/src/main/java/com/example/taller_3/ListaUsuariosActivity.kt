package com.example.taller_3

import UsuariosAdapter
import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class ListaUsuariosActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var usuariosAdapter: UsuariosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_usuarios)

        database = FirebaseDatabase.getInstance()
        val listaUsuarios: MutableList<Persona> = mutableListOf()
        val recyclerViewUsuarios: RecyclerView = findViewById(R.id.recyclerViewUsuarios)
        usuariosAdapter = UsuariosAdapter(listaUsuarios, object : UsuariosAdapter.OnVerPosicionClickListener {
            override fun onVerPosicionClick(usuario: Persona) {
                // Manejar el evento de clic en el botón "Ver posición" del usuario
                val intent = Intent(this@ListaUsuariosActivity, MapsActivityPorPersona::class.java)
                intent.putExtra("uid", usuario.uid)
                startActivity(intent)
            }
        })
        recyclerViewUsuarios.adapter = usuariosAdapter
        recyclerViewUsuarios.layoutManager = LinearLayoutManager(this)

        // Obtener la lista de usuarios desde la base de datos Firebase Realtime Database
        val pathUsuarios = "users"
        val usuariosRef = database.getReference(pathUsuarios)

        usuariosRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listaUsuarios.clear()

                for (usuarioSnapshot in dataSnapshot.children) {
                    val usuario = usuarioSnapshot.getValue(Persona::class.java)
                    if (usuario != null) {
                        if(usuario.disponible)
                            usuario?.let {
                                listaUsuarios.add(usuario)
                            }
                    }
                }

                usuariosAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error de la lectura de la lista de usuarios
                Toast.makeText(this@ListaUsuariosActivity, "Error al obtener la lista de usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
