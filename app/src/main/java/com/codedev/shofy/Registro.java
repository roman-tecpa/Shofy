package com.codedev.shofy;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;

import com.codedev.shofy.DB.DBHelper;

public class Registro extends Fragment {

    private AppCompatEditText edtNombre, edtCorreo, edtContrasena;
    private Button btnRegistrar;
    private DBHelper dbHelper;
    private NavController navController;

    public Registro() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registro, container, false);

        edtNombre = view.findViewById(R.id.edtNombre);
        edtCorreo = view.findViewById(R.id.edtCorreo);
        edtContrasena = view.findViewById(R.id.edtContrasena);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);

        dbHelper = new DBHelper(requireContext());
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        btnRegistrar.setOnClickListener(v -> {
            String nombre = edtNombre.getText().toString().trim();
            String correo = edtCorreo.getText().toString().trim();
            String contrasena = edtContrasena.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) || TextUtils.isEmpty(contrasena)) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(getContext(), "Ingresa un correo válido (ej. ejemplo@dominio.com)", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Validar si el correo ya existe
            if (dbHelper.correoExiste(correo)) {
                Toast.makeText(getContext(), "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar usuario en la base de datos
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            values.put("correo", correo);
            values.put("contrasena", contrasena);
            values.put("rol", "cliente"); // Por defecto, rol "cliente"

            long resultado = db.insert("Usuarios", null, values);

            if (resultado != -1) {
                Toast.makeText(getContext(), "Registro exitoso", Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.login); // Regresa al Login
            } else {
                Toast.makeText(getContext(), "Error al registrar", Toast.LENGTH_SHORT).show();
            }

            db.close();
        });

        return view;
    }
}
