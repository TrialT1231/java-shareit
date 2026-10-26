package ru.practicum.shareit.user;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();
    private long idCounter = 0;

    @Override
    public User save(User user) {
        user.setId(++idCounter);
        users.put(user.getId(), user);
        usersByEmail.put(emailKey(user.getEmail()), user);
        return user;
    }

    @Override
    public User update(User user) {
        User old = users.get(user.getId());
        if (old != null) {
            usersByEmail.remove(emailKey(old.getEmail()));
        }
        users.put(user.getId(), user);
        usersByEmail.put(emailKey(user.getEmail()), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(emailKey(email)));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void delete(Long id) {
        User removed = users.remove(id);
        if (removed != null) {
            usersByEmail.remove(emailKey(removed.getEmail()));
        }
    }

    private String emailKey(String email) {
        return email.toLowerCase();
    }
}