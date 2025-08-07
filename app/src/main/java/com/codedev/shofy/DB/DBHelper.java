package com.codedev.shofy.DB;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "shofy.db";
    public static final int DB_VERSION = 3; // Incrementado por los nuevos cambios

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
        // Tabla de usuarios
        // Tabla de usuarios con rol
        db.execSQL("CREATE TABLE Usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "correo TEXT UNIQUE NOT NULL, " +
                "contrasena TEXT NOT NULL, " +
                "rol TEXT NOT NULL" +  // <- NUEVO CAMPO
                ")");

        db.execSQL("INSERT INTO Usuarios (nombre, correo, contrasena, rol) VALUES " +
                "('Admin', 'admin@admin.com', 'admin123', 'admin')");



        // Tabla de productos
        db.execSQL("CREATE TABLE Productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT UNIQUE NOT NULL, " +
                "descripcion TEXT NOT NULL, " +
                "tipo TEXT NOT NULL, " +
                "cantidad_actual INTEGER DEFAULT 0, " +
                "cantidad_minima INTEGER NOT NULL, " +
                "precio_base REAL NOT NULL" +
                ")");

        // Tabla de ventas
        // Tabla de ventas
        db.execSQL("CREATE TABLE Ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_usuario INTEGER NOT NULL, " +
                "direccion_envio TEXT, " +  // 👈 Agregado
                "fecha_venta DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(id_usuario) REFERENCES Usuarios(id)" +
                ")");


        // Tabla de detalle de ventas
        db.execSQL("CREATE TABLE DetalleVentas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_venta INTEGER NOT NULL, " +
                "id_producto INTEGER NOT NULL, " +
                "cantidad_vendida INTEGER NOT NULL, " +
                "precio_venta REAL NOT NULL, " +
                "FOREIGN KEY(id_venta) REFERENCES Ventas(id), " +
                "FOREIGN KEY(id_producto) REFERENCES Productos(id)" +
                ")");

        // Tabla de caja
        db.execSQL("CREATE TABLE Caja (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "monto_total REAL NOT NULL, " +
                "fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS DetalleVentas");
        db.execSQL("DROP TABLE IF EXISTS Ventas");
        db.execSQL("DROP TABLE IF EXISTS Caja");
        db.execSQL("DROP TABLE IF EXISTS Productos");
        db.execSQL("DROP TABLE IF EXISTS Usuarios");
        onCreate(db);

    }

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
}
