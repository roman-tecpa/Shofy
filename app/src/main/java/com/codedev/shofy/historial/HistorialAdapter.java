package com.codedev.shofy.historial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.VH> {

    public interface OnRecomprarClick {
        void onRecomprar(int idProducto, String nombreProducto);
    }

    public static class Item {
        public int idDetalle;
        public int idVenta;
        public long fechaMillis;       // si parseas a millis; si no, guarda String
        public String fechaTexto;      // fallback
        public int idProducto;
        public String nombreProducto;
        public int cantidad;
        public double precioUnit;
        public double totalLinea;
        public int stockDisponible;
    }

    private final List<Item> data = new ArrayList<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final OnRecomprarClick listener;

    public HistorialAdapter(OnRecomprarClick listener) {
        this.listener = listener;
    }

    public void setData(List<Item> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial_compra, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = data.get(position);
        h.tvProducto.setText(it.nombreProducto);

        String fecha = it.fechaTexto != null ? it.fechaTexto
                : (it.fechaMillis > 0 ? sdf.format(it.fechaMillis) : "—");
        h.tvFecha.setText("Fecha: " + fecha);

        h.tvDetalle.setText("x" + it.cantidad + "  ·  " + currency.format(it.precioUnit) +
                "  =  " + currency.format(it.totalLinea));

        h.tvStock.setText("Stock: " + it.stockDisponible);

        boolean disponible = it.stockDisponible > 0;
        h.btnRecomprar.setEnabled(disponible);
        h.btnRecomprar.setAlpha(disponible ? 1f : 0.5f);
        h.btnRecomprar.setOnClickListener(v ->
        { if (listener != null && disponible) listener.onRecomprar(it.idProducto, it.nombreProducto); });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvProducto, tvFecha, tvDetalle, tvStock;
        Button btnRecomprar;
        VH(@NonNull View v) {
            super(v);
            tvProducto = v.findViewById(R.id.tvProducto);
            tvFecha = v.findViewById(R.id.tvFecha);
            tvDetalle = v.findViewById(R.id.tvDetalle);
            tvStock = v.findViewById(R.id.tvStock);
            btnRecomprar = v.findViewById(R.id.btnRecomprar);
        }
    }

}
