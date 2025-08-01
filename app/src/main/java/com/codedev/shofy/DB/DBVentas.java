package com.codedev.shofy.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.codedev.shofy.models.ItemCarrito;

import java.util.List;

public class DBVentas extends DBHelper {
    Context context;

    public DBVentas(Context context) {
        super(context);
        this.context = context;
    }

    public long registrarVenta(int idUsuario, List<ItemCarrito> items) {
        long idVenta = -1;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Insertar en la tabla Ventas
            ContentValues ventaValues = new ContentValues();
            ventaValues.put("id_usuario", idUsuario);
            idVenta = db.insert("Ventas", null, ventaValues);

            // Insertar en la tabla DetalleVentas
            for (ItemCarrito item : items) {
                ContentValues detalleValues = new ContentValues();
                detalleValues.put("id_venta", idVenta);
                detalleValues.put("id_producto", item.getProducto().getId());
                detalleValues.put("cantidad_vendida", item.getCantidad());

                // Precio total con IVA
                double precioBase = item.getProducto().getPrecioBase();
                double iva = obtenerIVA(item.getProducto().getTipo());
                double precioFinal = precioBase * (1 + iva);
                detalleValues.put("precio_venta", precioFinal);

                db.insert("DetalleVentas", null, detalleValues);
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

    private double obtenerIVA(String tipo) {
        switch (tipo.toLowerCase()) {
            case "supermercado": return 0.16;
            case "papelería": return 0.08;
            case "droguería": return 0.12;
            default: return 0.10;
        }
    }
}
