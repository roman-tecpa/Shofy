package com.codedev.shofy.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.codedev.shofy.models.Producto;

import java.util.ArrayList;

public class DBProductos extends DBHelper {

    Context context;

    public DBProductos(@Nullable Context context) {
        super(context);
        this.context = context;
    }

    public long registrarProducto(String nombre, String descripcion, String tipo, int cantidad_actual, int cantidad_minima, double precio_base) {
        long id = 0;
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            values.put("descripcion", descripcion);
            values.put("tipo", tipo);
            values.put("cantidad_actual", cantidad_actual);
            values.put("cantidad_minima", cantidad_minima);
            values.put("precio_base", precio_base);

            id = db.insert("Productos", null, values);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return id;
    }

    public boolean eliminarProducto(int id) {
        boolean eliminado = false;
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            int filasAfectadas = db.delete("Productos", "id=?", new String[]{String.valueOf(id)});
            eliminado = filasAfectadas > 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return eliminado;
    }

    public ArrayList<Producto> getProductos() {
        ArrayList<Producto> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM Productos", null);
            if (cursor.moveToFirst()) {
                do {
                    Producto producto = new Producto(
                            cursor.getInt(0), // id
                            cursor.getString(1), // nombre
                            cursor.getString(2), // descripcion
                            cursor.getString(3), // tipo
                            cursor.getInt(4), // cantidad_actual
                            cursor.getInt(5), // cantidad_minima
                            cursor.getDouble(6) // precio_base
                    );
                    lista.add(producto);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return lista;
    }
        public boolean actualizarProducto(int id, String nombre, String descripcion, String tipo,
                                      int cantidadActual, int cantidadMinima, double precioBase) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("descripcion", descripcion);
        values.put("tipo", tipo);
        values.put("cantidad_actual", cantidadActual);
        values.put("cantidad_minima", cantidadMinima);
        values.put("precio_base", precioBase);

        int rows = db.update("Productos", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

}
