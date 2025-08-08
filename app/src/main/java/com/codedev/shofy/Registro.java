package com.codedev.shofy;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.codedev.shofy.DB.DBHelper;

public class Registro extends Fragment {

    private AppCompatEditText edtNombre, edtCorreo, edtContrasena;
    private Button btnRegistrar, btnIrLogin;
    private DBHelper dbHelper;
    private NavController navController;

    public Registro() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro, container, false);

        // UI
        edtNombre = view.findViewById(R.id.edtNombre);
        edtCorreo = view.findViewById(R.id.edtCorreo);
        edtContrasena = view.findViewById(R.id.edtContrasena);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);
        btnIrLogin = view.findViewById(R.id.btnIrLogin); // <-- Asegúrate de tenerlo en el XML

        // Helpers
        dbHelper = new DBHelper(requireContext());
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        // Registrar usuario
        btnRegistrar.setOnClickListener(v -> {
            String nombre = edtNombre.getText() != null ? edtNombre.getText().toString().trim() : "";
            String correo = edtCorreo.getText() != null ? edtCorreo.getText().toString().trim() : "";
            String contrasena = edtContrasena.getText() != null ? edtContrasena.getText().toString().trim() : "";

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) || TextUtils.isEmpty(contrasena)) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(getContext(), "Ingresa un correo válido (ej. ejemplo@dominio.com)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validar si el correo ya existe
            if (dbHelper.correoExiste(correo)) {
                Toast.makeText(getContext(), "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar usuario
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            values.put("correo", correo);
            values.put("contrasena", contrasena);
            values.put("rol", "cliente"); // rol por defecto

            long resultado = db.insert("Usuarios", null, values);
            db.close();

            if (resultado != -1) {
                Toast.makeText(getContext(), "Registro exitoso", Toast.LENGTH_SHORT).show();
                // Ir al login
                irALogin();
            } else {
                Toast.makeText(getContext(), "Error al registrar", Toast.LENGTH_SHORT).show();
            }
        });

        // Ya tengo cuenta -> volver a login
        if (btnIrLogin != null) {
            btnIrLogin.setOnClickListener(v -> irALogin());
        }

        return view;
    }

    /** Navega al login de forma segura: intenta volver en la pila; si no, navega al destino 'login'. */
    private void irALogin() {
        // Si el login está en el back stack, volvemos
        boolean popped = navController.popBackStack(R.id.login, false);
        if (!popped) {
            // Si no estaba en el back stack, navegamos directamente
            navController.navigate(R.id.login);
        }
    }
}
