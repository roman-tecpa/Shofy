package com.codedev.shofy;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.DB.DBProductos;
import com.codedev.shofy.adapters.ProductoAdapterHome;
import com.codedev.shofy.models.Producto;

import java.util.ArrayList;

public class Home extends Fragment {

    private static final String STATE_CATEGORIA = "home_state_categoria";
    private static final String STATE_QUERY = "home_state_query";

    private EditText etBuscar;
    private RecyclerView recycler;

    private LinearLayout btnPapeleria, btnSupermercado, btnDrogueria, btnTodos;

    private ProductoAdapterHome adapter;
    private final ArrayList<Producto> listaProductos = new ArrayList<>();

    private String categoriaSeleccionada = "Todos";

    public Home() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        etBuscar = view.findViewById(R.id.etBuscar);
        recycler = view.findViewById(R.id.recyclerProductosHome);
        btnPapeleria = view.findViewById(R.id.btnPapeleria);
        btnSupermercado = view.findViewById(R.id.btnSupermercado);
        btnDrogueria = view.findViewById(R.id.btnDrogueria);
        btnTodos = view.findViewById(R.id.btnTodos);

        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // 1) Cargar datos SIN duplicar
        DBProductos dbProductos = new DBProductos(getContext());
        ArrayList<Producto> data = dbProductos.getProductos();

        listaProductos.clear(); // <- IMPRESCINDIBLE
        if (data != null) listaProductos.addAll(data);

        // 2) No compartas la misma referencia si tu adapter modifica internamente la lista
        adapter = new ProductoAdapterHome(new ArrayList<>(listaProductos), this);
        recycler.setAdapter(adapter);

        if (savedInstanceState != null) {
            categoriaSeleccionada = savedInstanceState.getString(STATE_CATEGORIA, "Todos");
            String q = savedInstanceState.getString(STATE_QUERY, "");
            if (q != null) etBuscar.setText(q);
        }

        resaltarCategoria();

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrar(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnTodos.setOnClickListener(v -> {
            categoriaSeleccionada = "Todos";
            resaltarCategoria();
            adapter.filtrar("");
            etBuscar.setText("");
        });

        btnPapeleria.setOnClickListener(v -> {
            categoriaSeleccionada = "Papelería";
            resaltarCategoria();
            adapter.filtrarPorTipo("Papelería");
        });

        btnSupermercado.setOnClickListener(v -> {
            categoriaSeleccionada = "Supermercado";
            resaltarCategoria();
            adapter.filtrarPorTipo("Supermercado");
        });

        btnDrogueria.setOnClickListener(v -> {
            categoriaSeleccionada = "Droguería";
            resaltarCategoria();
            adapter.filtrarPorTipo("Droguería");
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CATEGORIA, categoriaSeleccionada);
        outState.putString(STATE_QUERY, etBuscar != null && etBuscar.getText() != null ? etBuscar.getText().toString() : "");
    }

    private void resaltarCategoria() {
        setCategoriaBackground(btnPapeleria,    "Papelería");
        setCategoriaBackground(btnSupermercado, "Supermercado");
        setCategoriaBackground(btnDrogueria,    "Droguería");
        setCategoriaBackground(btnTodos,        "Todos");
    }

    private void setCategoriaBackground(View view, String cat) {
        if (view == null) return;
        if (categoriaSeleccionada.equalsIgnoreCase(cat)) {
            view.setBackgroundResource(R.drawable.bg_category_selected);
        } else {
            view.setBackgroundResource(R.drawable.bg_category);
        }
    }
}
