package com.codedev.shofy.DB;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "shofy.db";
    public static final int DB_VERSION = 4; // ok

    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void resetProductos() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM Productos");
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='Productos'");
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Usuarios
        db.execSQL("CREATE TABLE Usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "correo TEXT UNIQUE NOT NULL, " +
                "contrasena TEXT NOT NULL, " +
                "rol TEXT NOT NULL" +
                ")");

        db.execSQL("INSERT INTO Usuarios (nombre, correo, contrasena, rol) VALUES " +
                "('Admin', 'admin@admin.com', 'admin123', 'admin')");

        // Productos
        db.execSQL("CREATE TABLE Productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE NOT NULL, " +
                "descripcion TEXT NOT NULL, " +
                "tipo TEXT NOT NULL, " +
                "cantidad_actual INTEGER DEFAULT 0, " +
                "cantidad_minima INTEGER NOT NULL, " +
                "precio_base REAL NOT NULL" +
                ")");

        // Ventas
        db.execSQL("CREATE TABLE Ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_usuario INTEGER NOT NULL, " +
                "direccion_envio TEXT, " +
                "fecha_venta DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(id_usuario) REFERENCES Usuarios(id)" +
                ")");

        // DetalleVentas
        db.execSQL("CREATE TABLE DetalleVentas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_venta INTEGER NOT NULL, " +
                "id_producto INTEGER NOT NULL, " +
                "cantidad_vendida INTEGER NOT NULL, " +
                "precio_venta REAL NOT NULL, " +
                "FOREIGN KEY(id_venta) REFERENCES Ventas(id), " +
                "FOREIGN KEY(id_producto) REFERENCES Productos(id)" +
                ")");

        // Caja
        db.execSQL("CREATE TABLE Caja (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "monto_total REAL NOT NULL, " +
                "fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // Alertas
        db.execSQL("CREATE TABLE IF NOT EXISTS Alertas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "titulo TEXT NOT NULL," +
                "mensaje TEXT NOT NULL," +
                "creado_en INTEGER NOT NULL" + // epoch millis
                ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_alertas_creado_en ON Alertas(creado_en)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // ⚠️ Si no te importa perder datos, este enfoque es ok (resetea todo)
        db.execSQL("DROP TABLE IF EXISTS DetalleVentas");
        db.execSQL("DROP TABLE IF EXISTS Ventas");
        db.execSQL("DROP TABLE IF EXISTS Caja");
        db.execSQL("DROP TABLE IF EXISTS Productos");
        db.execSQL("DROP TABLE IF EXISTS Usuarios");
        db.execSQL("DROP TABLE IF EXISTS Alertas");
        // No crees el índice aquí antes de crear la tabla: onCreate lo hará
        onCreate(db);
    }

    // ===== Alertas =====

    public long insertarAlerta(String titulo, String mensaje) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("titulo", titulo);
        v.put("mensaje", mensaje);
        v.put("creado_en", System.currentTimeMillis());
        long id = db.insert("Alertas", null, v);
        db.close();
        return id;
    }

    public java.util.ArrayList<String[]> listarAlertas() {
        java.util.ArrayList<String[]> list = new java.util.ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, titulo, mensaje, creado_en FROM Alertas ORDER BY creado_en DESC", null);
        while (c.moveToNext()) {
            list.add(new String[]{
                    String.valueOf(c.getInt(0)),
                    c.getString(1),
                    c.getString(2),
                    String.valueOf(c.getLong(3))
            });
        }
        c.close();
        db.close();
        return list;
    }

    public int contarAlertas() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM Alertas", null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        db.close();
        return n;
    }

    public int borrarAlertasAntiguas24h() {
        long hace24h = System.currentTimeMillis() - 24L*60L*60L*1000L;
        SQLiteDatabase db = this.getWritableDatabase();
        int filas = db.delete("Alertas", "creado_en < ?", new String[]{String.valueOf(hace24h)});
        db.close();
        return filas;
    }

    public void borrarTodasAlertas() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Alertas", null, null);
        db.close();
    }

    // ===== Usuarios =====

    public int validarUsuario(String correo, String contrasena) {
        SQLiteDatabase db = this.getReadableDatabase();
        int idUsuario = 0;
        Cursor cursor = db.rawQuery("SELECT id FROM Usuarios WHERE correo = ? AND contrasena = ?",
                new String[]{correo, contrasena});
        if (cursor.moveToFirst()) {
            idUsuario = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return idUsuario;
    }

    public boolean esAdmin(int idUsuario) {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean admin = false;
        Cursor cursor = db.rawQuery("SELECT rol FROM Usuarios WHERE id = ?",
                new String[]{String.valueOf(idUsuario)});
        if (cursor.moveToFirst()) {
            String rol = cursor.getString(0);
            admin = rol.equalsIgnoreCase("admin");
        }
        cursor.close();
        db.close();
        return admin;
    }

    public String obtenerNombreUsuario(int idUsuario) {
        SQLiteDatabase db = this.getReadableDatabase();
        String nombre = "";
        Cursor cursor = db.rawQuery("SELECT nombre FROM Usuarios WHERE id = ?",
                new String[]{String.valueOf(idUsuario)});
        if (cursor.moveToFirst()) {
            nombre = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return nombre;
    }

    public boolean correoExiste(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM Usuarios WHERE correo = ?", new String[]{correo});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        db.close();
        return existe;
    }

    /** Obtener id de usuario a partir de su correo */
    public int obtenerIdUsuarioPorCorreo(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        int id = 0;
        Cursor c = db.rawQuery("SELECT id FROM Usuarios WHERE correo = ? LIMIT 1", new String[]{correo});
        if (c.moveToFirst()) {
            id = c.getInt(0);
        }
        c.close();
        db.close();
        return id;
    }
}
