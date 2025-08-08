package com.codedev.shofy;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.codedev.shofy.DB.DBHelper;

public class PurgaAlertasWorker extends Worker {

    public PurgaAlertasWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            DBHelper db = new DBHelper(getApplicationContext());
            db.borrarAlertasAntiguas24h(); // limpia >24h
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
