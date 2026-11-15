package ru.practicum.shareit.booking;

import ru.practicum.shareit.exception.BadRequestException;

/**
 * Значения параметра state в запросах списков бронирований.
 */
public enum BookingState {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED;

    public static BookingState from(String value) {
        try {
            return BookingState.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown state: " + value);
        }
    }
}
