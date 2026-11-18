package ru.practicum.shareit.request.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Краткие данные о вещи, добавленной в ответ на запрос.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemShortDto {
    private Long itemId;
    private String name;
    private Long ownerId;
}