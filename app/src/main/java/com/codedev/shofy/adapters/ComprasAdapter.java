package com.codedev.shofy.adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.R;
import com.codedev.shofy.models.CompraResumen;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

public class ComprasAdapter extends RecyclerView.Adapter<ComprasAdapter.VH> {

    public interface OnCompraClick { void onClick(CompraResumen compra); }
    public interface OnDescargarClick { void onDescargar(CompraResumen compra); }

    private final ArrayList<CompraResumen> data;
    private final OnCompraClick listener;
    private final OnDescargarClick descargarListener;

    public ComprasAdapter(ArrayList<CompraResumen> data,
                          OnCompraClick listener,
                          OnDescargarClick descargarListener) {
        this.data = data != null ? data : new ArrayList<>();
        this.listener = listener;
        this.descargarListener = descargarListener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compra_resumen, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CompraResumen c = data.get(position);

        // ID de venta
        h.txtId.setText("#" + c.getIdVenta());

        // “Producto” -> mostramos el cliente (puedes cambiar el label en el layout si quieres)
        String cliente = c.getCliente() != null && !c.getCliente().isEmpty() ? c.getCliente() : "(sin cliente)";
        h.txtProducto.setText(cliente);

        // Fecha: formatear millis a hora local MX (sin desfase)
        h.txtFecha.setText(formatHoraLocalMX(c.getFechaMillis()));

        // Total
        h.txtTotal.setText(String.format(new Locale("es", "MX"), "$%.2f", c.getTotal()));

        // Clicks
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(c); });
        h.btnDescargar.setOnClickListener(v -> { if (descargarListener != null) descargarListener.onDescargar(c); });
    }

    @Override public int getItemCount() { return data.size(); }

    public void actualizar(ArrayList<CompraResumen> nuevas) {
        data.clear();
        if (nuevas != null) data.addAll(nuevas);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtId, txtProducto, txtFecha, txtTotal, btnDescargar;
        VH(@NonNull View itemView) {
            super(itemView);
            txtId        = itemView.findViewById(R.id.txtCompraId);
            txtProducto  = itemView.findViewById(R.id.txtCompraProducto);
            txtFecha     = itemView.findViewById(R.id.txtCompraFecha);
            txtTotal     = itemView.findViewById(R.id.txtCompraTotal);
            btnDescargar = itemView.findViewById(R.id.btnDescargarTicket);
        }
    }

    // ===== Helpers =====
    private static final String Z_MX = "America/Mexico_City";

    private static String formatHoraLocalMX(long epochMs) {
        if (epochMs <= 0) return "";
        if (Build.VERSION.SDK_INT >= 26) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.of(Z_MX));
            return fmt.format(Instant.ofEpochMilli(epochMs));
        } else {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            f.setTimeZone(TimeZone.getTimeZone(Z_MX));
            return f.format(new java.util.Date(epochMs));
        }
    }
}
