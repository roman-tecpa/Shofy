package com.codedev.shofy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.R;
import com.codedev.shofy.models.ItemCarrito;
import com.codedev.shofy.models.Producto;

import java.util.List;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {

    private List<ItemCarrito> carrito;
    private Runnable calcularResumen;

    public CarritoAdapter(List<ItemCarrito> carrito, Runnable calcularResumen) {
        this.carrito = carrito;
        this.calcularResumen = calcularResumen;
    }

    @NonNull
    @Override
    public CarritoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carrito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarritoAdapter.ViewHolder holder, int position) {
        ItemCarrito item = carrito.get(position);
        Producto producto = item.getProducto();

        holder.txtNombre.setText(producto.getNombre());
        holder.txtPrecio.setText(String.format("Precio: $%.2f", producto.getPrecioBase()));
        holder.txtCantidad.setText(String.valueOf(item.getCantidad()));

        double subtotalItem = producto.getPrecioBase() * item.getCantidad();
        holder.txtSubtotal.setText(String.format("Subtotal: $%.2f", subtotalItem));

        // Botón Restar
        holder.btnRestar.setOnClickListener(v -> {
            if (item.getCantidad() > 1) {
                item.setCantidad(item.getCantidad() - 1);
                notifyItemChanged(position);
                calcularResumen.run();
            }
        });

        // Botón Sumar con validación de stock
        holder.btnSumar.setOnClickListener(v -> {
            int cantidadActual = item.getCantidad();
            int stockDisponible = producto.getCantidad_actual();

            if (cantidadActual < stockDisponible) {
                item.setCantidad(cantidadActual + 1);
                notifyItemChanged(position);
                calcularResumen.run();
            } else {
                Toast.makeText(holder.itemView.getContext(), "No hay más stock disponible", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón Eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            carrito.remove(position);  // Eliminar el item de la lista
            notifyItemRemoved(position);  // Notificar eliminación al RecyclerView
            notifyItemRangeChanged(position, carrito.size());  // Actualizar rango visual
            calcularResumen.run();  // Actualizar resumen en el fragmento
        });
    }

    @Override
    public int getItemCount() {
        return carrito.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtPrecio, txtCantidad, txtSubtotal;
        Button btnRestar, btnSumar, btnEliminar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreProductoCarrito);
            txtPrecio = itemView.findViewById(R.id.txtPrecioUnitarioProductoCarrito);
            txtCantidad = itemView.findViewById(R.id.txtCantidadProductoCarrito);
            txtSubtotal = itemView.findViewById(R.id.txtSubtotalProductoCarrito);
            btnRestar = itemView.findViewById(R.id.btnRestarCantidad);
            btnSumar = itemView.findViewById(R.id.btnSumarCantidad);
            btnEliminar = itemView.findViewById(R.id.btnEliminarItem);
        }
    }
}
