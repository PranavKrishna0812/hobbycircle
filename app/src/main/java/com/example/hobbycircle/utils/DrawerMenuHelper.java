package com.example.hobbycircle.utils;

import android.view.Menu;
import android.view.MenuItem;

import com.example.hobbycircle.R;
import com.google.android.material.navigation.NavigationView;

public final class DrawerMenuHelper {

    private DrawerMenuHelper() {
    }

    public static void applyRoleVisibility(NavigationView navigationView, boolean isAdmin) {
        if (navigationView == null) {
            return;
        }
        Menu menu = navigationView.getMenu();
        MenuItem manageEvents = menu.findItem(R.id.nav_manage_events);
        if (manageEvents != null) {
            manageEvents.setVisible(isAdmin);
        }
    }
}
