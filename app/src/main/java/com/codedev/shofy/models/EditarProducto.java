package com.codedev.shofy.models;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.codedev.shofy.DB.DBProductos;
import com.codedev.shofy.R;


public class EditarProducto extends Fragment {

    private EditText etNombre, etDescripcion, etCantidadActual, etCantidadMinima, etPrecioBase;
    private Spinner spinnerTipo;
    private Button btnGuardar, btnCancelar;

    private Producto producto;

    public static EditarProducto newInstance(Producto producto) {
        EditarProducto fragment = new EditarProducto();
        Bundle args = new Bundle();
        args.putSerializable("producto", producto);
        fragment.setArguments(args);
        return fragment;
    }

    public EditarProducto() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editar_producto, container, false);

        // Vinculación de vistas con los nuevos IDs
        etNombre = view.findViewById(R.id.editNombreProducto);
        etDescripcion = view.findViewById(R.id.editDescripcionProducto);
        etCantidadActual = view.findViewById(R.id.editCantidadActual);
        etCantidadMinima = view.findViewById(R.id.editCantidadMinima);
        etPrecioBase = view.findViewById(R.id.editPrecioBase);
        spinnerTipo = view.findViewById(R.id.editTipoProducto);
        btnGuardar = view.findViewById(R.id.btnGuardarCambios);
        btnCancelar = view.findViewById(R.id.btnCancelarEdicion);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.tipos_producto,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapter);

// ...
        if (getArguments() != null) {
            producto = (Producto) getArguments().getSerializable("producto");

            etNombre.setText(producto.getNombre());
            etDescripcion.setText(producto.getDescripcion());
            etCantidadActual.setText(String.valueOf(producto.getCantidad_actual()));
            etCantidadMinima.setText(String.valueOf(producto.getCantidad_minima()));
            etPrecioBase.setText(String.valueOf(producto.getPrecioBase()));

            int spinnerPos = adapter.getPosition(producto.getTipo());
            if (spinnerPos >= 0) {
                spinnerTipo.setSelection(spinnerPos);
            } else {
                Toast.makeText(requireContext(), "Tipo de producto no válido", Toast.LENGTH_SHORT).show();
            }
        }


        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCancelar.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void guardarCambios() {
        String nombre = etNombre.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String cantidadStr = etCantidadActual.getText().toString().trim();
        String minimoStr = etCantidadMinima.getText().toString().trim();
        String precioStr = etPrecioBase.getText().toString().trim();
        String tipo = spinnerTipo.getSelectedItem().toString();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(descripcion) ||
                TextUtils.isEmpty(cantidadStr) || TextUtils.isEmpty(minimoStr) || TextUtils.isEmpty(precioStr)) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadStr);
            int minimo = Integer.parseInt(minimoStr);
            double precio = Double.parseDouble(precioStr);

            DBProductos db = new DBProductos(getContext());
            boolean actualizado = db.actualizarProducto(producto.getId(), nombre, descripcion, tipo, cantidad, minimo, precio);

            if (actualizado) {
                Toast.makeText(getContext(), "Producto actualizado correctamente", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Verifica los valores numéricos", Toast.LENGTH_SHORT).show();
        }
    }
}
