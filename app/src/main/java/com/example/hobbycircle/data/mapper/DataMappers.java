package com.example.hobbycircle.data.mapper;

import com.example.hobbycircle.data.local.EventEntity;
import com.example.hobbycircle.data.local.UserEntity;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.model.User;

import java.util.ArrayList;
import java.util.List;

public final class DataMappers {

    private DataMappers() {
    }

    public static EventEntity toEventEntity(Event event) {
        EventEntity entity = new EventEntity();
        if (event == null) {
            return entity;
        }
        entity.setId(safe(event.getId()));
        entity.setTitle(safe(event.getTitle()));
        entity.setDescription(safe(event.getDescription()));
        entity.setHobbyId(safe(event.getHobbyId()));
        entity.setLocation(safe(event.getLocation()));
        entity.setMapQuery(safe(event.getMapQuery()));
        entity.setEventTimeMillis(event.getEventTimeMillis());
        entity.setCreatedByUserId(safe(event.getCreatedByUserId()));
        entity.setJoinedUserIdsCsv(toCsv(event.getJoinedUserIds()));
        entity.setImageUrl(safe(event.getImageUrl()));
        entity.setUpdatedAtMillis(event.getUpdatedAtMillis() > 0L
                ? event.getUpdatedAtMillis()
                : System.currentTimeMillis());
        return entity;
    }

    public static Event fromEventEntity(EventEntity entity) {
        Event event = new Event();
        if (entity == null) {
            return event;
        }
        event.setId(safe(entity.getId()));
        event.setTitle(safe(entity.getTitle()));
        event.setDescription(safe(entity.getDescription()));
        event.setHobbyId(safe(entity.getHobbyId()));
        event.setLocation(safe(entity.getLocation()));
        event.setMapQuery(safe(entity.getMapQuery()));
        event.setEventTimeMillis(entity.getEventTimeMillis());
        event.setCreatedByUserId(safe(entity.getCreatedByUserId()));
        event.setJoinedUserIds(fromCsv(entity.getJoinedUserIdsCsv()));
        event.setImageUrl(safe(entity.getImageUrl()));
        event.setUpdatedAtMillis(entity.getUpdatedAtMillis());
        return event;
    }

    public static List<Event> fromEventEntities(List<EventEntity> entities) {
        List<Event> events = new ArrayList<>();
        if (entities == null) {
            return events;
        }
        for (EventEntity entity : entities) {
            events.add(fromEventEntity(entity));
        }
        return events;
    }

    public static UserEntity toUserEntity(User user) {
        UserEntity entity = new UserEntity();
        if (user == null) {
            return entity;
        }
        entity.setId(safe(user.getId()));
        entity.setName(safe(user.getName()));
        entity.setEmail(safe(user.getEmail()));
        entity.setRole(safe(user.getRole()));
        entity.setLocation(safe(user.getLocation()));
        entity.setSelectedHobbiesCsv(toCsv(user.getSelectedHobbies()));
        entity.setPhotoUrl(safe(user.getPhotoUrl()));
        entity.setUpdatedAtMillis(user.getUpdatedAtMillis() > 0L
                ? user.getUpdatedAtMillis()
                : System.currentTimeMillis());
        return entity;
    }

    public static User fromUserEntity(UserEntity entity) {
        User user = new User();
        if (entity == null) {
            return user;
        }
        user.setId(safe(entity.getId()));
        user.setName(safe(entity.getName()));
        user.setEmail(safe(entity.getEmail()));
        user.setRole(safe(entity.getRole()));
        user.setLocation(safe(entity.getLocation()));
        user.setSelectedHobbies(fromCsv(entity.getSelectedHobbiesCsv()));
        user.setPhotoUrl(safe(entity.getPhotoUrl()));
        user.setUpdatedAtMillis(entity.getUpdatedAtMillis());
        return user;
    }

    public static String toCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(value.trim());
        }
        return sb.toString();
    }

    public static List<String> fromCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
