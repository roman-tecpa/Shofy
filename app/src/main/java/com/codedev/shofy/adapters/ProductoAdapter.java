package com.codedev.shofy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.DB.DBProductos;
import com.codedev.shofy.R;
import com.codedev.shofy.models.Producto;

import java.util.ArrayList;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ViewHolder> {

    private ArrayList<Producto> listaProductos;
    private Context context;
    private OnProductoClickListener listener;

    // Interfaz para manejar clics
    public interface OnProductoClickListener {
        void onEditarClick(Producto producto);
    }

    public ProductoAdapter(Context context, ArrayList<Producto> lista, OnProductoClickListener listener) {
        this.context = context;
        this.listaProductos = lista;
        this.listener = listener;
    }

    @Override
    public ProductoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(context).inflate(R.layout.item_producto, parent, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Producto producto = listaProductos.get(position);
        holder.tvNombre.setText(producto.getNombre());
        holder.tvId.setText("ID: " + producto.getId());
        holder.tvPrecio.setText(String.format("$%.2f", producto.getPrecioBase()));

        // Botón Editar
        holder.btnEditar.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onEditarClick(listaProductos.get(pos));
            }
        });

        // Botón Eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Producto productoEliminar = listaProductos.get(pos);

                // Inflar el layout personalizado del diálogo
                View dialogView = LayoutInflater.from(context).inflate(R.layout.confirmacion_eliminar, null);

                TextView tvMensaje = dialogView.findViewById(R.id.tvMensaje);
                Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
                Button btnConfirmar = dialogView.findViewById(R.id.btnConfirmar);

                // Personalizar mensaje con el nombre del producto
                tvMensaje.setText("¿Estás seguro de eliminar el producto \"" + productoEliminar.getNombre() + "\"?");

                // Crear y mostrar el diálogo
                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

                btnCancelar.setOnClickListener(view -> dialog.dismiss());

                btnConfirmar.setOnClickListener(view -> {
                    DBProductos dbProductos = new DBProductos(context);
                    boolean eliminado = dbProductos.eliminarProducto(productoEliminar.getId());

                    if (eliminado) {
                        listaProductos.remove(pos);
                        notifyItemRemoved(pos);
                        Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "No se pudo eliminar el producto", Toast.LENGTH_SHORT).show();
                    }

                    dialog.dismiss();
                });



                dialog.show();
            }
        });



    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvId, tvPrecio;
        Button btnEditar, btnEliminar;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvId = itemView.findViewById(R.id.tvId);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }

    public void actualizarLista(ArrayList<Producto> nuevaLista) {
        this.listaProductos.clear();
        this.listaProductos.addAll(nuevaLista);
        notifyDataSetChanged();
    }
}
