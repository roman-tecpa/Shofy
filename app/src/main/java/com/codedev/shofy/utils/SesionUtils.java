package com.codedev.shofy.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.codedev.shofy.DB.DBHelper;

public class SesionUtils {

    private static final String PREF_SESION = "sesion";
    private static final String K_CORREO = "correo";

    /**
     * Retorna el id del usuario actual usando el correo guardado en la sesión.
     * Devuelve 0 si no hay sesión válida.
     */
    public static int obtenerIdUsuarioDesdeSesion(Context ctx, DBHelper db) {
        SharedPreferences s = ctx.getSharedPreferences(PREF_SESION, Context.MODE_PRIVATE);
        String correo = s.getString(K_CORREO, null);
        if (TextUtils.isEmpty(correo)) return 0;
        return db.obtenerIdUsuarioPorCorreo(correo);
    }
}
