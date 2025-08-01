package com.codedev.shofy;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.adapters.CarritoAdapter;
import com.codedev.shofy.models.DialogCompraRealizada;
import com.codedev.shofy.models.ItemCarrito;
import com.codedev.shofy.models.Producto;
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

        adapter = new CarritoAdapter(items, () -> calcularYMostrarResumen(CarritoManager.getInstancia().getItems()));
        recyclerCarrito.setAdapter(adapter);

        calcularYMostrarResumen(items);

        btnRealizarPedido.setOnClickListener(v -> realizarPedido(v.getContext(), v));

        return view;
    }

    private void realizarPedido(Context context, View view) {
        int idUsuario = obtenerIdUsuario(); // Obtener el id del usuario (0 si no está logueado)

        if (idUsuario == 0) {
            // Usuario no ha iniciado sesión
            Toast.makeText(context, "Debes iniciar sesión para proceder con la compra.", Toast.LENGTH_LONG).show();
            return; // No continuar con la compra
        }

        List<ItemCarrito> carritoItems = CarritoManager.getInstancia().getItems();

        if (carritoItems.isEmpty()) {
            Toast.makeText(context, "El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // Insertar la venta
            ContentValues ventaValues = new ContentValues();
            ventaValues.put("id_usuario", idUsuario);
            long idVenta = db.insert("Ventas", null, ventaValues);

            if (idVenta == -1) throw new Exception("No se pudo registrar la venta");

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

                // Restar stock aquí solamente
                db.execSQL("UPDATE Productos SET cantidad_actual = cantidad_actual - ? WHERE id = ?",
                        new Object[]{cantidad, producto.getId()});
            }

            db.setTransactionSuccessful();
            Toast.makeText(context, "Venta registrada con éxito", Toast.LENGTH_SHORT).show();

            // Mostrar diálogo de compra realizada (opcional)
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

        // Limpiar carrito y actualizar resumen
        CarritoManager.getInstancia().limpiarCarrito();
        if (adapter != null) adapter.notifyDataSetChanged();
        calcularYMostrarResumen(CarritoManager.getInstancia().getItems());

        // Regresar al Home
        Bundle args = new Bundle();
        args.putBoolean("pedidoRealizado", true);

        NavController navController = Navigation.findNavController(view);
        if (navController.getCurrentDestination() == null ||
                navController.getCurrentDestination().getId() != R.id.nav_home) {
            navController.navigate(R.id.nav_home, args);
        }
    }

    private int obtenerIdUsuario() {
        // TODO: Aquí debes implementar la lógica real para obtener el id del usuario.
        // Por ejemplo, leyendo SharedPreferences, base de datos, sesión, etc.
        // Por ahora simulamos que no hay usuario logueado devolviendo 0
        return 0;
    }

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
