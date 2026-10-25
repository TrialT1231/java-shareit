package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

public final class ItemMapper {
    private ItemMapper() {
    }

    public static ItemDto toItemDto(Item item) {
        // requestId пока null: ItemRequest заполняется в спринте add-item-requests
        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.isAvailable(),
                null
        );
    }

    public static Item toItem(ItemDto dto, User owner) {
        Item item = new Item();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setAvailable(Boolean.TRUE.equals(dto.getAvailable()));
        item.setOwner(owner);
        return item;
    }
}
