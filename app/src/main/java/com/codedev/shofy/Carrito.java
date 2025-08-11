package com.codedev.shofy;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.adapters.CarritoAdapter;
import com.codedev.shofy.models.DialogCompraRealizada;
import com.codedev.shofy.models.ItemCarrito;
import com.codedev.shofy.models.Producto;
import com.codedev.shofy.utils.CarritoManager;
import com.codedev.shofy.utils.PrefsDireccion;

import java.util.List;

public class Carrito extends Fragment {

    // UI
    private RecyclerView recyclerCarrito;
    private TextView txtSubtotal, txtIVA, txtTotal, txtDireccionEnvio;
    private Button btnRealizarPedido, btnEditarDireccion;
    private CarritoAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_carrito, container, false);

        // Inicializar vistas
        recyclerCarrito      = view.findViewById(R.id.recyclerCarrito);
        txtSubtotal          = view.findViewById(R.id.txtSubtotal);
        txtIVA               = view.findViewById(R.id.txtIVA);
        txtTotal             = view.findViewById(R.id.txtTotal);
        txtDireccionEnvio    = view.findViewById(R.id.txtDireccionEnvio);
        btnEditarDireccion   = view.findViewById(R.id.btnEditarDireccion);
        btnRealizarPedido    = view.findViewById(R.id.btnRealizarPedido);

        // Dirección actual por usuario
        cargarDireccionGuardada();

        // Eventos
        btnEditarDireccion.setOnClickListener(v -> mostrarDialogoDireccion());
        btnRealizarPedido.setOnClickListener(v -> realizarPedido(v.getContext(), v));

        // RecyclerView
        recyclerCarrito.setLayoutManager(new LinearLayoutManager(getContext()));
        List<ItemCarrito> items = CarritoManager.getInstancia().getItems();
        adapter = new CarritoAdapter(items, () -> calcularYMostrarResumen(CarritoManager.getInstancia().getItems()));
        recyclerCarrito.setAdapter(adapter);

        calcularYMostrarResumen(items);
        return view;
    }

    // ---------------------------------------
    // DIRECCIÓN (per-usuario)
    // ---------------------------------------
    private void cargarDireccionGuardada() {
        String direccion = PrefsDireccion.obtenerDireccion(requireContext());
        if (!TextUtils.isEmpty(direccion)) {
            txtDireccionEnvio.setText("Dirección de envío: " + direccion);
        } else {
            txtDireccionEnvio.setText("Dirección de envío: No agregada");
        }
    }

    private void mostrarDialogoDireccion() {
        Context context = requireContext();
        EditText input = new EditText(context);
        input.setHint("Escribe tu dirección");

        String direccionActual = PrefsDireccion.obtenerDireccion(context);
        if (!TextUtils.isEmpty(direccionActual)) {
            input.setText(direccionActual);
        }

        new AlertDialog.Builder(context)
                .setTitle("Dirección de envío")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevaDireccion = input.getText().toString().trim();
                    if (!nuevaDireccion.isEmpty()) {
                        PrefsDireccion.guardarDireccion(context, nuevaDireccion);
                        txtDireccionEnvio.setText("Dirección de envío: " + nuevaDireccion);
                        Toast.makeText(context, "Dirección guardada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "La dirección no puede estar vacía", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------------------------------------
    // PEDIDO
    // ---------------------------------------
    private void realizarPedido(Context context, View view) {
        int idUsuario = obtenerIdUsuario();

        if (idUsuario == 0) {
            Toast.makeText(context, "Debes iniciar sesión para proceder con la compra.", Toast.LENGTH_LONG).show();
            Navigation.findNavController(view).navigate(R.id.login);
            return;
        }

        String direccion = com.codedev.shofy.utils.PrefsDireccion.obtenerDireccion(context);
        if (direccion == null || direccion.trim().isEmpty()) {
            Toast.makeText(context, "Por favor agrega una dirección de envío antes de continuar.", Toast.LENGTH_LONG).show();
            return;
        }

        List<ItemCarrito> carritoItems = com.codedev.shofy.utils.CarritoManager.getInstancia().getItems();
        if (carritoItems.isEmpty()) {
            Toast.makeText(context, "El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = null;
        long idVenta = -1;

        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();

            // 1) Insertar venta usando la MISMA conexión
            idVenta = dbHelper.insertarVenta(db, idUsuario, direccion);
            if (idVenta <= 0) throw new Exception("No se pudo registrar la venta");

            // 2) Detalles + stock usando la MISMA conexión
            for (ItemCarrito item : carritoItems) {
                Producto producto = item.getProducto();
                int cantidad = item.getCantidad();

                if (cantidad > producto.getCantidad_actual()) {
                    throw new Exception("La cantidad de " + producto.getNombre() + " supera el stock disponible.");
                }

                double precioUnitarioConIVA = producto.getPrecioBase() * (1 + obtenerIVA(producto.getTipo()));

                long idDet = dbHelper.insertarDetalleVenta(db, idVenta, producto.getId(), cantidad, precioUnitarioConIVA);
                if (idDet == -1) throw new Exception("No se pudo registrar el detalle de venta");

                dbHelper.descontarStock(db, producto.getId(), cantidad);
            }

            db.setTransactionSuccessful();

            Toast.makeText(context, "Venta registrada con éxito", Toast.LENGTH_SHORT).show();
            DialogCompraRealizada dialog = new DialogCompraRealizada();
            dialog.show(getParentFragmentManager(), "compra_realizada");

        } catch (Exception e) {
            Log.e("VENTA", "Error al registrar venta", e);
            Toast.makeText(context, "Error al registrar la venta: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        } finally {
            if (db != null) {
                try { db.endTransaction(); } catch (Exception ignored) {}
                try { db.close(); } catch (Exception ignored) {}
            }
        }

        // Limpiar carrito y actualizar vista
        com.codedev.shofy.utils.CarritoManager.getInstancia().limpiarCarrito();
        if (adapter != null) adapter.notifyDataSetChanged();
        calcularYMostrarResumen(com.codedev.shofy.utils.CarritoManager.getInstancia().getItems());

        // Redirigir al home
        Bundle args = new Bundle();
        args.putBoolean("pedidoRealizado", true);
        NavController navController = Navigation.findNavController(view);
        if (navController.getCurrentDestination() == null ||
                navController.getCurrentDestination().getId() != R.id.nav_home) {
            navController.navigate(R.id.nav_home, args);
        }
    }


    private int obtenerIdUsuario() {
        Context context = getContext();
        if (context == null) return 0;
        SharedPreferences preferences = context.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        return preferences.getInt("idUsuario", 0);
    }

    // ---------------------------------------
    // RESUMEN DE COMPRA
    // ---------------------------------------
    private void calcularYMostrarResumen(List<ItemCarrito> items) {
        double subtotal = 0;
        double ivaTotal = 0;

        for (ItemCarrito item : items) {
            double precioUnit = item.getProducto().getPrecioBase();
            int cantidad = item.getCantidad();
            double subtotalItem = precioUnit * cantidad;
            double ivaItem = subtotalItem * obtenerIVA(item.getProducto().getTipo());

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
            case "Papelería":   return 0.16;
            case "Supermercado":return 0.04;
            case "Droguería":   return 0.12;
            default:            return 0.0;
        }
    }

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;

        DBHelper dbh = new DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }
}
