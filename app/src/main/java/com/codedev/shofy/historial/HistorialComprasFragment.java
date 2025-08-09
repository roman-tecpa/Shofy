// app/src/main/java/com/codedev/shofy/historial/HistorialComprasFragment.java
package com.codedev.shofy.historial;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.R;
import com.codedev.shofy.models.Producto;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.navigation.fragment.NavHostFragment;

public class HistorialComprasFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvTotalGastado, tvEmpty;
    private DBHelper dbHelper;
    private HistorialAdapter adapter;
    private final NumberFormat currencyMx = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial_compras, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        rv = v.findViewById(R.id.rvHistorial);
        tvTotalGastado = v.findViewById(R.id.tvTotalGastado);
        tvEmpty = v.findViewById(R.id.tvEmpty);

        dbHelper = new DBHelper(requireContext());

        adapter = new HistorialAdapter((idProducto, nombre) -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor pc = db.rawQuery(
                        "SELECT id, nombre, descripcion, tipo, cantidad_actual, cantidad_minima, precio_base " +
                                "FROM Productos WHERE id = ? LIMIT 1",
                        new String[]{ String.valueOf(idProducto) }
                );

                if (pc.moveToFirst()) {
                    int id          = pc.getInt(0);
                    String nombreP  = pc.getString(1);
                    String descP    = pc.getString(2);
                    String tipoP    = pc.getString(3);
                    int cantAct     = pc.getInt(4);
                    int cantMin     = pc.getInt(5);
                    float precioP   = (float) pc.getDouble(6);

                    Producto p = new Producto(id, nombreP, descP, tipoP, cantAct, cantMin, precioP);

                    Bundle args = new Bundle();
                    args.putSerializable("producto", p);

                    NavHostFragment.findNavController(HistorialComprasFragment.this)
                            .navigate(R.id.detalladoProducto, args);
                } else {
                    Toast.makeText(requireContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show();
                }
                pc.close();
                db.close();

            } catch (Exception e) {
                Toast.makeText(requireContext(), "No se pudo abrir el producto", Toast.LENGTH_SHORT).show();
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        cargarDatos();
    }

    private void cargarDatos() {
        int idUsuario = obtenerIdUsuarioSesion(requireContext());
        if (idUsuario == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Inicia sesión para ver tu historial.");
            rv.setVisibility(View.GONE);
            tvTotalGastado.setText("Total gastado: " + currencyMx.format(0));
            return;
        }

        // Total gastado
        double total = dbHelper.totalGastadoPorUsuario(idUsuario);
        tvTotalGastado.setText("Total gastado: " + currencyMx.format(total));

        // Lista
        List<HistorialAdapter.Item> items = new ArrayList<>();
        Cursor c = dbHelper.historialComprasPorUsuario(idUsuario); // <-- Debe incluir v.fecha_venta_millis AS fecha_millis

        try {
            while (c.moveToNext()) {
                HistorialAdapter.Item it = new HistorialAdapter.Item();
                it.idDetalle       = c.getInt(c.getColumnIndexOrThrow("id_detalle"));
                it.idVenta         = c.getInt(c.getColumnIndexOrThrow("id_venta"));
                it.idProducto      = c.getInt(c.getColumnIndexOrThrow("id_producto"));
                it.nombreProducto  = c.getString(c.getColumnIndexOrThrow("nombre_producto"));
                it.cantidad        = c.getInt(c.getColumnIndexOrThrow("cantidad"));
                it.precioUnit      = c.getDouble(c.getColumnIndexOrThrow("precio_unit"));
                it.totalLinea      = c.getDouble(c.getColumnIndexOrThrow("total_linea"));
                it.stockDisponible = c.getInt(c.getColumnIndexOrThrow("stock_disponible"));

                // 🔥 Usamos epoch millis para formatear a hora LOCAL
                long millis = 0L;
                try {
                    millis = c.getLong(c.getColumnIndexOrThrow("fecha_millis")); // alias de v.fecha_venta_millis
                } catch (Exception ignored) {
                    // Si aún no migraste DBHelper, haz fallback temporal a la columna vieja (UTC string):
                    // it.fechaTexto = c.getString(c.getColumnIndexOrThrow("fecha_venta"));
                }

                if (millis > 0L) {
                    java.text.DateFormat df = android.text.format.DateFormat.getMediumDateFormat(requireContext());
                    java.text.DateFormat tf = android.text.format.DateFormat.getTimeFormat(requireContext());
                    it.fechaTexto = df.format(new java.util.Date(millis)) + " " + tf.format(new java.util.Date(millis));
                } else {
                    // fallback por si no existe el campo (evita crashear)
                    it.fechaTexto = "—";
                }

                items.add(it);
            }
        } finally {
            if (c != null) c.close();
        }

        if (items.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            adapter.setData(items);
        }
    }

    private int obtenerIdUsuarioSesion(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        int id = sp.getInt("idUsuario", 0);
        if (id == 0) {
            String correo = sp.getString("correo", null);
            if (correo != null) {
                try { id = new DBHelper(ctx).obtenerIdUsuarioPorCorreo(correo); } catch (Exception ignored) {}
            }
        }
        return id;
    }
}
