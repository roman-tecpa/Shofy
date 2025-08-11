package com.codedev.shofy.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.R;
import com.codedev.shofy.models.Producto;

import java.util.ArrayList;

public class ProductoAdapterHome extends RecyclerView.Adapter<ProductoAdapterHome.ProductoViewHolder> {

    private ArrayList<Producto> productos;
    private ArrayList<Producto> productosOriginales;
    private Fragment parentFragment;

    // Constructor
    public ProductoAdapterHome(ArrayList<Producto> productos, Fragment parentFragment) {
        this.productos = new ArrayList<>(productos);
        this.productosOriginales = new ArrayList<>(productos);
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto_home, parent, false);
        return new ProductoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        Producto producto = productos.get(position);

        holder.nombre.setText(producto.getNombre());
        holder.precio.setText("$" + producto.getPrecioBase());

        // Mostrar unidades disponibles
        holder.unidades.setText("Unidades: " + producto.getCantidad_actual());

        int iconoTipo = obtenerIconoPorTipo(producto.getTipo());
        holder.imagen.setImageResource(iconoTipo);

        // Click para abrir fragmento de detalle
        holder.itemView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putSerializable("producto", producto);
            Navigation.findNavController(v).navigate(R.id.action_nav_home_to_detalladoProducto, args);
        });
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    public void filtrar(String texto) {
        texto = texto.toLowerCase();
        productos.clear();

        if (texto.isEmpty()) {
            productos.addAll(productosOriginales);
        } else {
            for (Producto p : productosOriginales) {
                if (p.getNombre().toLowerCase().contains(texto)) {
                    productos.add(p);
                }
            }
        }

        notifyDataSetChanged();
    }

    public void filtrarPorTipo(String tipo) {
        productos.clear();
        for (Producto p : productosOriginales) {
            if (p.getTipo().equalsIgnoreCase(tipo)) {
                productos.add(p);
            }
        }
        notifyDataSetChanged();
    }

    private int obtenerIconoPorTipo(String tipo) {
        switch (tipo) {
            case "Papelería": return R.drawable.ic_papeleria;
            case "Supermercado": return R.drawable.ic_supermercado;
            case "Droguería": return R.drawable.ic_drogueria;
            default: return R.drawable.ic_default;
        }
    }

    // ViewHolder
    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView nombre, precio, unidades;
        ImageView imagen;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.txtNombre);
            precio = itemView.findViewById(R.id.txtPrecio);
            unidades = itemView.findViewById(R.id.txtUnidades); // Nuevo TextView
            imagen = itemView.findViewById(R.id.imgProducto);
        }
    }
}
