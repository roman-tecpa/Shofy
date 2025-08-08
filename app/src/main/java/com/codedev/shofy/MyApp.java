package com.codedev.shofy;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MyApp extends Application {
    public static final String CHANNEL_STOCK_ID = "stock_alerts";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_STOCK_ID,
                    "Alertas de Stock",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Notifica cuando el stock está en o por debajo del mínimo");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
