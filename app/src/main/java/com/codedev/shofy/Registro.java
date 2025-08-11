package com.codedev.shofy;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
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
        btnIrLogin = view.findViewById(R.id.btnIrLogin);

        // Helpers
        dbHelper = new DBHelper(requireContext());
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        // --- Filtros ---
        edtNombre.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        boolean ok = Character.isLetter(c) || Character.isSpaceChar(c);
                        if (!ok) return "";
                    }
                    return null;
                },
                new InputFilter.LengthFilter(60)
        });
        edtNombre.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String limpio = s.toString().replaceAll("\\s{2,}", " ");
                if (!limpio.equals(s.toString())) {
                    int pos = edtNombre.getSelectionStart();
                    edtNombre.setText(new SpannableStringBuilder(limpio));
                    edtNombre.setSelection(Math.min(limpio.length(), pos));
                }
            }
        });

        edtCorreo.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        boolean ok = Character.isLetterOrDigit(c) ||
                                c == '@' || c == '.' || c == '_' || c == '-' || c == '+';
                        if (Character.isWhitespace(c) || !ok) return "";
                    }
                    return null;
                },
                new InputFilter.LengthFilter(80)
        });
        edtCorreo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String limpio = s.toString()
                        .replace("\u00A0","")
                        .replaceAll("\\s+","");
                if (!limpio.equals(s.toString())) {
                    int pos = edtCorreo.getSelectionStart();
                    edtCorreo.setText(new SpannableStringBuilder(limpio));
                    edtCorreo.setSelection(Math.min(limpio.length(), pos));
                }
            }
        });

        // Ojo para contraseña
        final Drawable eye = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_view);
        edtContrasena.setCompoundDrawablesWithIntrinsicBounds(null, null, eye, null);
        edtContrasena.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && eye != null) {
                int right = edtContrasena.getRight() - edtContrasena.getPaddingRight();
                if (event.getRawX() >= right - eye.getIntrinsicWidth()) {
                    togglePasswordVisibility(edtContrasena);
                    edtContrasena.setSelection(edtContrasena.length());
                    return true;
                }
            }
            return false;
        });

        // Validación en vivo
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { validarCampos(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtNombre.addTextChangedListener(watcher);
        edtCorreo.addTextChangedListener(watcher);
        edtContrasena.addTextChangedListener(watcher);
        btnRegistrar.setEnabled(false);

        // Registrar
        btnRegistrar.setOnClickListener(v -> {
            String nombre = safeText(edtNombre);
            String correo = normalizeEmail(safeText(edtCorreo));
            String contrasena = safeText(edtContrasena);

            if (!nombreValido(nombre)) {
                edtNombre.setError("Nombre inválido (solo letras y espacios)");
                return;
            }
            if (!emailValidoFuerte(correo)) {
                edtCorreo.setError("Correo no válido");
                return;
            }
            if (!passwordSimple(contrasena)) {
                edtContrasena.setError("Contraseña inválida");
                return;
            }

            if (dbHelper.correoExiste(correo)) {
                edtCorreo.setError("El correo ya está registrado");
                Toast.makeText(getContext(), "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            values.put("correo", correo.toLowerCase());
            values.put("contrasena", contrasena);
            values.put("rol", "cliente");

            long resultado = db.insert("Usuarios", null, values);
            db.close();

            if (resultado != -1) {
                Toast.makeText(getContext(), "Registro exitoso", Toast.LENGTH_SHORT).show();
                irALogin();
            } else {
                Toast.makeText(getContext(), "Error al registrar", Toast.LENGTH_SHORT).show();
            }
        });

        if (btnIrLogin != null) {
            btnIrLogin.setOnClickListener(v -> irALogin());
        }

        return view;
    }

    private void validarCampos() {
        String nombre = safeText(edtNombre);
        String correo = normalizeEmail(safeText(edtCorreo));
        String pass = safeText(edtContrasena);

        edtNombre.setError(nombreValido(nombre) ? null : "Nombre inválido");
        edtCorreo.setError(emailValidoFuerte(correo) ? null : "Correo no válido");
        edtContrasena.setError(passwordSimple(pass) ? null : "Contraseña inválida");

        btnRegistrar.setEnabled(
                edtNombre.getError() == null &&
                        edtCorreo.getError() == null &&
                        edtContrasena.getError() == null &&
                        !TextUtils.isEmpty(nombre) &&
                        !TextUtils.isEmpty(correo) &&
                        !TextUtils.isEmpty(pass)
        );
    }

    private boolean nombreValido(String nombre) {
        return !TextUtils.isEmpty(nombre) && nombre.matches("^[\\p{L} ]+$");
    }

    private String normalizeEmail(String correo) {
        if (correo == null) return "";
        return correo.trim().toLowerCase().replace("\u00A0","").replaceAll("\\s+","");
    }

    private boolean emailValidoFuerte(String correo) {
        if (TextUtils.isEmpty(correo)) return false;
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) return false;
        if (correo.chars().filter(ch -> ch == '@').count() != 1) return false;
        String[] partes = correo.split("@");
        if (partes.length != 2) return false;
        return !TextUtils.isEmpty(partes[0]) && !TextUtils.isEmpty(partes[1]) && partes[1].contains(".");
    }

    // Nueva validación simple de contraseña
    private boolean passwordSimple(String pass) {
        return !TextUtils.isEmpty(pass) && !pass.contains(" ");
    }

    private String safeText(AppCompatEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private void togglePasswordVisibility(AppCompatEditText et) {
        if (et.getTransformationMethod() == null) {
            et.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        } else if (et.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
            et.setTransformationMethod(null);
        } else {
            et.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        }
    }

    private void irALogin() {
        boolean popped = navController.popBackStack(R.id.login, false);
        if (!popped) {
            navController.navigate(R.id.login);
        }
    }
}
