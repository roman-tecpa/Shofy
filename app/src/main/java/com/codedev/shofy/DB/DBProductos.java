package com.codedev.shofy.DB;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.codedev.shofy.models.Producto;
import com.codedev.shofy.utils.NotificationUtils;

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

    public boolean existeProducto(String nombre) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM Productos WHERE nombre = ?", new String[]{nombre});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        db.close();
        return existe;
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

    /**
     * Actualiza el stock y, si queda en o por debajo del mínimo, notifica SOLO si el admin está logueado.
     */
    // En DBProductos.java
    public boolean actualizarStockProducto(int idProducto, int nuevoStock) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("cantidad_actual", nuevoStock);

        int filas = db.update("Productos", values, "id = ?", new String[]{String.valueOf(idProducto)});
        db.close();

        boolean ok = filas > 0;
        if (ok && isAdminLoggedIn(context) && productoEstaBajoMinimo(idProducto)) {
            Producto p = getProductoById(idProducto);
            if (p != null) {
                com.codedev.shofy.utils.NotificationUtils.notifyLowStockSingle(context, p); // 👈
            }
        }
        return ok;
    }



    // Lista todos los productos en o por debajo del mínimo
    public ArrayList<Producto> getProductosBajoMinimo() {
        ArrayList<Producto> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM Productos WHERE cantidad_actual <= cantidad_minima",
                    null
            );
            if (cursor.moveToFirst()) {
                do {
                    Producto p = new Producto(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getInt(4),
                            cursor.getInt(5),
                            cursor.getDouble(6)
                    );
                    lista.add(p);
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

    // Checa si un producto (por id) está en o por debajo del mínimo
    public boolean productoEstaBajoMinimo(int idProducto) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        boolean bajo = false;
        try {
            c = db.rawQuery(
                    "SELECT cantidad_actual, cantidad_minima FROM Productos WHERE id = ?",
                    new String[]{String.valueOf(idProducto)}
            );
            if (c.moveToFirst()) {
                int actual = c.getInt(0);
                int minimo = c.getInt(1);
                bajo = actual <= minimo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return bajo;
    }

    // (Opcional) Obtener un producto por id para notificar solo ese
    public Producto getProductoById(int idProducto) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        Producto p = null;
        try {
            c = db.rawQuery("SELECT * FROM Productos WHERE id = ? LIMIT 1",
                    new String[]{String.valueOf(idProducto)});
            if (c.moveToFirst()) {
                p = new Producto(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3),
                        c.getInt(4),
                        c.getInt(5),
                        c.getDouble(6)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return p;
    }

    // ===== Helpers privados =====

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;

        DBHelper dbh = new DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }
}
