package com.codedev.shofy;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.models.DialogCompraRealizada;
import com.codedev.shofy.utils.PrefsDireccion;

public class ConfirmarCompra extends Fragment {

    private TextView txtDireccionConfirmada;
    private Button btnFinalizarCompra;

    public ConfirmarCompra() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirmar_compra, container, false);

        txtDireccionConfirmada = view.findViewById(R.id.txtDireccionConfirmada);
        btnFinalizarCompra = view.findViewById(R.id.btnFinalizarCompra);

        // 1) Mostrar dirección guardada del usuario actual
        String direccion = PrefsDireccion.obtenerDireccion(requireContext());
        if (!TextUtils.isEmpty(direccion)) {
            txtDireccionConfirmada.setText("Dirección de envío: " + direccion);
        } else {
            txtDireccionConfirmada.setText("Dirección de envío: No agregada");
            Toast.makeText(requireContext(), "No hay dirección registrada para este usuario", Toast.LENGTH_SHORT).show();
        }

        // 2) Finalizar compra: valida login + dirección, inserta Venta con direccion_envio
        btnFinalizarCompra.setOnClickListener(v -> {
            DBHelper db = new DBHelper(requireContext().getApplicationContext());

            int idUsuario = com.codedev.shofy.utils.SesionUtils.obtenerIdUsuarioDesdeSesion(requireContext(), db);
            if (idUsuario <= 0) {
                Toast.makeText(requireContext(), "Debes iniciar sesión antes de comprar", Toast.LENGTH_LONG).show();
                return;
            }

            String dir = PrefsDireccion.obtenerDireccion(requireContext());
            if (TextUtils.isEmpty(dir)) {
                Toast.makeText(requireContext(), "Agrega una dirección antes de finalizar la compra", Toast.LENGTH_LONG).show();
                return;
            }

            // Inserta la venta con la dirección actual
            long idVenta = db.insertarVenta(idUsuario, dir);
            if (idVenta <= 0) {
                Toast.makeText(requireContext(), "No se pudo registrar la venta", Toast.LENGTH_LONG).show();
                return;
            }

            // TODO: aquí inserta los renglones en DetalleVentas con tu carrito actual.
            //       Por ejemplo:
            // for (ItemCarrito item : CarritoManager.getItems()) {
            //     db.insertarDetalleVenta(idVenta, item.getIdProducto(), item.getCantidad(), item.getPrecioUnitario());
            // }
            // (Si no tienes ese método aún, te lo paso en seguida.)

            DialogCompraRealizada dialog = new DialogCompraRealizada();
            dialog.show(getParentFragmentManager(), "compra_realizada");
        });

        return view;
    }
}
