package com.codedev.shofy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.DB.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AlertasAdmin extends Fragment {

    private RecyclerView rv;
    private AlertasAdapter adapter;
    private DBHelper dbHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // para el botón "Limpiar"
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alertas_admin, container, false);
        rv = v.findViewById(R.id.rvAlertas);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new AlertasAdapter();
        rv.setAdapter(adapter);

        // Inicializa DBHelper una sola vez
        dbHelper = new DBHelper(requireContext());

        cargar();
        return v;
    }

    private void cargar() {
        ArrayList<String[]> data = dbHelper.listarAlertas(); // [id, titulo, mensaje, creado_en]
        adapter.submit(data);

        // refresca menú por si hay badges/contadores
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void confirmarYLimpiarHistorial() {
        int idUsuario = obtenerIdUsuarioSesion(requireContext());
        if (idUsuario == 0) {
            Toast.makeText(requireContext(), "Inicia sesión para limpiar tu historial.", Toast.LENGTH_SHORT).show();
            return;
        }


        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirmar")
                .setMessage("¿Seguro que deseas limpiar todo tu historial de compras?")
                .setPositiveButton("Sí", (d, w) -> {
                    int borrados = dbHelper.eliminarHistorialUsuario(idUsuario);
                    if (borrados >= 0) {
                        Toast.makeText(requireContext(), "Historial limpiado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "No se pudo limpiar el historial", Toast.LENGTH_SHORT).show();
                    }
                    cargar(); // refrescar lista tras limpiar
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int obtenerIdUsuarioSesion(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        int id = sp.getInt("idUsuario", 0);
        if (id == 0) {
            String correo = sp.getString("correo", null);
            if (correo != null) {
                try { id = new DBHelper(ctx).obtenerIdUsuarioPorCorreo(correo); } catch (Exception ignored) {}
            }
        }
        return id;
    }

    static class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.VH> {
        private ArrayList<String[]> items = new ArrayList<>();

        void submit(ArrayList<String[]> d) {
            items = (d != null) ? d : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(android.R.layout.simple_list_item_2, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            String[] row = items.get(pos); // [id, titulo, mensaje, creado_en]
            String titulo = (row.length > 1) ? row[1] : "";
            String mensaje = (row.length > 2) ? row[2] : "";
            String fechaTxt = "";
            try {
                long ts = (row.length > 3) ? Long.parseLong(row[3]) : 0L;
                if (ts > 0) {
                    fechaTxt = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                            .format(new Date(ts));
                }
            } catch (Exception ignored) {}

            ((TextView) h.itemView.findViewById(android.R.id.text1)).setText(titulo);
            ((TextView) h.itemView.findViewById(android.R.id.text2)).setText(
                    fechaTxt.isEmpty() ? mensaje : (mensaje + " · " + fechaTxt)
            );

            // Click opcional
            h.itemView.setOnClickListener(v -> {
                // Ejemplo: navegar o mostrar detalle
                // NavHostFragment.findNavController(AlertasAdmin.this).navigate(R.id.nav_home);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            VH(@NonNull View itemView){ super(itemView); }
        }
    }


}
