package com.example.hobbycircle.data.remote;

import androidx.annotation.NonNull;

/**
 * Canonical Firebase Storage paths.
 */
public final class StoragePaths {

    private StoragePaths() {
    }

    public static String userAvatar(@NonNull String userId) {
        return "users/" + userId + "/avatar.jpg";
    }

    public static String eventCover(@NonNull String eventId) {
        return "events/" + eventId + "/cover.jpg";
    }
}
