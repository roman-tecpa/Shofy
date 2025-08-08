package com.codedev.shofy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.fragment.NavHostFragment;

import com.codedev.shofy.DB.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AlertasAdmin extends Fragment {

    private RecyclerView rv;
    private AlertasAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // para el botón "Limpiar"
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alertas_admin, container, false);
        rv = v.findViewById(R.id.rvAlertas);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        adapter = new AlertasAdapter();
        rv.setAdapter(adapter);
        cargar();
        return v;
    }

    private void cargar() {
        DBHelper db = new DBHelper(requireContext());
        ArrayList<String[]> data = db.listarAlertas(); // [id, titulo, mensaje, creado_en]
        adapter.submit(data);
        // refresca badge del drawer
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    @Override
    public void onCreateOptionsMenu(android.view.Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_alertas, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_limpiar) {
            // Limpia todo y refresca
            DBHelper db = new DBHelper(requireContext());
            db.borrarTodasAlertas();
            cargar();
            // también refresca el badge del drawer
            try {
                android.view.MenuItem mItem = ((MainActivity) requireActivity())
                        .findViewById(R.id.alertasAdmin);
            } catch (Exception ignored) {}
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ===== Adapter simple (simple_list_item_2) =====
    static class AlertasAdapter extends RecyclerView.Adapter<AlertasAdapter.VH> {
        private ArrayList<String[]> items = new ArrayList<>();

        void submit(ArrayList<String[]> d){ items = d; notifyDataSetChanged(); }

        @Override public VH onCreateViewHolder(ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_2, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            String[] row = items.get(pos); // [id, titulo, mensaje, creado_en]
            String titulo = row[1];
            String mensaje = row[2];
            long ts = Long.parseLong(row[3]);
            String fecha = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(ts));
            ((TextView) h.itemView.findViewById(android.R.id.text1)).setText(titulo);
            ((TextView) h.itemView.findViewById(android.R.id.text2)).setText(mensaje + " · " + fecha);

            // Click: si quieres ir a lista de productos o editar, aquí podrías navegar
            h.itemView.setOnClickListener(v -> {
                // Ejemplo: volver a Home
                // NavHostFragment.findNavController(AlertasAdmin.this).navigate(R.id.nav_home);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            VH(View itemView){ super(itemView); }
        }
    }
}
