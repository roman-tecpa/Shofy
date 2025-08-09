package com.codedev.shofy.DB;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "shofy.db";
    public static final int DB_VERSION = 5; // v5: fecha_venta_millis

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

        // Ventas (incluye fecha_venta legado y nueva fecha_venta_millis)
        db.execSQL("CREATE TABLE Ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "id_usuario INTEGER NOT NULL, " +
                "direccion_envio TEXT, " +
                "fecha_venta DATETIME DEFAULT CURRENT_TIMESTAMP, " + // UTC (legado)
                "fecha_venta_millis INTEGER, " +                     // epoch ms (nuevo)
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
        // Migración a v5: añadir fecha_venta_millis y backfill desde fecha_venta (UTC)
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE Ventas ADD COLUMN fecha_venta_millis INTEGER");
            } catch (Exception ignored) { /* ya existe */ }

            // Backfill: convertir TEXT UTC a epoch millis
            db.execSQL(
                    "UPDATE Ventas " +
                            "SET fecha_venta_millis = (strftime('%s', fecha_venta) * 1000) " +
                            "WHERE fecha_venta IS NOT NULL AND (fecha_venta_millis IS NULL OR fecha_venta_millis = 0)"
            );
            return; // no reseteamos si venimos de <5
        }

        // Si algún día quieres reset total, descomenta el bloque siguiente:
        /*
        db.execSQL("DROP TABLE IF EXISTS DetalleVentas");
        db.execSQL("DROP TABLE IF EXISTS Ventas");
        db.execSQL("DROP TABLE IF EXISTS Caja");
        db.execSQL("DROP TABLE IF EXISTS Productos");
        db.execSQL("DROP TABLE IF EXISTS Usuarios");
        db.execSQL("DROP TABLE IF EXISTS Alertas");
        onCreate(db);
        */
    }
    private long startOfDayMillis(Date d) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(d);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long endOfDayMillis(Date d) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(d);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        cal.set(java.util.Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
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
                "SELECT id, titulo, mensaje, creado_en FROM Alertas ORDER BY creado_en DESC", null
        );
        while (c.moveToNext()) {
            list.add(new String[]{
                    String.valueOf(c.getInt(0)),
                    c.getString(1),
                    c.getString(2),
                    String.valueOf(c.getLong(3)) // epoch millis tal cual
            });
        }
        c.close();
        db.close();
        return list;
    }

    // En DBHelper.java
    public int eliminarHistorialUsuario(int idUsuario) {
        SQLiteDatabase db = null;
        Cursor cur = null;
        int filasAfectadas = 0;

        try {
            db = this.getWritableDatabase();
            db.beginTransaction();

            // 1) Obtener IDs de ventas del usuario
            cur = db.rawQuery("SELECT id FROM Ventas WHERE id_usuario = ?", new String[]{ String.valueOf(idUsuario) });

            // Si no hay ventas, nada que borrar
            if (cur.getCount() == 0) {
                db.setTransactionSuccessful();
                return 0;
            }

            // Construir lista de ids para IN (...)
            StringBuilder sb = new StringBuilder();
            List<String> args = new ArrayList<>();
            while (cur.moveToNext()) {
                if (sb.length() > 0) sb.append(",");
                sb.append("?");
                args.add(String.valueOf(cur.getInt(0)));
            }
            String inClause = sb.toString();
            String[] inArgs = args.toArray(new String[0]);

            // 2) Borrar detalle de todas esas ventas
            int detallesBorrados = db.delete("DetalleVentas", "id_venta IN (" + inClause + ")", inArgs);

            // 3) Borrar ventas del usuario
            int ventasBorradas = db.delete("Ventas", "id_usuario = ?", new String[]{ String.valueOf(idUsuario) });

            filasAfectadas = detallesBorrados + ventasBorradas;

            db.setTransactionSuccessful();
        } catch (Exception e) {
            filasAfectadas = -1; // indicar error
        } finally {
            if (cur != null) cur.close();
            if (db != null) db.endTransaction();
            if (db != null && db.isOpen()) db.close();
        }
        return filasAfectadas;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // equivalente a PRAGMA foreign_keys=ON
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
        long hace24h = System.currentTimeMillis() - 24L * 60L * 60L * 1000L;
        SQLiteDatabase db = this.getWritableDatabase();
        int filas = db.delete("Alertas", "creado_en < ?", new String[]{ String.valueOf(hace24h) });
        db.close();
        return filas;
    }

    public void borrarTodasAlertas() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Alertas", null, null);
        db.close();
    }

    // ===== Ventas =====

    /** Inserta una venta con epoch millis correcto. Devuelve id de la venta. */
    public long insertarVenta(int idUsuario, String direccionEnvio) {
        SQLiteDatabase db = this.getWritableDatabase();
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("id_usuario", idUsuario);
        v.put("direccion_envio", direccionEnvio);
        v.put("fecha_venta_millis", System.currentTimeMillis());
        long id = db.insert("Ventas", null, v);
        db.close();
        return id;
    }

    /** Historial por usuario: SIEMPRE devuelve fecha_millis (epoch ms) listo para formatear */
    public Cursor historialComprasPorUsuario(int idUsuario) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT " +
                        "  dv.id                   AS id_detalle, " +
                        "  v.id                    AS id_venta, " +
                        "  COALESCE(v.fecha_venta_millis, strftime('%s', v.fecha_venta) * 1000) AS fecha_millis, " +
                        "  p.id                    AS id_producto, " +
                        "  p.nombre                AS nombre_producto, " +
                        "  dv.cantidad_vendida     AS cantidad, " +
                        "  dv.precio_venta         AS precio_unit, " +
                        "  (dv.cantidad_vendida * dv.precio_venta) AS total_linea, " +
                        "  p.cantidad_actual       AS stock_disponible " +
                        "FROM Ventas v " +
                        "JOIN DetalleVentas dv ON dv.id_venta = v.id " +
                        "JOIN Productos p ON p.id = dv.id_producto " +
                        "WHERE v.id_usuario = ? " +
                        "ORDER BY fecha_millis DESC, dv.id DESC",
                new String[]{ String.valueOf(idUsuario) }
        );
    }

    /** Total gastado por el usuario */
    public double totalGastadoPorUsuario(int idUsuario) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(dv.cantidad_vendida * dv.precio_venta), 0.0) " +
                        "FROM Ventas v " +
                        "JOIN DetalleVentas dv ON dv.id_venta = v.id " +
                        "WHERE v.id_usuario = ?",
                new String[]{ String.valueOf(idUsuario) }
        );
        double total = 0.0;
        if (c.moveToFirst()) total = c.getDouble(0);
        c.close();
        db.close();
        return total;
    }

    // ===== Usuarios =====

    public int validarUsuario(String correo, String contrasena) {
        SQLiteDatabase db = this.getReadableDatabase();
        int idUsuario = 0;
        Cursor cursor = db.rawQuery("SELECT id FROM Usuarios WHERE correo = ? AND contrasena = ?",
                new String[]{ correo, contrasena });
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
                new String[]{ String.valueOf(idUsuario) });
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
                new String[]{ String.valueOf(idUsuario) });
        if (cursor.moveToFirst()) {
            nombre = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return nombre;
    }

    public boolean correoExiste(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM Usuarios WHERE correo = ?", new String[]{ correo });
        boolean existe = cursor.moveToFirst();
        cursor.close();
        db.close();
        return existe;
    }

    /** Obtener id de usuario a partir de su correo */
    public int obtenerIdUsuarioPorCorreo(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        int id = 0;
        Cursor c = db.rawQuery("SELECT id FROM Usuarios WHERE correo = ? LIMIT 1", new String[]{ correo });
        if (c.moveToFirst()) {
            id = c.getInt(0);
        }
        c.close();
        db.close();
        return id;
    }
}
