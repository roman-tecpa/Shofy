package com.codedev.shofy.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.codedev.shofy.MainActivity;
import com.codedev.shofy.MyApp;
import com.codedev.shofy.R;
import com.codedev.shofy.models.Producto;
import com.codedev.shofy.DB.DBHelper; // 👈 Paso 2: usamos DB para guardar alerta

import java.util.List;

public class NotificationUtils {

    // 🔔 Notificación inmediata por producto (ID único por producto) + guarda alerta
    public static void notifyLowStockSingle(Context ctx, Producto p) {
        if (p == null) return;

        int notificationId = 2000 + p.getId();

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("abrir_editar_producto", true);
        intent.putExtra("id_producto_editar", p.getId());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                ctx,
                p.getId(), // requestCode único por producto
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String titulo = "Hay que reabastecer: " + safe(p.getNombre());
        String contenido = safe(p.getNombre()) + " (" + p.getCantidad_actual() + " / min " + p.getCantidad_minima() + ")";

        // ✅ Guardar alerta en BD
        try {
            new DBHelper(ctx).insertarAlerta(titulo, contenido);
        } catch (Exception ignored) {}

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, MyApp.CHANNEL_STOCK_ID)
                .setSmallIcon(R.drawable.ic_stock) // asegúrate de tener este ícono
                .setContentTitle(titulo)
                .setContentText(contenido)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contenido))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(ctx).notify(notificationId, b.build());
    }

    // 📦 Notificación resumen (para el Worker cada 15 min), mostrando nombres + guarda alerta
    public static void notifyLowStock(Context ctx, List<Producto> bajos) {
        if (bajos == null || bajos.isEmpty()) return;

        int summaryId = 1001;

        // Abrir al primer producto bajo
        int idPrimerProducto = bajos.get(0).getId();
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("abrir_editar_producto", true);
        intent.putExtra("id_producto_editar", idPrimerProducto);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                ctx,
                idPrimerProducto,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, MyApp.CHANNEL_STOCK_ID)
                .setSmallIcon(R.drawable.ic_stock)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);

        String titulo;
        String cuerpoParaAlerta; // 👈 lo que guardaremos en la BD

        if (bajos.size() == 1) {
            Producto p = bajos.get(0);
            titulo = "Hay que reabastecer: " + safe(p.getNombre());
            String contenido = safe(p.getNombre()) + " (" + p.getCantidad_actual() + " / min " + p.getCantidad_minima() + ")";
            cuerpoParaAlerta = contenido;

            b.setContentTitle(titulo)
                    .setContentText(contenido)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(contenido));
        } else {
            int n = bajos.size();
            titulo = "Hay que reabastecer productos (" + n + ")";
            if (n <= 4) {
                String nombres = joinNames(bajos, n); // Paracetamol, Colores, ...
                cuerpoParaAlerta = nombres;

                b.setContentTitle(titulo)
                        .setContentText(nombres)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(nombres));
            } else {
                NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
                int mostrar = Math.min(n, 5);
                for (int i = 0; i < mostrar; i++) {
                    Producto p = bajos.get(i);
                    inbox.addLine("• " + safe(p.getNombre()) + " (" + p.getCantidad_actual() + " / min " + p.getCantidad_minima() + ")");
                }
                if (n > 5) inbox.addLine("… +" + (n - 5) + " más");

                cuerpoParaAlerta = joinNames(bajos, 5) + (n > 5 ? " +" + (n - 5) + " más" : "");

                b.setContentTitle(titulo)
                        .setContentText("Revisar inventario")
                        .setStyle(inbox);
            }
        }

        // ✅ Guardar alerta en BD (título + lista corta de productos)
        try {
            new DBHelper(ctx).insertarAlerta(titulo, cuerpoParaAlerta);
        } catch (Exception ignored) {}

        NotificationManagerCompat.from(ctx).notify(summaryId, b.build());
    }

    // ===== Helpers =====
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String joinNames(List<Producto> list, int limit) {
        StringBuilder sb = new StringBuilder();
        int cut = Math.min(limit, list.size());
        for (int i = 0; i < cut; i++) {
            if (i > 0) sb.append(", ");
            sb.append(safe(list.get(i).getNombre()));
        }
        String s = sb.toString();
        return s.length() > 120 ? s.substring(0, 117) + "..." : s;
    }

    public static void cancelAll(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancelAll();
    }
}
