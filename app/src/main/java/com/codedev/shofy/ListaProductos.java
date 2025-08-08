package com.codedev.shofy;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codedev.shofy.DB.DBProductos;
import com.codedev.shofy.adapters.ProductoAdapter;
import com.codedev.shofy.models.EditarProducto;
import com.codedev.shofy.models.Producto;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListaProductos extends Fragment {

    private RecyclerView recyclerView;
    private ProductoAdapter adapter;
    private ArrayList<Producto> listaOriginal = new ArrayList<>();
    private EditText editBuscar;

    // Botones (contenedores) de categorías
    private View btnPapeleria, btnSupermercado, btnDrogueria, btnTodos;

    // Estado de filtro
    private String categoriaSeleccionada = "Todos";

    public ListaProductos() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_productos, container, false);

        inicializarVista(view);
        cargarProductos();
        configurarEventos();

        // Primer render
        resaltarCategoria();
        aplicarFiltros();

        return view;
    }

    private void inicializarVista(View view) {
        recyclerView    = view.findViewById(R.id.recyclerProductos);
        editBuscar      = view.findViewById(R.id.editBuscarProducto);
        btnPapeleria    = view.findViewById(R.id.btnPapeleria);
        btnSupermercado = view.findViewById(R.id.btnSupermercado);
        btnDrogueria    = view.findViewById(R.id.btnDrogueria);
        btnTodos        = view.findViewById(R.id.btnTodos);

        // ✅ Lista en una sola columna (vertical)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ProductoAdapter(
                getContext(),
                new ArrayList<>(),
                producto -> {
                    NavController navController = NavHostFragment.findNavController(ListaProductos.this);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("producto", producto);
                    navController.navigate(R.id.editarProducto, bundle);
                }
        );
        recyclerView.setAdapter(adapter);
    }


    private void cargarProductos() {
        DBProductos db = new DBProductos(getContext());
        ArrayList<Producto> data = db.getProductos();
        if (data != null) {
            listaOriginal.clear();
            listaOriginal.addAll(data);
        }
    }

    private void configurarEventos() {
        // Buscar por texto
        editBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { aplicarFiltros(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Categorías
        btnPapeleria.setOnClickListener(v -> { categoriaSeleccionada = "Papelería";   resaltarCategoria(); aplicarFiltros(); });
        btnSupermercado.setOnClickListener(v -> { categoriaSeleccionada = "Supermercado"; resaltarCategoria(); aplicarFiltros(); });
        btnDrogueria.setOnClickListener(v -> { categoriaSeleccionada = "Droguería";   resaltarCategoria(); aplicarFiltros(); });
        btnTodos.setOnClickListener(v -> { categoriaSeleccionada = "Todos";           resaltarCategoria(); aplicarFiltros(); });
    }

    private void aplicarFiltros() {
        String query = editBuscar.getText() != null
                ? normalizar(editBuscar.getText().toString().trim().toLowerCase(Locale.ROOT))
                : "";

        List<Producto> filtrados = new ArrayList<>();
        for (Producto p : listaOriginal) {
            String tipo   = p.getTipo()   != null ? p.getTipo()   : "";
            String nombre = p.getNombre() != null ? p.getNombre() : "";

            // Normalizamos para ignorar acentos en la búsqueda
            String nombreNorm = normalizar(nombre.toLowerCase(Locale.ROOT));
            String tipoNorm   = normalizar(tipo);

            boolean porCategoria = categoriaSeleccionada.equals("Todos")
                    || tipoNorm.equalsIgnoreCase(normalizar(categoriaSeleccionada));

            boolean porTexto = query.isEmpty() || nombreNorm.contains(query);

            if (porCategoria && porTexto) {
                filtrados.add(p);
            }
        }

        // 👇 Aquí usamos tu método tal cual
        adapter.actualizarLista(new ArrayList<>(filtrados));
    }

    private void resaltarCategoria() {
        setCategoriaBackground(btnPapeleria,    "Papelería");
        setCategoriaBackground(btnSupermercado, "Supermercado");
        setCategoriaBackground(btnDrogueria,    "Droguería");
        setCategoriaBackground(btnTodos,        "Todos");
    }

    private void setCategoriaBackground(View view, String cat) {
        if (categoriaSeleccionada.equalsIgnoreCase(cat)) {
            view.setBackgroundResource(R.drawable.bg_category_selected);
        } else {
            view.setBackgroundResource(R.drawable.bg_category);
        }
    }

    // Quita acentos para comparar/buscar de forma más flexible
    private String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
