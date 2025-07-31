package com.codedev.shofy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.R;
import com.codedev.shofy.models.ItemCarrito;

import java.util.List;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder> {

    private final List<ItemCarrito> items;

    public CarritoAdapter(List<ItemCarrito> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CarritoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carrito, parent, false);
        return new CarritoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CarritoViewHolder holder, int position) {
        ItemCarrito item = items.get(position);

        holder.txtNombre.setText(item.getProducto().getNombre());
        holder.txtCantidad.setText("Cantidad: " + item.getCantidad());
        holder.txtPrecioUnitario.setText(String.format("Precio unitario: $%.2f", item.getProducto().getPrecioBase()));

        double subtotal = item.getProducto().getPrecioBase() * item.getCantidad();
        holder.txtSubtotal.setText(String.format("Subtotal: $%.2f", subtotal));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class CarritoViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtCantidad, txtPrecioUnitario, txtSubtotal;

        public CarritoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtNombreProductoCarrito);
            txtCantidad = itemView.findViewById(R.id.txtCantidadProductoCarrito);
            txtPrecioUnitario = itemView.findViewById(R.id.txtPrecioUnitarioProductoCarrito);
            txtSubtotal = itemView.findViewById(R.id.txtSubtotalProductoCarrito);
        }
    }
}
