package com.codedev.shofy;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.codedev.shofy.DB.DBHelper;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.codedev.shofy.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // Obtener NavController desde NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            Log.e("NAV_INIT", "NavHostFragment no encontrado");
            return;
        }

        // FAB para ir al carrito
        binding.appBarMain.fab.setOnClickListener(view -> {
            try {
                if (navController.getCurrentDestination() == null ||
                        navController.getCurrentDestination().getId() != R.id.carrito) {
                    navController.navigate(R.id.carrito);
                }
            } catch (Exception e) {
                Log.e("FAB_NAV", "Error al navegar al fragment_carrito", e);
                Snackbar.make(view, "No se pudo abrir el carrito", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .show();
            }
        });

        // Configuración del drawer y nav view
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.agregarProducto, R.id.listaProductos, R.id.carrito)
                .setOpenableLayout(drawer)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // ✅ Lógica para detectar clic en "Cerrar sesión"
        binding.navView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_logout) {
                // Borrar sesión
                SharedPreferences preferences = getSharedPreferences("sesion", Context.MODE_PRIVATE);
                preferences.edit().clear().apply();

                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

                // Ir al login
                if (navController.getCurrentDestination() == null ||
                        navController.getCurrentDestination().getId() != R.id.login) {
                    navController.navigate(R.id.login);
                }

                binding.drawerLayout.closeDrawers();
                return true;
            }

            // Navegación normal
            if (navController.getCurrentDestination() == null ||
                    navController.getCurrentDestination().getId() != itemId) {
                navController.navigate(itemId);
            }

            binding.drawerLayout.closeDrawers();
            return true;
        });

        // ✅ Ocultar Toolbar y FAB en login y registro
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean ocultarUI = (id == R.id.login || id == R.id.registro);

            if (ocultarUI) {
                binding.appBarMain.toolbar.setVisibility(View.GONE);
                binding.appBarMain.fab.setVisibility(View.GONE);
            } else {
                binding.appBarMain.toolbar.setVisibility(View.VISIBLE);
                binding.appBarMain.fab.setVisibility(View.VISIBLE);
            }
        });
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

    public void registrarProducto() {
        DBHelper base = new DBHelper(MainActivity.this);
        SQLiteDatabase db = base.getWritableDatabase();

        if (db != null) {
            Toast.makeText(this, "Base de datos creada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al crear la base de datos", Toast.LENGTH_SHORT).show();
        }
    }
}
