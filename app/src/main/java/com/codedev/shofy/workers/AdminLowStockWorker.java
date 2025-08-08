package com.codedev.shofy.workers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.DB.DBProductos;
import com.codedev.shofy.models.Producto;
import com.codedev.shofy.utils.NotificationUtils;

import java.util.ArrayList;

public class AdminLowStockWorker extends Worker {

    public static final String PREFS_SESION = "sesion"; // tu SharedPreferences
    public static final String KEY_CORREO = "correo";    // guardas el correo al iniciar sesión

    public AdminLowStockWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // Solo corre si hay admin logueado
        if (!isAdminLoggedIn(ctx)) {
            return Result.success();
        }

        DBProductos dbp = new DBProductos(ctx);
        ArrayList<Producto> bajos = dbp.getProductosBajoMinimo();
        if (!bajos.isEmpty()) {
            NotificationUtils.notifyLowStock(ctx, bajos);
        }

        return Result.success();
    }

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE);
        String correo = sp.getString(KEY_CORREO, null);
        if (correo == null) return false;

        DBHelper dbh = new DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }
}
