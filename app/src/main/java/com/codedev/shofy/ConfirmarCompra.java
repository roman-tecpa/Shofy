package com.codedev.shofy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.codedev.shofy.models.DialogCompraRealizada;

public class ConfirmarCompra extends Fragment {

    private TextView txtDireccionConfirmada;
    private Button btnFinalizarCompra;

    private static final String PREFS_NAME = "MisPreferencias";
    private static final String CLAVE_DIRECCION = "direccion_envio";

    public ConfirmarCompra() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirmar_compra, container, false);

        txtDireccionConfirmada = view.findViewById(R.id.txtDireccionConfirmada);
        btnFinalizarCompra = view.findViewById(R.id.btnFinalizarCompra);

        // Mostrar dirección guardada
        SharedPreferences preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String direccion = preferences.getString(CLAVE_DIRECCION, null);

        if (direccion != null && !direccion.trim().isEmpty()) {
            txtDireccionConfirmada.setText("Dirección de envío: " + direccion);
        } else {
            txtDireccionConfirmada.setText("Dirección de envío: No agregada");
            Toast.makeText(getContext(), "No hay dirección registrada", Toast.LENGTH_SHORT).show();
        }

        // Finalizar compra
        btnFinalizarCompra.setOnClickListener(v -> {
            DialogCompraRealizada dialog = new DialogCompraRealizada();
            dialog.show(getParentFragmentManager(), "compra_realizada");
        });

        return view;
    }
}
