package com.example.hobbycircle.utils;

import android.content.Context;
import android.text.TextUtils;

import com.example.hobbycircle.R;

import java.util.Locale;

public final class UserRoleUtil {

    private UserRoleUtil() {
    }

    public static String resolveRole(Context context, String email, String firestoreRole) {
        if (Constants.ROLE_ADMIN.equals(normalizeRole(firestoreRole))) {
            return Constants.ROLE_ADMIN;
        }
        if (isConfiguredAdminEmail(context, email)) {
            return Constants.ROLE_ADMIN;
        }
        return Constants.ROLE_USER;
    }

    public static boolean isAdmin(String role) {
        return Constants.ROLE_ADMIN.equals(normalizeRole(role));
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return Constants.ROLE_USER;
        }
        String normalized = role.trim().toLowerCase(Locale.US);
        if (Constants.ROLE_ADMIN.equals(normalized)) {
            return Constants.ROLE_ADMIN;
        }
        return Constants.ROLE_USER;
    }

    public static boolean isConfiguredAdminEmail(Context context, String email) {
        if (context == null || TextUtils.isEmpty(email)) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.US);
        String[] adminEmails = context.getResources().getStringArray(R.array.admin_emails);
        if (adminEmails == null) {
            return false;
        }
        for (String adminEmail : adminEmails) {
            if (adminEmail == null || adminEmail.trim().isEmpty()) {
                continue;
            }
            if (normalizedEmail.equals(adminEmail.trim().toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }
}
