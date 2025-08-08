package com.codedev.shofy.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.codedev.shofy.models.CompraDetalle;
import com.codedev.shofy.models.CompraResumen;
import com.codedev.shofy.models.ItemCarrito;
import com.codedev.shofy.models.Producto;

import java.util.ArrayList;
import java.util.List;

public class DBVentas extends DBHelper {
    private final Context context;

    public DBVentas(Context context) {
        super(context);
        this.context = context;
    }

    /** REGISTRA UNA VENTA con sus detalles. Guarda en precio_venta el PRECIO UNITARIO con IVA. */
    public long registrarVenta(int idUsuario, List<ItemCarrito> items) {
        long idVenta = -1;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Cabecera
            ContentValues ventaValues = new ContentValues();
            ventaValues.put("id_usuario", idUsuario); // fecha_venta = DEFAULT CURRENT_TIMESTAMP
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

                    // (Opcional) actualizar stock:
                    // db.execSQL("UPDATE Productos SET cantidad_actual = cantidad_actual - ? WHERE id = ?",
                    //        new Object[]{cantidad, idProducto});
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            idVenta = -1;
        } finally {
            db.endTransaction();
            db.close();
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

    /** LISTA COMPRAS con total + un producto ejemplo, filtrando por rango (ISO). */
    public ArrayList<CompraResumen> listarComprasResumenRango(String desdeIso, String hastaIso) {
        ArrayList<CompraResumen> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql =
                "SELECT v.id, v.fecha_venta, " +
                        "       IFNULL(SUM(d.cantidad_vendida * d.precio_venta), 0) AS total, " +
                        "       (SELECT p.nombre " +
                        "          FROM DetalleVentas d2 " +
                        "          JOIN Productos p ON p.id = d2.id_producto " +
                        "         WHERE d2.id_venta = v.id " +
                        "         LIMIT 1) AS producto_ejemplo, " +
                        "       COUNT(DISTINCT d.id_producto) AS prod_distintos " +
                        "FROM Ventas v " +
                        "LEFT JOIN DetalleVentas d ON d.id_venta = v.id " +
                        "WHERE datetime(v.fecha_venta) BETWEEN datetime(?) AND datetime(?) " +
                        "GROUP BY v.id, v.fecha_venta " +
                        "ORDER BY datetime(v.fecha_venta) DESC";

        Cursor c = db.rawQuery(sql, new String[]{desdeIso, hastaIso});
        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(0);
                String fecha = c.getString(1);
                double total = c.getDouble(2);
                String productoEj = c.getString(3);
                int distintos = c.getInt(4);
                lista.add(new CompraResumen(
                        id,
                        fecha,
                        total,
                        productoEj != null ? productoEj : "(sin detalle)",
                        distintos
                ));
            }
            c.close();
        }
        db.close();
        return lista;
    }

    /** DETALLE de una venta para el ticket (cabecera + líneas). */
    public CompraDetalle obtenerDetalleVenta(int idVenta) {
        SQLiteDatabase db = this.getReadableDatabase();
        CompraDetalle detalle = null;

        // Cabecera con total ya calculado
        String sqlVenta =
                "SELECT v.id, v.fecha_venta, u.nombre, u.correo, " +
                        "       IFNULL(SUM(d.cantidad_vendida * d.precio_venta), 0) AS total " +
                        "FROM Ventas v " +
                        "JOIN Usuarios u ON u.id = v.id_usuario " +
                        "LEFT JOIN DetalleVentas d ON d.id_venta = v.id " +
                        "WHERE v.id = ? " +
                        "GROUP BY v.id, v.fecha_venta, u.nombre, u.correo";

        Cursor cVenta = db.rawQuery(sqlVenta, new String[]{String.valueOf(idVenta)});
        if (cVenta.moveToFirst()) {
            int id = cVenta.getInt(0);
            String fecha = cVenta.getString(1);
            String cliente = cVenta.getString(2);
            String correo = cVenta.getString(3);
            double total = cVenta.getDouble(4);
            detalle = new CompraDetalle(id, fecha, cliente, correo, total);
        }
        cVenta.close();

        if (detalle != null) {
            // Líneas
            String sqlProd =
                    "SELECT p.nombre, d.cantidad_vendida, d.precio_venta " +
                            "FROM DetalleVentas d " +
                            "JOIN Productos p ON p.id = d.id_producto " +
                            "WHERE d.id_venta = ?";

            Cursor cProd = db.rawQuery(sqlProd, new String[]{String.valueOf(idVenta)});
            while (cProd.moveToNext()) {
                String nombre = cProd.getString(0);
                int cantidad = cProd.getInt(1);
                double precioUnit = cProd.getDouble(2);
                detalle.getProductos().add(
                        new CompraDetalle.ProductoLinea(nombre, cantidad, precioUnit)
                );
            }
            cProd.close();
        }

        db.close();
        return detalle;
    }
}
