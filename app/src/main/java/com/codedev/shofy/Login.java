package com.codedev.shofy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import com.codedev.shofy.DB.DBHelper;

public class Login extends Fragment {

    private AppCompatEditText edtCorreo, edtContrasena;
    private Button btnLogin, btnRegistrar;
    private DBHelper dbHelper;
    private NavController navController;

    public Login() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        edtCorreo = view.findViewById(R.id.edtCorreo);
        edtContrasena = view.findViewById(R.id.edtContrasena);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);

        dbHelper = new DBHelper(requireContext());
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        btnLogin.setOnClickListener(v -> {
            String correo = edtCorreo.getText().toString().trim();
            String contrasena = edtContrasena.getText().toString().trim();

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            int idUsuario = dbHelper.validarUsuario(correo, contrasena);

            if (idUsuario != 0) {
                boolean esAdmin = dbHelper.esAdmin(idUsuario);
                String nombre = dbHelper.obtenerNombreUsuario(idUsuario);
                guardarSesion(idUsuario);
                if (esAdmin) {
                    Toast.makeText(getContext(), "Bienvenido administrador, " + nombre, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Bienvenido, " + nombre, Toast.LENGTH_SHORT).show();
                }

                navController.navigate(R.id.nav_home);
            } else {
                Toast.makeText(getContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }

        });

        btnRegistrar.setOnClickListener(v -> {
            navController.navigate(R.id.registro);
        });

        return view;
    }

    private void guardarSesion(int idUsuario) {
        SharedPreferences preferences = requireActivity().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("idUsuario", idUsuario);
        editor.apply();
    }
}
