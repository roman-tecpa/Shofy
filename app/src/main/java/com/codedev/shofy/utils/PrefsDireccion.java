package com.codedev.shofy.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.codedev.shofy.DB.DBHelper;

/**
 * Maneja la dirección de envío "actual" del usuario, almacenándola en SharedPreferences
 * con un archivo por usuario (namespacing por id). No altera el esquema de la DB.
 * En Ventas, se persistirá la dirección al llamar a insertarVenta().
 */
public class PrefsDireccion {

    // Donde ya guardas sesión
    private static final String PREF_SESION = "sesion";
    private static final String K_CORREO = "correo";     // ya dijiste que guardas el correo al iniciar sesión

    // Prefs base para direcciones (1 archivo por usuario)
    private static final String PREFS_BASE = "MisPreferencias_Direcciones";
    private static final String CLAVE_DIR  = "direccion_envio";

    /**
     * Obtiene el id del usuario a partir del correo guardado en la sesión.
     * Si no hay correo o no existe en la DB, devuelve 0 (invitado/no logueado).
     */
    public static int getUserId(Context ctx) {
        SharedPreferences s = ctx.getSharedPreferences(PREF_SESION, Context.MODE_PRIVATE);
        String correo = s.getString(K_CORREO, null);
        if (TextUtils.isEmpty(correo)) return 0;

        DBHelper db = new DBHelper(ctx.getApplicationContext());
        int id = db.obtenerIdUsuarioPorCorreo(correo);
        return Math.max(id, 0);
    }

    /** Nombre del archivo de prefs para ESTE usuario. */
    private static String fileNameForUser(int userId) {
        // userId==0 => guest
        return PREFS_BASE + "_uid_" + userId;
    }

    /** SharedPreferences scoped por usuario. */
    private static SharedPreferences userPrefs(Context ctx, int userId) {
        return ctx.getSharedPreferences(fileNameForUser(userId), Context.MODE_PRIVATE);
    }

    public static void guardarDireccion(Context ctx, String direccion) {
        int uid = getUserId(ctx);
        userPrefs(ctx, uid).edit().putString(CLAVE_DIR, direccion).apply();
    }

    public static String obtenerDireccion(Context ctx) {
        int uid = getUserId(ctx);
        return userPrefs(ctx, uid).getString(CLAVE_DIR, null);
    }

    public static void limpiarDireccion(Context ctx) {
        int uid = getUserId(ctx);
        userPrefs(ctx, uid).edit().remove(CLAVE_DIR).apply();
    }
}
