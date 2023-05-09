import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taller_3.Persona
import com.example.taller_3.R
import com.google.firebase.storage.FirebaseStorage

class UsuariosAdapter(
    private val listaUsuarios: List<Persona>,
    private val onVerPosicionClickListener: OnVerPosicionClickListener
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewUsuario: ImageView = itemView.findViewById(R.id.imageViewUsuario)
        val textViewNombre: TextView = itemView.findViewById(R.id.textViewNombre)
        val buttonVerPosicion: Button = itemView.findViewById(R.id.buttonVerPosicion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.adapter_usuarios,
            parent,
            false
        )
        return UsuarioViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = listaUsuarios[position]

        holder.textViewNombre.text = "${usuario.nombre} ${usuario.apellido}"

        // Descargar imagen de Firebase Storage y mostrarla en el ImageView
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/profile/${usuario.uid}/image.jpg")

        imageRef.getBytes(1024 * 1024)
            .addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.imageViewUsuario.setImageBitmap(bitmap)
            }
            .addOnFailureListener { exception ->
                Log.d("UsuariosAdapter", "Error al descargar la imagen del usuario ${usuario.nombre}: $exception")
            }

        holder.buttonVerPosicion.setOnClickListener {
            onVerPosicionClickListener.onVerPosicionClick(usuario)
        }
    }

    override fun getItemCount() = listaUsuarios.size

    interface OnVerPosicionClickListener {
        fun onVerPosicionClick(usuario: Persona)
    }
}
