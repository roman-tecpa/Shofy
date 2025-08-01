package com.codedev.shofy;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.adapters.ProductoAdapter;
import com.codedev.shofy.models.EditarProducto;
import com.codedev.shofy.models.Producto;
import com.codedev.shofy.DB.DBProductos;

import java.util.ArrayList;

public class ListaProductos extends Fragment {

    private RecyclerView recyclerView;
    private ProductoAdapter adapter;
    private ArrayList<Producto> listaOriginal;
    private EditText editBuscar;

    public ListaProductos() {
        // Constructor público vacío obligatorio
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_productos, container, false);

        inicializarVista(view);
        cargarProductos();
        configurarFiltro();

        return view;
    }

    private void inicializarVista(View view) {
        recyclerView = view.findViewById(R.id.recyclerProductos);
        editBuscar = view.findViewById(R.id.editBuscarProducto);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void cargarProductos() {
        DBProductos db = new DBProductos(getContext());
        listaOriginal = db.getProductos();

        adapter = new ProductoAdapter(getContext(), new ArrayList<>(listaOriginal), new ProductoAdapter.OnProductoClickListener() {
            @Override
            public void onEditarClick(Producto producto) {
                EditarProducto editarFragment = EditarProducto.newInstance(producto);
                NavController navController = NavHostFragment.findNavController(ListaProductos.this);
                Bundle bundle = new Bundle();
                bundle.putSerializable("producto", producto);
                navController.navigate(R.id.editarProducto, bundle);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void configurarFiltro() {
        editBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String texto = s.toString().trim().toLowerCase();
                ArrayList<Producto> filtrados = new ArrayList<>();
                for (Producto p : listaOriginal) {
                    if (p.getNombre() != null && p.getNombre().toLowerCase().contains(texto)) {
                        filtrados.add(p);
                    }
                }
                adapter.actualizarLista(filtrados);
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }
}