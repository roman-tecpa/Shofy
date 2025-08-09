// app/src/main/java/com/codedev/shofy/DetalladoProducto.java
package com.codedev.shofy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.codedev.shofy.models.Producto;
import com.codedev.shofy.utils.CarritoManager;

public class DetalladoProducto extends Fragment {

    private Producto producto;
    private int cantidadSeleccionada = 1;
    private int cantidadDisponible = 1;
    private TextView txtCantidad;

    public DetalladoProducto() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detallado_producto, container, false);

        // UI
        TextView txtNombre = view.findViewById(R.id.txtNombreDetalle);
        TextView txtDescripcion = view.findViewById(R.id.txtDescripcionDetalle);
        TextView txtPrecio = view.findViewById(R.id.txtPrecioDetalle);
        ImageView imgTipo = view.findViewById(R.id.imgTipo);
        TextView txtTotalUnidades = view.findViewById(R.id.txtTotalUnidades);
        txtCantidad = view.findViewById(R.id.txtCantidadCompra);
        Button btnSumar = view.findViewById(R.id.btnSumar);
        Button btnRestar = view.findViewById(R.id.btnRestar);
        Button btnAgregarCarrito = view.findViewById(R.id.btnAgregarCarrito);
        Button btnRealizarCompra = view.findViewById(R.id.btnRealizarCompra);

        // Argumentos
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey("producto")) {
                // ✅ Recibir el objeto completo
                producto = (Producto) args.getSerializable("producto");
            } else {
                // Fallback por si en algún lugar envían claves sueltas
                producto = new Producto(
                        args.getInt("id", 0),
                        args.getString("nombre", "N/A"),
                        args.getString("descripcion", "N/A"),
                        args.getString("tipo", "Otro"),
                        args.getInt("Productos totales", 1),
                        args.getInt("Productos mínimos", 1),
                        args.getFloat("precio", 0f)
                );
            }

            if (producto != null) {
                cantidadDisponible = producto.getCantidad_actual();

                txtNombre.setText(producto.getNombre());
                txtDescripcion.setText(producto.getDescripcion());
                txtPrecio.setText(String.format("$%.2f", producto.getPrecioBase()));
                imgTipo.setImageResource(obtenerIconoPorTipo(producto.getTipo()));

                if (cantidadDisponible <= 0) {
                    txtTotalUnidades.setText("Sin stock disponible");
                    txtCantidad.setText("0");
                    btnSumar.setEnabled(false);
                    btnRestar.setEnabled(false);
                    btnAgregarCarrito.setEnabled(false);
                    btnRealizarCompra.setEnabled(false);

                    Toast.makeText(getContext(), "Este producto ya no tiene unidades disponibles.", Toast.LENGTH_LONG).show();
                } else {
                    txtTotalUnidades.setText("Total Unidades: " + cantidadDisponible);
                    txtCantidad.setText(String.valueOf(cantidadSeleccionada));
                }
            } else {
                Toast.makeText(getContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show();
            }
        }

        // Botón ➕
        btnSumar.setOnClickListener(v -> {
            if (cantidadSeleccionada < cantidadDisponible) {
                cantidadSeleccionada++;
                txtCantidad.setText(String.valueOf(cantidadSeleccionada));
            } else {
                Toast.makeText(getContext(), "Máximo permitido: " + cantidadDisponible, Toast.LENGTH_SHORT).show();
            }
        });

        // Botón ➖
        btnRestar.setOnClickListener(v -> {
            if (cantidadSeleccionada > 1) {
                cantidadSeleccionada--;
                txtCantidad.setText(String.valueOf(cantidadSeleccionada));
            }
        });

        // 🛒 Agregar al carrito
        btnAgregarCarrito.setOnClickListener(v -> {
            if (producto != null) {
                int cantidadYaEnCarrito = CarritoManager.getInstancia().obtenerCantidadDeProducto(producto);
                int cantidadTotal = cantidadYaEnCarrito + cantidadSeleccionada;

                if (cantidadTotal <= cantidadDisponible) {
                    CarritoManager.getInstancia().agregarProducto(producto, cantidadSeleccionada);
                    Toast.makeText(getContext(), "Agregado al carrito: " + cantidadSeleccionada + " unidad(es)", Toast.LENGTH_SHORT).show();
                } else if (cantidadTotal == 0) {
                    Toast.makeText(getContext(), "Y no puedes agregar más productos al carrito", Toast.LENGTH_SHORT).show();
                } else {
                    int restantes = cantidadDisponible - cantidadYaEnCarrito;
                    Toast.makeText(getContext(), "Solo puedes agregar " + restantes + " unidad(es) más al carrito.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "⚠ Error: producto no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        // 🧾 Realizar compra (actualiza cantidad sin acumular y navega a Carrito)
        btnRealizarCompra.setOnClickListener(v -> {
            try {
                if (producto != null) {
                    if (cantidadSeleccionada <= producto.getCantidad_actual()) {
                        CarritoManager.getInstancia().actualizarCantidadProducto(producto, cantidadSeleccionada);
                        cantidadSeleccionada = 1;
                        txtCantidad.setText(String.valueOf(cantidadSeleccionada));

                        NavController navController = Navigation.findNavController(v);
                        if (navController.getCurrentDestination() == null ||
                                navController.getCurrentDestination().getId() != R.id.carrito) {
                            navController.navigate(R.id.action_detalladoProducto_to_carrito);
                        }
                    } else {
                        int restantes = producto.getCantidad_actual();
                        Toast.makeText(getContext(), "Solo puedes agregar hasta " + restantes + " unidad(es) al carrito.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Producto no disponible", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error al navegar al carrito", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private int obtenerIconoPorTipo(String tipo) {
        if (tipo == null) return R.drawable.ic_default;
        String t = tipo.trim().toLowerCase(java.util.Locale.ROOT);
        switch (t) {
            case "papelería":
            case "papeleria":   return R.drawable.ic_papeleria;
            case "supermercado":return R.drawable.ic_supermercado;
            case "droguería":
            case "drogueria":   return R.drawable.ic_drogueria;
            default:            return R.drawable.ic_default;
        }
    }

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;

        com.codedev.shofy.DB.DBHelper dbh = new com.codedev.shofy.DB.DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }
}
