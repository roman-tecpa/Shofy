package com.codedev.shofy;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.DB.DBProductos;
import com.codedev.shofy.adapters.ProductoAdapterHome;
import com.codedev.shofy.models.Producto;

import java.util.ArrayList;

public class Home extends Fragment {

    public Home() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Buscador
        EditText etBuscar = view.findViewById(R.id.etBuscar);

        // RecyclerView
        RecyclerView recycler = view.findViewById(R.id.recyclerProductosHome);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Obtener productos desde la base de datos
        DBProductos dbProductos = new DBProductos(getContext());
        ArrayList<Producto> listaProductos = dbProductos.getProductos();

        // Adaptador
        ProductoAdapterHome adapter = new ProductoAdapterHome(listaProductos, this);
        recycler.setAdapter(adapter);

        // 🔍 Búsqueda en tiempo real
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrar(s.toString().trim());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // Botones de categoría
        LinearLayout btnPapeleria = view.findViewById(R.id.btnPapeleria);
        LinearLayout btnSupermercado = view.findViewById(R.id.btnSupermercado);
        LinearLayout btnDrogueria = view.findViewById(R.id.btnDrogueria);
        LinearLayout btnTodos = view.findViewById(R.id.btnTodos);

        btnTodos.setOnClickListener(v -> {
            adapter.filtrar("");
            etBuscar.setText("");
        });

        btnPapeleria.setOnClickListener(v -> adapter.filtrarPorTipo("Papelería"));
        btnSupermercado.setOnClickListener(v -> adapter.filtrarPorTipo("Supermercado"));
        btnDrogueria.setOnClickListener(v -> adapter.filtrarPorTipo("Droguería"));

        return view;
    }
}
