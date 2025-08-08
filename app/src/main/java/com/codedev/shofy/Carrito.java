package com.codedev.shofy;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

import java.util.List;

public class Carrito extends Fragment {

    // UI
    private RecyclerView recyclerCarrito;
    private TextView txtSubtotal, txtIVA, txtTotal, txtDireccionEnvio;
    private Button btnRealizarPedido, btnEditarDireccion;
    private CarritoAdapter adapter;

    // Preferencias
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "MisPreferencias";
    private static final String CLAVE_DIRECCION = "direccion_envio";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_carrito, container, false);

        // Inicializar vistas
        recyclerCarrito = view.findViewById(R.id.recyclerCarrito);
        txtSubtotal = view.findViewById(R.id.txtSubtotal);
        txtIVA = view.findViewById(R.id.txtIVA);
        txtTotal = view.findViewById(R.id.txtTotal);
        txtDireccionEnvio = view.findViewById(R.id.txtDireccionEnvio);
        btnEditarDireccion = view.findViewById(R.id.btnEditarDireccion);
        btnRealizarPedido = view.findViewById(R.id.btnRealizarPedido);

        // Preferencias
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
    // DIRECCIÓN
    // ---------------------------------------
    private void cargarDireccionGuardada() {
        String direccion = sharedPreferences.getString(CLAVE_DIRECCION, "");
        if (!direccion.isEmpty()) {
            txtDireccionEnvio.setText("Dirección de envío: " + direccion);
        } else {
            txtDireccionEnvio.setText("Dirección de envío: No agregada");
        }
    }

    private void mostrarDialogoDireccion() {
        Context context = requireContext();
        EditText input = new EditText(context);
        input.setHint("Escribe tu dirección");
        input.setText(sharedPreferences.getString(CLAVE_DIRECCION, ""));

        new AlertDialog.Builder(context)
                .setTitle("Dirección de envío")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevaDireccion = input.getText().toString().trim();
                    if (!nuevaDireccion.isEmpty()) {
                        sharedPreferences.edit().putString(CLAVE_DIRECCION, nuevaDireccion).apply();
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

        // Validar dirección
        String direccion = sharedPreferences.getString(CLAVE_DIRECCION, null);
        if (direccion == null || direccion.trim().isEmpty()) {
            Toast.makeText(context, "Por favor agrega una dirección de envío antes de continuar.", Toast.LENGTH_LONG).show();
            return;
        }

        // Validar carrito
        List<ItemCarrito> carritoItems = CarritoManager.getInstancia().getItems();
        if (carritoItems.isEmpty()) {
            Toast.makeText(context, "El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // Insertar venta con dirección
            ContentValues ventaValues = new ContentValues();
            ventaValues.put("id_usuario", idUsuario);
            ventaValues.put("direccion_envio", direccion); // 👈 Guardar dirección en BD
            long idVenta = db.insert("Ventas", null, ventaValues);

            if (idVenta == -1) throw new Exception("No se pudo registrar la venta");

            // Detalles de productos
            for (ItemCarrito item : carritoItems) {
                Producto producto = item.getProducto();
                int cantidad = item.getCantidad();

                if (cantidad > producto.getCantidad_actual()) {
                    throw new Exception("La cantidad de " + producto.getNombre() + " supera el stock disponible.");
                }

                double precioUnitarioConIVA = producto.getPrecioBase() * (1 + obtenerIVA(producto.getTipo()));

                ContentValues detalle = new ContentValues();
                detalle.put("id_venta", idVenta);
                detalle.put("id_producto", producto.getId());
                detalle.put("cantidad_vendida", cantidad);
                detalle.put("precio_venta", precioUnitarioConIVA);

                long idDetalle = db.insert("DetalleVentas", null, detalle);
                if (idDetalle == -1) throw new Exception("No se pudo registrar el detalle de venta");

                // Actualizar stock
                db.execSQL("UPDATE Productos SET cantidad_actual = cantidad_actual - ? WHERE id = ?",
                        new Object[]{cantidad, producto.getId()});
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
            db.endTransaction();
            db.close();
        }

        // Limpiar carrito y actualizar vista
        CarritoManager.getInstancia().limpiarCarrito();
        if (adapter != null) adapter.notifyDataSetChanged();
        calcularYMostrarResumen(CarritoManager.getInstancia().getItems());

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
            case "Papelería": return 0.16;
            case "Supermercado": return 0.04;
            case "Droguería": return 0.12;
            default: return 0.0;
        }
    }

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;

        com.codedev.shofy.DB.DBHelper dbh = new com.codedev.shofy.DB.DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }

}
