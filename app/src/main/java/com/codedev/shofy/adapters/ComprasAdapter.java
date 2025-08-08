package com.codedev.shofy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.R;
import com.codedev.shofy.models.CompraResumen;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ComprasAdapter extends RecyclerView.Adapter<ComprasAdapter.VH> {

    public interface OnCompraClick { void onClick(CompraResumen compra); }
    public interface OnDescargarClick { void onDescargar(CompraResumen compra); }

    private final ArrayList<CompraResumen> data;
    private final OnCompraClick listener;
    private final OnDescargarClick descargarListener;

    public ComprasAdapter(ArrayList<CompraResumen> data, OnCompraClick listener, OnDescargarClick descargarListener) {
        this.data = data;
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

        h.txtId.setText("#" + c.getIdVenta());

        // Producto de ejemplo + "+N más"
        String prod = c.getProductoEjemplo() != null ? c.getProductoEjemplo() : "(sin detalle)";
        int extras = Math.max(0, c.getProductosDistintos() - 1);
        h.txtProducto.setText(extras > 0 ? prod + " (+" + extras + " más)" : prod);

        // Fecha boni
        String outFecha = c.getFechaVenta();
        try {
            String raw = c.getFechaVenta().replace('T', ' ');
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            outFecha = out.format(in.parse(raw));
        } catch (Exception ignored) {}
        h.txtFecha.setText(outFecha);

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
        h.txtTotal.setText(nf.format(c.getTotal()));

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(c); });
        h.btnDescargar.setOnClickListener(v -> { if (descargarListener != null) descargarListener.onDescargar(c); });
    }

    @Override public int getItemCount() { return data.size(); }

    public void actualizar(ArrayList<CompraResumen> nuevas) {
        data.clear(); data.addAll(nuevas); notifyDataSetChanged();
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
}
