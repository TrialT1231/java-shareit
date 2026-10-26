package ru.practicum.shareit.item;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InMemoryItemStorage implements ItemStorage {
    private final Map<Long, Item> items = new HashMap<>();
    private final Map<Long, List<Item>> itemsByOwner = new HashMap<>();
    private long idCounter = 0;

    @Override
    public Item save(Item item) {
        item.setId(++idCounter);
        items.put(item.getId(), item);
        itemsByOwner.computeIfAbsent(item.getOwner().getId(), id -> new ArrayList<>()).add(item);
        return item;
    }

    @Override
    public Item update(Item item) {
        // владелец не меняется, а в индексе лежит тот же объект
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> findByOwnerId(Long ownerId) {
        return new ArrayList<>(itemsByOwner.getOrDefault(ownerId, List.of()));
    }

    @Override
    public List<Item> searchAvailable(String text) {
        String query = text.toLowerCase();
        return items.values().stream()
                .filter(Item::isAvailable)
                .filter(i -> i.getName().toLowerCase().contains(query)
                        || i.getDescription().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }
}