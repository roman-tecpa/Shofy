// DBVentas.java
package com.codedev.shofy.DB;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.codedev.shofy.models.CompraDetalle;
import com.codedev.shofy.models.CompraResumen;
import com.codedev.shofy.models.ItemCarrito;
import com.codedev.shofy.models.Producto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DBVentas extends DBHelper {
    private final Context context;

    public DBVentas(Context context) {
        super(context);
        this.context = context;
    }

    // ========= UTIL TIEMPO =========
    private static String isoUtcNow(long ms) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new java.util.Date(ms));
    }

    /** REGISTRA UNA VENTA con sus detalles. Guarda en precio_venta el PRECIO UNITARIO con IVA. */
    public long registrarVenta(int idUsuario, List<ItemCarrito> items) {
        long idVenta = -1;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // MISMO instante para ambas columnas
            long nowMs = System.currentTimeMillis();
            String nowIso = isoUtcNow(nowMs);

            // Cabecera
            ContentValues ventaValues = new ContentValues();
            ventaValues.put("id_usuario", idUsuario);
            ventaValues.put("fecha_venta", nowIso);                // ISO-8601 UTC
            ventaValues.put("fecha_venta_millis", nowMs);          // epoch ms
            // Si tienes direccion_envio, añade: ventaValues.put("direccion_envio", direccion);

            idVenta = db.insertOrThrow("Ventas", null, ventaValues);

            // Detalles
            if (items != null) {
                for (ItemCarrito item : items) {
                    if (item == null || item.getProducto() == null) continue;
                    Producto prod = item.getProducto();

                    int idProducto = prod.getId();
                    int cantidad = item.getCantidad();
                    double precioBase = prod.getPrecioBase();
                    double iva = obtenerIVA(prod.getTipo());
                    double precioUnitFinal = precioBase * (1 + iva); // unitario con IVA

                    ContentValues d = new ContentValues();
                    d.put("id_venta", idVenta);
                    d.put("id_producto", idProducto);
                    d.put("cantidad_vendida", cantidad);
                    d.put("precio_venta", precioUnitFinal);
                    db.insertOrThrow("DetalleVentas", null, d);

                    // Descontar stock en la misma transacción
                    db.execSQL(
                            "UPDATE Productos SET cantidad_actual = cantidad_actual - ? WHERE id = ?",
                            new Object[]{cantidad, idProducto}
                    );
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            idVenta = -1;
        } finally {
            try { db.endTransaction(); } catch (Exception ignored) {}
            db.close();
        }

        // POST-COMMIT: notificar bajo stock (solo si hay admin logueado)
        try {
            if (idVenta != -1 && isAdminLoggedIn(context) && items != null) {
                DBProductos dbp = new DBProductos(context);
                HashSet<Integer> ids = new HashSet<>();
                for (ItemCarrito it : items) {
                    if (it != null && it.getProducto() != null) ids.add(it.getProducto().getId());
                }
                for (Integer idProd : ids) {
                    if (dbp.productoEstaBajoMinimo(idProd)) {
                        Producto p = dbp.getProductoById(idProd);
                        if (p != null) com.codedev.shofy.utils.NotificationUtils.notifyLowStockSingle(context, p);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return idVenta;
    }

    /** IVA por tipo (tolera acentos y sin acentos) */
    private double obtenerIVA(String tipo) {
        if (tipo == null) return 0.10;
        String t = tipo.toLowerCase();
        if (t.equals("supermercado")) return 0.16;
        if (t.equals("papelería") || t.equals("papeleria")) return 0.08;
        if (t.equals("droguería") || t.equals("drogueria")) return 0.12;
        return 0.10;
    }

    // ========= LISTADOS =========

    /** NUEVO: LISTA COMPRAS por rango en millis (UTC). Empata con tu ComprasAdmin. */
    public ArrayList<CompraResumen> listarComprasResumenRangoMillis(long desdeMs, long hastaMs) {
        ArrayList<CompraResumen> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT v.id, v.id_usuario, v.fecha_venta_millis, " +
                        "IFNULL(u.nombre,'') AS cliente, " +
                        "SUM(d.cantidad_vendida * d.precio_venta) AS total " +
                        "FROM Ventas v " +
                        "JOIN DetalleVentas d ON d.id_venta = v.id " +
                        "LEFT JOIN Usuarios u ON u.id = v.id_usuario " +
                        "WHERE v.fecha_venta_millis BETWEEN ? AND ? " +
                        "GROUP BY v.id " +
                        "ORDER BY v.fecha_venta_millis DESC",
                new String[]{ String.valueOf(desdeMs), String.valueOf(hastaMs) }
        );

        while (c.moveToNext()) {
            int idVenta = c.getInt(0);
            int idUsuario = c.getInt(1);
            long fechaMs = c.getLong(2);
            String cliente = c.getString(3);
            double total = c.getDouble(4);

            // Modelo esperado por tu ComprasAdmin/Adapter
            lista.add(new CompraResumen(idVenta, idUsuario, fechaMs, cliente, total));
        }
        c.close();
        db.close();
        return lista;
    }

    /** LEGADO: filtrado con strings ISO. Mejor usa listarComprasResumenRangoMillis(). */
    @Deprecated
    public ArrayList<CompraResumen> listarComprasResumenRango(String desdeIso, String hastaIso) {
        ArrayList<CompraResumen> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql =
                "SELECT v.id, v.fecha_venta, " +
                        "       IFNULL(SUM(d.cantidad_vendida * d.precio_venta), 0) AS total, " +
                        "       (SELECT p.nombre FROM DetalleVentas d2 " +
                        "           JOIN Productos p ON p.id = d2.id_producto " +
                        "          WHERE d2.id_venta = v.id LIMIT 1) AS producto_ejemplo, " +
                        "       COUNT(DISTINCT d.id_producto) AS prod_distintos " +
                        "FROM Ventas v " +
                        "LEFT JOIN DetalleVentas d ON d.id_venta = v.id " +
                        "WHERE datetime(v.fecha_venta) BETWEEN datetime(?) AND datetime(?) " +
                        "GROUP BY v.id, v.fecha_venta " +
                        "ORDER BY datetime(v.fecha_venta) DESC";

        Cursor c = db.rawQuery(sql, new String[]{desdeIso, hastaIso});
        while (c.moveToNext()) {
            // Si alguien aún llama a este método, devuelves algo básico
            int id = c.getInt(0);
            String fecha = c.getString(1);
            double total = c.getDouble(2);
            // cliente y idUsuario no están en este select; marcamos genérico
            lista.add(new CompraResumen(id, 0, /*fechaMs*/ 0L, "(ver detalle)", total));
        }
        c.close();
        db.close();
        return lista;
    }

    /** DETALLE de una venta para el ticket (cabecera + líneas). Devuelve fechaMillis. */
    public CompraDetalle obtenerDetalleVenta(int idVenta) {
        SQLiteDatabase db = this.getReadableDatabase();
        CompraDetalle detalle = null;

        // Cabecera (traemos fecha_venta_millis + cliente)
        Cursor cVenta = db.rawQuery(
                "SELECT v.id, v.id_usuario, v.fecha_venta_millis, IFNULL(u.nombre,'') AS cliente " +
                        "FROM Ventas v " +
                        "LEFT JOIN Usuarios u ON u.id = v.id_usuario " +
                        "WHERE v.id = ? LIMIT 1",
                new String[]{ String.valueOf(idVenta) }
        );

        if (cVenta.moveToFirst()) {
            long fechaMs = cVenta.getLong(2);
            String cliente = cVenta.getString(3);
            detalle = new CompraDetalle(idVenta, fechaMs, cliente);
        }
        cVenta.close();

        if (detalle == null) {
            db.close();
            return null;
        }

        // Líneas
        Cursor cProd = db.rawQuery(
                "SELECT p.nombre, d.cantidad_vendida, d.precio_venta " +
                        "FROM DetalleVentas d " +
                        "JOIN Productos p ON p.id = d.id_producto " +
                        "WHERE d.id_venta = ?",
                new String[]{ String.valueOf(idVenta) }
        );
        double total = 0.0;
        while (cProd.moveToNext()) {
            String nombre = cProd.getString(0);
            int cantidad = cProd.getInt(1);
            double precioUnit = cProd.getDouble(2);
            detalle.getProductos().add(
                    new CompraDetalle.ProductoLinea(nombre, cantidad, precioUnit)
            );
            total += cantidad * precioUnit;
        }
        cProd.close();
        db.close();

        detalle.setTotal(total);
        return detalle;
    }

    // ===== Helper: ¿hay admin logueado? =====
    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;

        DBHelper dbh = new DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }
}
