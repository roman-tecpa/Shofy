package com.codedev.shofy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.codedev.shofy.DB.DBHelper;

import java.util.concurrent.TimeUnit;

public class Login extends Fragment {

    private AppCompatEditText edtCorreo, edtContrasena;
    private Button btnLogin, btnRegistrar;
    private DBHelper dbHelper;
    private NavController navController;
    private ActivityResultLauncher<String> notifPermissionLauncher;

    // Opcional: lista de destinos solo para admin (tu app ya lo usa)
    private static final java.util.Set<Integer> ADMIN_ONLY = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    R.id.listaProductos,
                    R.id.agregarProducto,
                    R.id.alertasAdmin,
                    R.id.comprasAdmin
            )
    );

    // Rate limit
    private int intentosFallidos = 0;
    private long bloqueoHasta = 0L;

    public Login() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> Toast.makeText(
                        requireContext(),
                        isGranted ? "Permiso de notificaciones concedido" : "Permiso denegado",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        edtCorreo = view.findViewById(R.id.edtCorreo);
        edtContrasena = view.findViewById(R.id.edtContrasena);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);
        dbHelper = new DBHelper(requireContext());
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        // Seguridad visual (opcional): bloquea screenshots/grabación en esta pantalla
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        // --- Seguridad de CORREO: filtros de caracteres y longitud ---
        edtCorreo.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    // Bloquea espacios y caracteres no típicos del email
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

        // Sanitiza pegados/elimina espacios invisibles
        edtCorreo.addTextChangedListener(new TextWatcher() {
            private String previo = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { previo = s.toString(); }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String limpio = s.toString()
                        .replace("\u00A0","")
                        .replaceAll("\\s+",""); // quita blancos
                if (!limpio.equals(s.toString())) {
                    int pos = edtCorreo.getSelectionStart();
                    edtCorreo.setText(new SpannableStringBuilder(limpio));
                    int newPos = Math.min(limpio.length(), Math.max(0, pos - (previo.length() - limpio.length())));
                    edtCorreo.setSelection(newPos);
                }
            }
        });

        // --- Ojo de contraseña (sin cambiar layout) ---
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

        // Validación en vivo + botón deshabilitado
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validarCamposSeguros(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        edtCorreo.addTextChangedListener(watcher);
        edtContrasena.addTextChangedListener(watcher);
        btnLogin.setEnabled(false);

        // --- Login ---
        btnLogin.setOnClickListener(v -> {
            long ahora = SystemClock.elapsedRealtime();
            if (ahora < bloqueoHasta) {
                long resta = (bloqueoHasta - ahora) / 1000;
                Toast.makeText(getContext(), "Espera " + resta + "s para volver a intentar", Toast.LENGTH_SHORT).show();
                return;
            }

            String correo = texto(edtCorreo).toLowerCase();
            String contrasena = texto(edtContrasena);

            if (!emailValidoFuerte(correo)) {
                edtCorreo.setError("Correo no válido");
                return;
            }
            if (contrasena.isEmpty()) {
                edtContrasena.setError("La contraseña es obligatoria");
                return;
            }

            int idUsuario = dbHelper.validarUsuario(correo, contrasena);
            if (idUsuario != 0) {
                intentosFallidos = 0;
                boolean esAdmin = dbHelper.esAdmin(idUsuario);
                String nombre = dbHelper.obtenerNombreUsuario(idUsuario);
                guardarSesion(idUsuario, correo);

                if (esAdmin) {
                    programarWorkerStockAdmin();
                    programarWorkerPurgaAlertas();
                    solicitarPermisoNotificacionesSiHaceFalta();
                    Toast.makeText(getContext(), "Bienvenido administrador, " + nombre, Toast.LENGTH_SHORT).show();
                } else {
                    cancelarWorkerStockAdmin();
                    Toast.makeText(getContext(), "Bienvenido, " + nombre, Toast.LENGTH_SHORT).show();
                }

                if (isAdded() && requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).aplicarRolYRedirigir();
                }
            } else {
                intentosFallidos++;
                if (intentosFallidos >= 5) {
                    bloqueoHasta = SystemClock.elapsedRealtime() + 30_000; // 30s
                    intentosFallidos = 0;
                    Toast.makeText(getContext(), "Demasiados intentos. Espera 30s.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnRegistrar.setOnClickListener(v -> navController.navigate(R.id.registro));

        return view;
    }

    /* ===================== Utilidades de seguridad ===================== */

    private void togglePasswordVisibility(AppCompatEditText et) {
        TransformationMethod tm = et.getTransformationMethod();
        if (tm instanceof PasswordTransformationMethod) {
            et.setTransformationMethod(null); // mostrar
        } else {
            et.setTransformationMethod(PasswordTransformationMethod.getInstance()); // ocultar
        }
    }

    private boolean emailValidoFuerte(String correo) {
        if (correo == null) return false;
        correo = correo.trim().toLowerCase()
                .replace("\u00A0","")
                .replaceAll("\\s+","");

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) return false;

        // Reglas extra
        if (correo.chars().filter(ch -> ch == '@').count() != 1) return false;
        String[] partes = correo.split("@");
        if (partes.length != 2) return false;

        String local = partes[0];
        String domain = partes[1];
        if (TextUtils.isEmpty(local) || TextUtils.isEmpty(domain)) return false;
        if (local.startsWith(".") || local.endsWith(".")) return false;
        if (domain.startsWith(".") || domain.endsWith(".")) return false;
        if (!domain.contains(".")) return false; // requiere TLD

        return true;
    }

    private void validarCamposSeguros() {
        String correo = texto(edtCorreo);
        String pass = texto(edtContrasena);

        if (!emailValidoFuerte(correo)) {
            edtCorreo.setError("Correo no válido");
        } else {
            edtCorreo.setError(null);
        }
        if (pass.isEmpty()) {
            edtContrasena.setError("Ingresa tu contraseña");
        } else {
            edtContrasena.setError(null);
        }
        btnLogin.setEnabled(edtCorreo.getError() == null && edtContrasena.getError() == null);
    }

    private String texto(AppCompatEditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }

    private void guardarSesion(int idUsuario, String correo) {
        SharedPreferences preferences = requireActivity().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        preferences.edit()
                .putInt("idUsuario", idUsuario)
                .putString("correo", correo)
                .apply();
    }

    private void programarWorkerStockAdmin() {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                com.codedev.shofy.workers.AdminLowStockWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "LowStockAdminWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );
    }

    private void programarWorkerPurgaAlertas() {
        androidx.work.PeriodicWorkRequest req = new androidx.work.PeriodicWorkRequest.Builder(
                com.codedev.shofy.PurgaAlertasWorker.class,
                24, TimeUnit.HOURS
        ).build();

        androidx.work.WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "PurgaAlertasWorker",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                req
        );
    }

    private void cancelarWorkerStockAdmin() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("LowStockAdminWorker");
    }

    private void solicitarPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /* ===================== Limpieza ===================== */

    @Override
    public void onPause() {
        super.onPause();
        if (edtContrasena != null && edtContrasena.getText() != null) {
            edtContrasena.getText().clear(); // limpia password al salir
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Quita FLAG_SECURE si no quieres bloquear screenshots en otras pantallas
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
