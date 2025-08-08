package com.codedev.shofy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.pm.PackageManager;
import android.os.Build;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;

import com.codedev.shofy.DB.DBHelper;

// WorkManager
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;



public class Login extends Fragment {

    private AppCompatEditText edtCorreo, edtContrasena;
    private Button btnLogin, btnRegistrar;
    private DBHelper dbHelper;
    private NavController navController;
    private ActivityResultLauncher<String> notifPermissionLauncher;


    private static final java.util.Set<Integer> ADMIN_ONLY = new java.util.HashSet<>(
            java.util.Arrays.asList(
                    R.id.listaProductos,
                    R.id.agregarProducto,
                    R.id.alertasAdmin,
                    R.id.comprasAdmin
            )
    );


    public Login() {
        // Constructor vacío requerido
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notifPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // Si quieres, puedes hacer algo cuando se otorgue/deniegue.
                    // Por ejemplo, mostrar un Toast.
                     Toast.makeText(requireContext(), isGranted ? "Permiso de notificaciones concedido" : "Permiso denegado", Toast.LENGTH_SHORT).show();
                }
        );
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
                Toast.makeText(getContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        });


        btnRegistrar.setOnClickListener(v -> {
            navController.navigate(R.id.registro);
        });

        return view;
    }

    private void guardarSesion(int idUsuario, String correo) {
        SharedPreferences preferences = requireActivity().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("idUsuario", idUsuario);
        editor.putString("correo", correo); // <-- NECESARIO para el Worker
        editor.apply();
    }

    private void programarWorkerStockAdmin() {
        // 15 minutos es el mínimo permitido por WorkManager para trabajos periódicos
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
        androidx.work.PeriodicWorkRequest req =
                new androidx.work.PeriodicWorkRequest.Builder(
                        com.codedev.shofy.PurgaAlertasWorker.class,
                        24, java.util.concurrent.TimeUnit.HOURS
                )
                        // Opcional: corre primera vez con un pequeño delay para no chocar al inicio
                        //.setInitialDelay(15, java.util.concurrent.TimeUnit.MINUTES)
                        .build();

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

}
