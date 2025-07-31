package com.codedev.shofy;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    RecyclerView recyclerView;
    ProductoAdapter adapter;
    ArrayList<Producto> lista;

    public ListaProductos() {
        // Constructor público vacío obligatorio
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla el layout del fragment
        View view = inflater.inflate(R.layout.fragment_lista_productos, container, false);

        recyclerView = view.findViewById(R.id.recyclerProductos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        DBProductos db = new DBProductos(getContext());
        lista = db.getProductos();

        adapter = new ProductoAdapter(getContext(), lista, new ProductoAdapter.OnProductoClickListener() {
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

        return view;
    }
}
