package com.example.activitiesandviews.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.activitiesandviews.R;
import com.example.activitiesandviews.di.AuthEventBus;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject AuthEventBus authEventBus;

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    /*
     * Destinos donde el BottomNav debe estar visible.
     * Todas las demás pantallas (detalle, sub-pantallas) lo ocultan.
     */
    private static final Set<Integer> BOTTOM_NAV_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.homeFragment,
            R.id.pokemonListFragment,
            R.id.materialDemoFragment
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilita el modo edge-to-edge: el contenido se dibuja detrás de la barra de estado
        // y la barra de navegación. Los insets deben aplicarse manualmente.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        applyWindowInsets();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        setupToolbar();
        setupBottomNavigation();
        observeSessionExpired();
    }

    // ─── WindowInsets ─────────────────────────────────────────────────────────

    /*
     * La Toolbar absorbe el inset superior (barra de estado).
     * El BottomNav absorbe el inset inferior (barra de navegación).
     * Los fragments no necesitan preocuparse por los bordes superior/inferior.
     */
    private void applyWindowInsets() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    // ─── Toolbar ──────────────────────────────────────────────────────────────

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
         * AppBarConfiguration define los destinos "top-level" (sin botón back).
         * Se pasan los IDs de los tabs del BottomNav + loginFragment.
         *
         * Destinos no incluidos aquí → NavigationUI muestra la flecha back
         * automáticamente. No hace falta código en cada Fragment.
         */
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.loginFragment,
                R.id.homeFragment
        ).build();

        // Toolbar sincronizada: título y back gestionados por NavigationUI
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    // ─── BottomNavigation ─────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        /*
         * setupWithNavController conecta el BottomNav al NavController:
         *   - Al tocar un tab: navega al destino cuyo ID coincide con el item
         *   - El tab activo se resalta automáticamente al cambiar de destino
         *   - El ítem seleccionado resalta con colorPrimary del Theme
         */
        NavigationUI.setupWithNavController(bottomNav, navController);

        /*
         * Mostrar u ocultar el BottomNav según el destino actual.
         * OnDestinationChangedListener se llama cada vez que cambia el Fragment.
         */
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (BOTTOM_NAV_DESTINATIONS.contains(destination.getId())) {
                bottomNav.setVisibility(View.VISIBLE);
            } else {
                bottomNav.setVisibility(View.GONE);
            }
        });
    }

    // ─── Session ──────────────────────────────────────────────────────────────

    private void observeSessionExpired() {
        authEventBus.getSessionExpired().observe(this, expired -> {
            if (Boolean.TRUE.equals(expired)) {
                navController.navigate(
                        R.id.auth_nav_graph,
                        null,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                );
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
