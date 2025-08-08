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

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.DB.DBProductos;
import com.codedev.shofy.R;
import com.codedev.shofy.utils.NotificationUtils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

public class EditarProducto extends Fragment {

    private EditText etNombre, etDescripcion, etCantidadActual, etCantidadMinima, etPrecioBase;
    private Spinner spinnerTipo;
    private Button btnGuardar, btnCancelar;

    private Producto producto; // el que estamos editando (cargado por id o por argumento serializado)

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

        // Vinculación de vistas
        etNombre = view.findViewById(R.id.editNombreProducto);
        etDescripcion = view.findViewById(R.id.editDescripcionProducto);
        etCantidadActual = view.findViewById(R.id.editCantidadActual);
        etCantidadMinima = view.findViewById(R.id.editCantidadMinima);
        etPrecioBase = view.findViewById(R.id.editPrecioBase);
        spinnerTipo = view.findViewById(R.id.editTipoProducto);
        btnGuardar = view.findViewById(R.id.btnGuardarCambios);
        btnCancelar = view.findViewById(R.id.btnCancelarEdicion);

        // Adaptador del spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.tipos_producto,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(adapter);

        // Cargar producto ya sea por objeto serializado o por idProducto (desde la notificación)
        cargarProductoDesdeArgumentos(adapter);

        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnCancelar.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void cargarProductoDesdeArgumentos(ArrayAdapter<CharSequence> adapter) {
        DBProductos db = new DBProductos(requireContext());

        if (getArguments() != null) {
            // 1) Prioridad: si viene idProducto (desde la notificación)
            int idFromNoti = getArguments().getInt("idProducto", -1);
            if (idFromNoti != -1) {
                producto = db.getProductoById(idFromNoti);
            }

            // 2) Si no vino idProducto o no se encontró, intenta con el objeto serializado
            if (producto == null) {
                Producto pArg = (Producto) getArguments().getSerializable("producto");
                if (pArg != null) {
                    // Refrescar desde DB por si cambió algo
                    Producto pDb = db.getProductoById(pArg.getId());
                    producto = (pDb != null) ? pDb : pArg;
                }
            }
        }

        if (producto == null) {
            Toast.makeText(requireContext(), "No se pudo cargar el producto", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        // Poblar UI
        etNombre.setText(producto.getNombre());
        etDescripcion.setText(producto.getDescripcion());
        etCantidadActual.setText(String.valueOf(producto.getCantidad_actual()));
        etCantidadMinima.setText(String.valueOf(producto.getCantidad_minima()));
        etPrecioBase.setText(String.valueOf(producto.getPrecioBase()));

        int spinnerPos = adapter.getPosition(producto.getTipo());
        if (spinnerPos >= 0) {
            spinnerTipo.setSelection(spinnerPos);
        } else {
            // Si el tipo guardado no está en el array, dejamos el actual del spinner y avisamos
            Toast.makeText(requireContext(), "Tipo de producto no válido, selecciona uno.", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarCambios() {
        String nombre = etNombre.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String cantidadStr = etCantidadActual.getText().toString().trim();
        String minimoStr = etCantidadMinima.getText().toString().trim();
        String precioStr = etPrecioBase.getText().toString().trim();
        String tipo = spinnerTipo.getSelectedItem() != null ? spinnerTipo.getSelectedItem().toString() : "";

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(descripcion) ||
                TextUtils.isEmpty(cantidadStr) || TextUtils.isEmpty(minimoStr) || TextUtils.isEmpty(precioStr) ||
                TextUtils.isEmpty(tipo)) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadStr);
            int minimo = Integer.parseInt(minimoStr);
            double precio = Double.parseDouble(precioStr);

            DBProductos db = new DBProductos(requireContext());
            boolean actualizado = db.actualizarProducto(producto.getId(), nombre, descripcion, tipo, cantidad, minimo, precio);

            if (actualizado) {
                // Actualizamos el objeto local para reflejar cambios
                producto.setNombre(nombre);
                producto.setDescripcion(descripcion);
                producto.setTipo(tipo);
                producto.setCantidad_actual(cantidad);
                producto.setCantidad_minima(minimo);
                producto.setPrecioBase(precio);

                // Si el stock quedó <= mínimo y el admin está logueado, notifica con el nombre del producto
                if (db.productoEstaBajoMinimo(producto.getId()) && isAdminLoggedIn(requireContext())) {
                    ArrayList<Producto> lista = new ArrayList<>();
                    // podríamos traerlo de DB para info más fresca
                    Producto pRefrescado = db.getProductoById(producto.getId());
                    lista.add(pRefrescado != null ? pRefrescado : producto);
                    NotificationUtils.notifyLowStock(requireContext(), lista);
                }

                Toast.makeText(getContext(), "Producto actualizado correctamente", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Verifica los valores numéricos", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Helpers =====

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;

        DBHelper dbh = new DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }
}
