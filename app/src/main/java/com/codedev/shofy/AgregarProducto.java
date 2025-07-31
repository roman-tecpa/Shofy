package com.codedev.shofy;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.codedev.shofy.DB.DBProductos;

public class AgregarProducto extends Fragment {

    EditText nombreProducto, descripcionProducto, cantidadActual, cantidadMinima, precioBase;
    Spinner spinnerCategoria;
    Button btnAgregarProducto;

    public AgregarProducto() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agregar_producto, container, false);

        // Referencias a los campos de la vista
        nombreProducto = view.findViewById(R.id.nombreProducto);
        descripcionProducto = view.findViewById(R.id.descripcionProducto);
        cantidadActual = view.findViewById(R.id.cantidadActual);
        cantidadMinima = view.findViewById(R.id.cantidadMinima);
        precioBase = view.findViewById(R.id.precioBase);
        spinnerCategoria = view.findViewById(R.id.tipoProducto);
        btnAgregarProducto = view.findViewById(R.id.btnAgregarProducto);

        // Cargar opciones desde strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.tipos_producto,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);

        btnAgregarProducto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = nombreProducto.getText().toString().trim();
                String descripcion = descripcionProducto.getText().toString().trim();
                String cantidadStr = cantidadActual.getText().toString().trim();
                String minimoStr = cantidadMinima.getText().toString().trim();
                String precioStr = precioBase.getText().toString().trim();
                String categoria = spinnerCategoria.getSelectedItem().toString();

                // Validación de campos vacíos
                if (nombre.isEmpty() || descripcion.isEmpty() || cantidadStr.isEmpty() || minimoStr.isEmpty() || precioStr.isEmpty()) {
                    Toast.makeText(getContext(), "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int cantidad = Integer.parseInt(cantidadStr);
                    int minimo = Integer.parseInt(minimoStr);
                    double precio = Double.parseDouble(precioStr);

                    DBProductos dbProductos = new DBProductos(getContext());
                    long id = dbProductos.registrarProducto(nombre, descripcion, categoria, cantidad, minimo, precio);

                    if (id > 0) {
                        Toast.makeText(getContext(), "Producto agregado con éxito", Toast.LENGTH_SHORT).show();
                        limpiarCampos();
                    } else {
                        Toast.makeText(getContext(), "Error al agregar el producto", Toast.LENGTH_SHORT).show();
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Cantidad, mínimo y precio deben ser numéricos válidos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }




    private void limpiarCampos() {
        nombreProducto.setText("");
        descripcionProducto.setText("");
        cantidadActual.setText("");
        cantidadMinima.setText("");
        precioBase.setText("");
        spinnerCategoria.setSelection(0);
    }
}
