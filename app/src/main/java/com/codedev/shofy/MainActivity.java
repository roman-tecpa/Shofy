package com.codedev.shofy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.WorkManager;

import com.codedev.shofy.DB.DBHelper;
import com.codedev.shofy.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // ------------------------
    // Campos
    // ------------------------
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // Instancia única del helper
    private DBHelper dbHelper;

    private NavController navController;

    // Notificaciones
    private static final String CH_STOCK = "stock_low_channel";

    // Destinos exclusivos de ADMIN
    private static final Set<Integer> ADMIN_ONLY = new HashSet<>(
            Arrays.asList(
                    R.id.dashboardKpiFragment,
                    R.id.listaProductos,
                    R.id.comprasAdmin,
                    R.id.alertasAdmin
            )
    );

    // Pantallas bloqueadas para ADMIN (no debe entrar ahí)
    private static final Set<Integer> STORE_BLOCKED_FOR_ADMIN = new HashSet<>(
            Arrays.asList(
                    R.id.nav_home,
                    R.id.carrito,
                    R.id.detalladoProducto
            )
    );

    // —— NUEVO: Control centralizado de visibilidad del FAB ——
    /** Siempre oculto el FAB en estos destinos (para cualquier rol). */
    private static final Set<Integer> FAB_HIDE_ALWAYS = new HashSet<>(Arrays.asList(
            R.id.dashboardKpiFragment,
            R.id.listaProductos,
            R.id.comprasAdmin,
            R.id.alertasAdmin,
            R.id.carrito,
            R.id.editarProducto,
            R.id.login,
            R.id.registro,
            R.id.agregarProducto
            // Agrega aquí otros destinos donde NO quieres ver el FAB nunca
            // p.ej. R.id.conocenosFragment
    ));

    /** Oculto el FAB solo si es ADMIN en estos destinos. */
    private static final Set<Integer> FAB_HIDE_IF_ADMIN = new HashSet<>(Arrays.asList(
            R.id.nav_home,
            R.id.detalladoProducto
            // Agrega aquí los destinos donde, siendo admin, no debe ver FAB
    ));

    // ------------------------
    // Ciclo de vida
    // ------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        // ===== DB & Notifs =====
        dbHelper = new DBHelper(this);
        try {
            dbHelper.ensureTablaAlertas();      // <-- clave para evitar crash
            dbHelper.borrarAlertasAntiguas24h();// opcional
        } catch (Exception e) {
            Log.e("DB_INIT", "Error asegurando Alertas", e);
        }
        crearCanalNotificaciones();

        // Toolbar action
        binding.appBarMain.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.actions_conocenos) {
                try {
                    if (navController.getCurrentDestination() == null ||
                            navController.getCurrentDestination().getId() != R.id.conocenosFragment) {
                        navController.navigate(R.id.conocenosFragment);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "No pude abrir 'Conócenos'", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // Nav Controller
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
                R.id.nav_home,
                R.id.listaProductos,
                R.id.comprasAdmin,
                R.id.alertasAdmin,
                R.id.dashboardKpiFragment
        )
                .setOpenableLayout(drawer)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        inflarMenuPorRol(false);
        binding.getRoot().post(this::actualizarHeaderDrawer);
        handleIntentForEditarProducto(getIntent());

        if (isAdminLoggedIn(this)) {
            try { verificarStockBajo(); } catch (Exception e) {
                Log.e("STOCK_INIT", "Error verificando stock bajo", e);
            }
        }

        // Drawer listener (logout + gate)
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

            if (!isAdminLoggedIn(this) && ADMIN_ONLY.contains(itemId)) {
                Toast.makeText(this, "Opción solo para administradores", Toast.LENGTH_SHORT).show();
                binding.drawerLayout.closeDrawers();
                return true;
            }

            if (isAdminLoggedIn(this) && STORE_BLOCKED_FOR_ADMIN.contains(itemId)) {
                Toast.makeText(this, "Vista no disponible para administradores", Toast.LENGTH_SHORT).show();
                binding.drawerLayout.closeDrawers();
                binding.getRoot().postDelayed(() -> {
                    try { navController.navigate(R.id.dashboardKpiFragment); } catch (Exception ignored) {}
                }, 180);
                return true;
            }

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

        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int id = destination.getId();

            if (isAdminLoggedIn(this) && STORE_BLOCKED_FOR_ADMIN.contains(id)) {
                Toast.makeText(this, "Redirigido al Dashboard (vista de admin)", Toast.LENGTH_SHORT).show();
                binding.getRoot().post(() -> {
                    try { navController.navigate(R.id.dashboardKpiFragment); } catch (Exception ignored) {}
                });
                return;
            }

            if (!isAdminLoggedIn(this) && ADMIN_ONLY.contains(id)) {
                Toast.makeText(this, "No tienes permisos para acceder aquí", Toast.LENGTH_SHORT).show();
                binding.getRoot().post(() -> {
                    try { navController.navigate(R.id.nav_home); } catch (Exception ignored) {}
                });
                return;
            }

            boolean ocultarToolbar = (id == R.id.login || id == R.id.registro);
            binding.appBarMain.toolbar.setVisibility(ocultarToolbar ? View.GONE : View.VISIBLE);

            // —— clave: el FAB SIEMPRE se decide aquí ——
            actualizarFab(id);
        });

        // Si al abrir la app ya estás logueado y caes en login, redirige y luego ajusta el FAB
        binding.getRoot().post(() -> {
            try {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() == R.id.login &&
                        isLoggedIn()) {
                    aplicarRolYRedirigir();
                } else if (navController.getCurrentDestination() != null) {
                    // Inicializa el FAB acorde al destino actual para evitar “parpadeo”
                    actualizarFab(navController.getCurrentDestination().getId());
                }
            } catch (Exception ignored) {}
        });

        // ❌ Importante: eliminado el show forzado del FAB al final de onCreate()
        // binding.appBarMain.fab.show();
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

        inflarMenuPorRol(true);
        binding.getRoot().post(this::actualizarHeaderDrawer);

        try { dbHelper.borrarAlertasAntiguas24h(); } catch (Exception ignored) {}
        if (isAdminLoggedIn(this)) {
            try { verificarStockBajo(); } catch (Exception e) {
                Log.e("STOCK_RESUME", "Error verificando stock bajo", e);
            }
        }

        if (navController != null && navController.getCurrentDestination() != null) {
            actualizarFab(navController.getCurrentDestination().getId());
        }
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
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.actions_conocenos) {
            try {
                if (navController.getCurrentDestination() == null ||
                        navController.getCurrentDestination().getId() != R.id.conocenosFragment) {
                    navController.navigate(R.id.conocenosFragment);
                }
            } catch (Exception e) {
                Toast.makeText(this, "No pude abrir 'Conócenos'", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // ------------------------
    // Sesión y Roles
    // ------------------------
    private boolean isLoggedIn() {
        SharedPreferences sp = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        return sp.getString("correo", null) != null;
    }

    private boolean isAdminLoggedIn(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE);
        String correo = sp.getString("correo", null);
        if (correo == null) return false;
        int id = dbHelper != null ? dbHelper.obtenerIdUsuarioPorCorreo(correo) : 0;
        return id != 0 && dbHelper.esAdmin(id);
    }

    // ------------------------
    // Header del Drawer
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
            if (idUsuario != 0) {
                nombre = dbHelper.obtenerNombreUsuario(idUsuario);
                admin  = dbHelper.esAdmin(idUsuario);
            }
        } catch (Exception ignored) {}

        if (tvNombre != null) tvNombre.setText(nombre != null && !nombre.isEmpty() ? nombre : "Invitado");
        if (tvCorreo != null) tvCorreo.setText(correo != null && !correo.isEmpty() ? correo : "—");
        if (tvRol != null)    tvRol.setText(admin ? "Administrador" : "Usuario");
    }

    // ------------------------
    // Menú por rol + badge de alertas
    // ------------------------
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
                        int n = 0;
                        try { n = dbHelper.contarAlertas(); } catch (Exception ignored) {}
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

    // —— NUEVO: lógica única para decidir visibilidad del FAB ——
    private void actualizarFab(int destinationId) {
        boolean admin = isAdminLoggedIn(this);

        boolean ocultarFab =
                FAB_HIDE_ALWAYS.contains(destinationId) ||
                        (admin && FAB_HIDE_IF_ADMIN.contains(destinationId));

        if (ocultarFab) {
            binding.appBarMain.fab.hide();
            // Asegura que no ocupe espacio en CoordinatorLayout/AppBar
            binding.appBarMain.fab.setVisibility(View.GONE);
        } else {
            binding.appBarMain.fab.post(() -> {
                binding.appBarMain.fab.show();
                if (binding.appBarMain.fab.getVisibility() != View.VISIBLE) {
                    binding.appBarMain.fab.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    // ------------------------
    // Login/Redirección
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

        // —— clave: después de redirigir, ajusta el FAB al nuevo destino ——
        binding.getRoot().post(() -> {
            if (navController.getCurrentDestination() != null) {
                actualizarFab(navController.getCurrentDestination().getId());
            }
        });
    }

    public void refrescarMenuPorRol() {
        inflarMenuPorRol(false);
    }

    // ------------------------
    // STOCK BAJO (defensivo + notificación)
    // ------------------------
    private void verificarStockBajo() {
        try { dbHelper.ensureTablaAlertas(); } catch (Exception ignored) {}

        long ahora = System.currentTimeMillis();
        long ventanaMs = 6L * 60L * 60L * 1000L; // 6 horas
        long limiteDesde = ahora - ventanaMs;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT nombre, cantidad_actual, cantidad_minima " +
                "FROM Productos WHERE cantidad_actual <= cantidad_minima";

        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                String nombre = c.getString(0);
                int actual = c.getInt(1);
                int minimo = c.getInt(2);

                String titulo = "Stock bajo";
                String mensaje = nombre + " (" + actual + "/" + minimo + ") requiere reabastecimiento";

                if (!existeAlertaReciente(mensaje, limiteDesde)) {
                    // Notificación real (opcional)
                    notificarStockBajo(titulo, mensaje);

                    registrarAlerta(titulo, mensaje);
                }
            }
        } catch (android.database.sqlite.SQLiteException ex) {
            Log.e("Alertas", "Faltaba tabla Alertas. Creando y reintentando...", ex);
            try {
                dbHelper.ensureTablaAlertas();
                if (c != null) c.close();
                c = db.rawQuery(sql, null);
                while (c.moveToNext()) {
                    String nombre = c.getString(0);
                    int actual = c.getInt(1);
                    int minimo = c.getInt(2);
                    String titulo = "Stock bajo";
                    String mensaje = nombre + " (" + actual + "/" + minimo + ") requiere reabastecimiento";
                    if (!existeAlertaReciente(mensaje, limiteDesde)) {
                        notificarStockBajo(titulo, mensaje);
                        registrarAlerta(titulo, mensaje);
                    }
                }
            } catch (Exception ex2) {
                Log.e("Alertas", "Reintento falló", ex2);
            }
        } catch (Exception e) {
            Log.e("Stock", "Error verificando stock bajo", e);
        } finally {
            if (c != null) c.close();
        }
    }

    /** Devuelve true si existe una alerta con el mismo mensaje creada después de 'limiteDesdeMs'. */
    private boolean existeAlertaReciente(String mensaje, long limiteDesdeMs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String q = "SELECT 1 FROM Alertas WHERE mensaje = ? AND creado_en > ? LIMIT 1";
        String[] args = { mensaje, String.valueOf(limiteDesdeMs) };
        Cursor cur = null;
        try {
            cur = db.rawQuery(q, args);
            return cur.moveToFirst();
        } catch (android.database.sqlite.SQLiteException ex) {
            try {
                dbHelper.ensureTablaAlertas();
                if (cur != null) cur.close();
                cur = db.rawQuery(q, args);
                return cur.moveToFirst();
            } catch (Exception ex2) {
                Log.e("Alertas", "existeAlertaReciente falló", ex2);
                return false;
            }
        } finally {
            if (cur != null) cur.close();
        }
    }

    /** Inserta una alerta con el tiempo actual en millis. */
    private void registrarAlerta(String titulo, String mensaje) {
        try {
            SQLiteDatabase dbw = dbHelper.getWritableDatabase();
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put("titulo", titulo);
            cv.put("mensaje", mensaje);
            cv.put("creado_en", System.currentTimeMillis());
            dbw.insert("Alertas", null, cv);
        } catch (Exception e) {
            Log.e("Alertas", "No se pudo registrar alerta", e);
        }
    }

    // ------------------------
    // Notificaciones
    // ------------------------
    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_STOCK,
                    "Alertas de stock bajo",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Notificaciones cuando un producto está por debajo del mínimo");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private void notificarStockBajo(String titulo, String mensaje) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CH_STOCK)
                .setSmallIcon(R.drawable.ic_shofy) // pon tu icono
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        nm.notify(id, b.build());
    }

    // ------------------------
    // Utils de DB (demo)
    // ------------------------
    public void registrarProducto() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
