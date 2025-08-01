package com.codedev.shofy;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.adapters.CarritoAdapter;
import com.codedev.shofy.models.ItemCarrito;
import com.codedev.shofy.utils.CarritoManager;

import java.util.List;

public class Carrito extends Fragment {

    private RecyclerView recyclerCarrito;
    private TextView txtSubtotal, txtIVA, txtTotal;
    private Button btnRealizarPedido;
    private CarritoAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_carrito, container, false);

        recyclerCarrito = view.findViewById(R.id.recyclerCarrito);
        txtSubtotal = view.findViewById(R.id.txtSubtotal);
        txtIVA = view.findViewById(R.id.txtIVA);
        txtTotal = view.findViewById(R.id.txtTotal);
        btnRealizarPedido = view.findViewById(R.id.btnRealizarPedido);

        recyclerCarrito.setLayoutManager(new LinearLayoutManager(getContext()));

        List<ItemCarrito> items = CarritoManager.getInstancia().getItems();

        // Pasamos un Runnable para actualizar resumen cada vez que cambia el carrito
        adapter = new CarritoAdapter(items, () -> calcularYMostrarResumen(CarritoManager.getInstancia().getItems()));

        recyclerCarrito.setAdapter(adapter);

        calcularYMostrarResumen(items);

        btnRealizarPedido.setOnClickListener(v -> {
            try {
                Context context = v.getContext();
                if (context == null) return;
                Toast.makeText(context, "Pedido realizado con éxito", Toast.LENGTH_SHORT).show();
                CarritoManager carrito = CarritoManager.getInstancia();
                carrito.limpiarCarrito();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                calcularYMostrarResumen(carrito.getItems());

                Bundle args = new Bundle();
                args.putBoolean("pedidoRealizado", true);

                NavController navController = Navigation.findNavController(v);
                if (navController.getCurrentDestination() == null ||
                        navController.getCurrentDestination().getId() != R.id.nav_home) {
                    navController.navigate(R.id.nav_home, args);
                }

            } catch (Exception e) {
                Log.e("REALIZAR_PEDIDO", "Error al realizar pedido", e);
                Toast.makeText(v.getContext(), "Error al realizar el pedido", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void calcularYMostrarResumen(List<ItemCarrito> items) {
        double subtotal = 0;
        double ivaTotal = 0;

        for (ItemCarrito item : items) {
            double precioUnit = item.getProducto().getPrecioBase();
            int cantidad = item.getCantidad();
            double subtotalItem = precioUnit * cantidad;

            double ivaPorcentaje = obtenerIVA(item.getProducto().getTipo());
            double ivaItem = subtotalItem * ivaPorcentaje;

            subtotal += subtotalItem;
            ivaTotal += ivaItem;
        }

        double total = subtotal + ivaTotal;

        txtSubtotal.setText(String.format("Subtotal: $%.2f", subtotal));
        txtIVA.setText(String.format("IVA: $%.2f", ivaTotal));
        txtTotal.setText(String.format("Total: $%.2f", total));
    }

    private double obtenerIVA(String tipo) {
        switch (tipo) {
            case "Papelería":
                return 0.16;
            case "Supermercado":
                return 0.04;
            case "Droguería":
                return 0.12;
            default:
                return 0.0;
        }
    }
}
