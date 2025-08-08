package com.codedev.shofy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity {

    // ------------------------
    // Campos
    // ------------------------
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;


    // Destinos exclusivos de ADMIN
    private static final Set<Integer> ADMIN_ONLY = new HashSet<>(
            Arrays.asList(
                    R.id.listaProductos,
                    R.id.agregarProducto,
                    R.id.alertasAdmin,
                    R.id.comprasAdmin
            )
    );

    // ------------------------
    // Ciclo de vida
    // ------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // NavController desde el NavHostFragment
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) {
            Log.e("NAV_INIT", "NavHostFragment no encontrado");
            return;
        }
        navController = navHostFragment.getNavController();

        // FAB -> Carrito
        binding.appBarMain.fab.setOnClickListener(view -> {
            try {
                if (navController.getCurrentDestination() == null ||
                        navController.getCurrentDestination().getId() != R.id.carrito) {
                    navController.navigate(R.id.carrito);
                }
            } catch (Exception e) {
                Log.e("FAB_NAV", "Error al navegar al carrito", e);
                Snackbar.make(view, "No se pudo abrir el carrito", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .show();
            }
        });

        // Drawer + NavigationView
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.agregarProducto, R.id.listaProductos, R.id.carrito, R.id.alertasAdmin)
                .setOpenableLayout(drawer)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        inflarMenuPorRol(false);
        binding.getRoot().post(this::actualizarHeaderDrawer);
        handleIntentForEditarProducto(getIntent());
        if (isAdminLoggedIn(this)) {
            verificarStockBajo();
        }

        // Listener del Drawer (logout + gate)
        binding.navView.setNavigationItemSelectedListener(item -> {
            final int itemId = item.getItemId();

            // Logout
            if (itemId == R.id.nav_logout) {
                SharedPreferences preferences = getSharedPreferences("sesion", Context.MODE_PRIVATE);
                preferences.edit().clear().apply();
                try {
                    WorkManager.getInstance(this).cancelUniqueWork("PurgaAlertasWorker");
                    WorkManager.getInstance(this).cancelUniqueWork("LowStockAdminWorker");
                } catch (Exception ignored) {}
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

                binding.drawerLayout.closeDrawers();
                binding.getRoot().postDelayed(() -> {
                    try {
                        if (navController.getCurrentDestination() == null ||
                                navController.getCurrentDestination().getId() != R.id.login) {
                            navController.navigate(R.id.login);
                        }
                    } catch (Exception e) {
                        Log.e("NAV_MENU", "Error navegando a login", e);
                    }
                }, 180);
                return true;
            }

            // Gate admin desde el Drawer
            if (!isAdminLoggedIn(this) && ADMIN_ONLY.contains(itemId)) {
                Toast.makeText(this, "Opción solo para administradores", Toast.LENGTH_SHORT).show();
                binding.drawerLayout.closeDrawers();
                return true;
            }

            // Navegación normal
            binding.drawerLayout.closeDrawers();
            binding.getRoot().postDelayed(() -> {
                try {
                    if (navController.getCurrentDestination() == null ||
                            navController.getCurrentDestination().getId() != itemId) {
                        navController.navigate(itemId);
                    }
                } catch (Exception e) {
                    String name;
                    try {
                        name = getResources().getResourceEntryName(itemId);
                    } catch (Exception ex) {
                        name = String.valueOf(itemId);
                    }
                    Log.e("NAV_MENU", "No pude navegar a " + name, e);
                }
            }, 180);

            return true;
        });

        // Gate global (deeplinks / navigate externos) + Toolbar/FAB
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int id = destination.getId();

            if (!isAdminLoggedIn(this) && ADMIN_ONLY.contains(id)) {
                Toast.makeText(this, "No tienes permisos para acceder aquí", Toast.LENGTH_SHORT).show();
                binding.getRoot().post(() -> {
                    try { navController.navigate(R.id.nav_home); } catch (Exception ignored) {}
                });
                return;
            }

            boolean ocultarToolbar = (id == R.id.login || id == R.id.registro);
            binding.appBarMain.toolbar.setVisibility(ocultarToolbar ? View.GONE : View.VISIBLE);

            actualizarFab(id);
        });

        // Auto-skip del login si ya hay sesión
        binding.getRoot().post(() -> {
            try {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() == R.id.login &&
                        isLoggedIn()) {
                    aplicarRolYRedirigir();
                }
            } catch (Exception ignored) {}
        });

        binding.appBarMain.fab.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentForEditarProducto(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-inflar por si cambió la sesión/rol y refrescar header
        inflarMenuPorRol(true);
        binding.getRoot().post(this::actualizarHeaderDrawer);

        // Purga alertas antiguas y re-checar stock si es admin
        try { new DBHelper(this).borrarAlertasAntiguas24h(); } catch (Exception ignored) {}
        if (isAdminLoggedIn(this)) verificarStockBajo();

        // Refrescar FAB según destino actual
        if (navController != null && navController.getCurrentDestination() != null) {
            actualizarFab(navController.getCurrentDestination().getId());
        }

        // Refrescar badge si existe
        try {
            android.view.MenuItem item = binding.navView.getMenu().findItem(R.id.alertasAdmin);
            if (item != null && item.getActionView() != null) {
                Object r = item.getActionView().getTag();
                if (r instanceof Runnable) ((Runnable) r).run();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // ------------------------
    // Helpers de sesión/rol
    // ------------------------
    private boolean isLoggedIn() {
        SharedPreferences sp = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        return sp.getString("correo", null) != null;
    }

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;
        DBHelper dbh = new DBHelper(ctx);
        int id = dbh.obtenerIdUsuarioPorCorreo(correo);
        return id != 0 && dbh.esAdmin(id);
    }

    // ------------------------
    // UI helpers
    // ------------------------
    private void actualizarHeaderDrawer() {
        if (binding == null || binding.navView == null) {
            Log.w("HEADER", "binding/navView aún no listos");
            return;
        }
        View header;
        try {
            header = binding.navView.getHeaderView(0);
        } catch (Exception e) {
            Log.w("HEADER", "No hay header en el NavigationView todavía", e);
            return;
        }
        if (header == null) return;

        android.widget.TextView tvNombre = header.findViewById(R.id.tvNombreHeader);
        android.widget.TextView tvCorreo = header.findViewById(R.id.tvCorreoHeader);
        android.widget.TextView tvRol    = header.findViewById(R.id.tvRolHeader);

        SharedPreferences sp = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        int idUsuario = sp.getInt("idUsuario", 0);
        String correo = sp.getString("correo", "");

        String nombre = "";
        boolean admin = false;
        try {
            DBHelper dbh = new DBHelper(this);
            if (idUsuario != 0) {
                nombre = dbh.obtenerNombreUsuario(idUsuario);
                admin  = dbh.esAdmin(idUsuario);
            }
        } catch (Exception ignored) {}

        if (tvNombre != null) tvNombre.setText(nombre != null && !nombre.isEmpty() ? nombre : "Invitado");
        if (tvCorreo != null) tvCorreo.setText(correo != null && !correo.isEmpty() ? correo : "—");
        if (tvRol != null)    tvRol.setText(admin ? "Administrador" : "Usuario");
    }

    private void inflarMenuPorRol(boolean conservarSeleccionActual) {
        if (binding == null || binding.navView == null) {
            Log.w("MENU", "binding/navView no listos aún");
            return;
        }

        boolean admin = isAdminLoggedIn(this);

        int checkedItemId = 0;
        if (conservarSeleccionActual && binding.navView.getCheckedItem() != null) {
            checkedItemId = binding.navView.getCheckedItem().getItemId();
        }

        Menu menu = binding.navView.getMenu();
        menu.clear();
        if (admin) {
            getMenuInflater().inflate(R.menu.main_admin, menu);
        } else {
            getMenuInflater().inflate(R.menu.main_user, menu);
        }

        if (checkedItemId != 0 && menu.findItem(checkedItemId) != null) {
            binding.navView.setCheckedItem(checkedItemId);
        }

        configurarBadgeAlertas();
    }

    private void actualizarFab(int destinationId) {
        boolean ocultarFab =
                (destinationId == R.id.carrito
                        || destinationId == R.id.editarProducto
                        || destinationId == R.id.comprasAdmin
                        || destinationId == R.id.listaProductos
                        || destinationId == R.id.agregarProducto
                        || destinationId == R.id.login
                        || destinationId == R.id.registro
                        || destinationId == R.id.alertasAdmin);

        if (ocultarFab) {
            if (binding.appBarMain.fab.getVisibility() == View.VISIBLE) {
                binding.appBarMain.fab.hide();
            }
        } else {
            binding.appBarMain.fab.post(() -> {
                binding.appBarMain.fab.show();
                if (binding.appBarMain.fab.getVisibility() != View.VISIBLE) {
                    binding.appBarMain.fab.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void configurarBadgeAlertas() {
        if (binding == null || binding.navView == null) return;

        try {
            Menu menu = binding.navView.getMenu();
            android.view.MenuItem itemAlertas = menu.findItem(R.id.alertasAdmin);
            if (itemAlertas != null) {
                boolean admin = isAdminLoggedIn(this);
                itemAlertas.setVisible(admin);

                View actionView = itemAlertas.getActionView();
                if (actionView != null) {
                    android.widget.TextView badge = actionView.findViewById(R.id.badge_count);

                    actionView.setOnClickListener(v -> {
                        if (!isAdminLoggedIn(this)) {
                            Toast.makeText(this, "Opción solo para administradores", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (navController.getCurrentDestination() == null ||
                                navController.getCurrentDestination().getId() != R.id.alertasAdmin) {
                            navController.navigate(R.id.alertasAdmin);
                        }
                    });

                    Runnable refreshBadge = () -> {
                        DBHelper dbh = new DBHelper(this);
                        int n = dbh.contarAlertas();
                        if (badge != null) {
                            if (n > 0) {
                                badge.setText(String.valueOf(Math.min(n, 99)));
                                badge.setVisibility(View.VISIBLE);
                            } else {
                                badge.setVisibility(View.GONE);
                            }
                        }
                    };

                    refreshBadge.run();
                    actionView.setTag(refreshBadge);
                }
            }
        } catch (Exception e) {
            Log.e("BADGE", "Error configurando badge", e);
        }
    }

    // ------------------------
    // Navegación post-login
    // ------------------------
    public void aplicarRolYRedirigir() {
        boolean admin = isAdminLoggedIn(this);
        inflarMenuPorRol(false);

        int destino = admin ? R.id.dashboardKpiFragment : R.id.nav_home;

        androidx.navigation.NavOptions opts = new androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.login, true)
                .setLaunchSingleTop(true)
                .build();
        try {
            if (navController.getCurrentDestination() == null ||
                    navController.getCurrentDestination().getId() != destino) {
                navController.navigate(destino, null, opts);
            }
        } catch (Exception e) {
            Log.e("LOGIN_NAV", "No pude redirigir post-login", e);
        }

        android.view.MenuItem target = binding.navView.getMenu().findItem(destino);
        if (target != null) binding.navView.setCheckedItem(destino);
        binding.drawerLayout.closeDrawers();
    }

    public void refrescarMenuPorRol() {
        inflarMenuPorRol(false);
    }

    private void verificarStockBajo() {
        DBHelper dbh = new DBHelper(this);
        SQLiteDatabase db = dbh.getReadableDatabase();

        long hace6h = System.currentTimeMillis() - 6L * 60L * 60L * 1000L;

        Cursor c = db.rawQuery(
                "SELECT nombre, cantidad_actual, cantidad_minima " +
                        "FROM Productos WHERE cantidad_actual <= cantidad_minima", null
        );

        while (c.moveToNext()) {
            String nombre = c.getString(0);
            int actual = c.getInt(1);
            int minimo = c.getInt(2);

            String titulo = "Stock bajo";
            String mensaje = nombre + " (" + actual + "/" + minimo + ") requiere reabastecimiento";

            Cursor dup = db.rawQuery(
                    "SELECT 1 FROM Alertas WHERE mensaje = ? AND creado_en > ? LIMIT 1",
                    new String[]{mensaje, String.valueOf(hace6h)}
            );
            boolean existe = dup.moveToFirst();
            dup.close();

            if (!existe) {
                dbh.insertarAlerta(titulo, mensaje);
            }
        }

        c.close();
        db.close();
    }

    public void registrarProducto() {
        DBHelper base = new DBHelper(MainActivity.this);
        SQLiteDatabase db = base.getWritableDatabase();

        if (db != null) {
            Toast.makeText(this, "Base de datos creada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al crear la base de datos", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------
    // Intent: abrir EditarProducto
    // ------------------------
    private void handleIntentForEditarProducto(Intent intent) {
        if (intent == null) return;
        boolean abrir = intent.getBooleanExtra("abrir_editar_producto", false);
        if (!abrir) return;

        int idProd = intent.getIntExtra("id_producto_editar", -1);
        if (idProd == -1 || navController == null) return;

        Bundle args = new Bundle();
        args.putInt("idProducto", idProd);

        try {
            navController.navigate(R.id.editarProducto, args);
        } catch (Exception e) {
            binding.getRoot().post(() -> {
                try { navController.navigate(R.id.editarProducto, args); } catch (Exception ignored) {}
            });
        }

        intent.removeExtra("abrir_editar_producto");
        intent.removeExtra("id_producto_editar");
    }
}
