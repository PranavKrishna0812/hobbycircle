package com.example.hobbycircle.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.example.hobbycircle.R;
import com.example.hobbycircle.ui.auth.ProfileActivity;
import com.example.hobbycircle.ui.home.HomeActivity;
import com.example.hobbycircle.ui.home.NearbyEventsActivity;
import com.example.hobbycircle.ui.admin.AdminEventsActivity;
import com.example.hobbycircle.ui.events.CreateEventActivity;
import com.example.hobbycircle.utils.DrawerMenuHelper;
import com.example.hobbycircle.utils.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public abstract class BaseDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnDrawerMenu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.setContentView(R.layout.activity_base_drawer);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnDrawerMenu = findViewById(R.id.btnDrawerMenu);

        getLayoutInflater().inflate(contentLayoutResId(), findViewById(R.id.drawerContentFrame), true);

        navigationView.setNavigationItemSelectedListener(this);
        btnDrawerMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        DrawerMenuHelper.applyRoleVisibility(navigationView, new PreferenceManager(this).isAdmin());
        setDrawerTitle(getDefaultTitle());
    }

    /** Layout placed below the top bar (no DrawerLayout here). */
    @LayoutRes
    protected abstract int contentLayoutResId();

    /** Title shown next to the hamburger. */
    @NonNull
    protected String getDefaultTitle() {
        return "Hobby Circle";
    }

    protected void setDrawerTitle(@NonNull String title) {
        TextView tv = findViewById(R.id.tvDrawerTitle);
        if (tv != null) {
            tv.setText(title);
        }
    }

    protected DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    protected NavigationView getNavigationView() {
        return navigationView;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        Class<? extends FragmentActivity> target = null;

        if (id == R.id.nav_home) {
            Intent i = new Intent(this, HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } else if (id == R.id.nav_profile) {
            target = ProfileActivity.class;
        } else if (id == R.id.nav_nearby_events) {
            target = NearbyEventsActivity.class;
        } else if (id == R.id.nav_create_event) {
            target = CreateEventActivity.class;
        } else if (id == R.id.nav_manage_events) {
            if (!new PreferenceManager(this).isAdmin()) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            target = AdminEventsActivity.class;
        }

        if (target != null && !this.getClass().equals(target)) {
            startActivity(new Intent(this, target));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavigationHeader();
    }

    protected void updateNavigationHeader() {
        if (navigationView != null) {
            android.view.View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvUserInitial = headerView.findViewById(R.id.tvUserInitial);
                TextView tvHeaderName = headerView.findViewById(R.id.tvHeaderName);
                TextView tvHeaderEmail = headerView.findViewById(R.id.tvHeaderEmail);

                PreferenceManager preferenceManager = new PreferenceManager(this);
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                String name = "";
                String email = "";

                if (currentUser != null) {
                    name = currentUser.getDisplayName();
                    email = currentUser.getEmail();
                }
                if (name == null || name.trim().isEmpty()) {
                    name = preferenceManager.getUserName();
                }
                if (email == null || email.trim().isEmpty()) {
                    email = preferenceManager.getUserEmail();
                }

                String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : "User";
                String displayEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "user@example.com";

                if (tvHeaderName != null) tvHeaderName.setText(displayName);
                if (tvHeaderEmail != null) tvHeaderEmail.setText(displayEmail);
                if (tvUserInitial != null && !displayName.isEmpty()) {
                    tvUserInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
                }
            }
        }
    }
}