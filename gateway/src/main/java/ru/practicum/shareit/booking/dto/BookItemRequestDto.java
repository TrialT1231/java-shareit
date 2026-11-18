package ru.practicum.shareit.booking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookItemRequestDto {
    private long itemId;

    @NotNull
    private LocalDateTime start;

    @NotNull
    private LocalDateTime end;

    @AssertTrue(message = "Дата начала не может быть в прошлом")
    public boolean isStartValid() {
        if (start == null) return true;
        // Даем погрешность в 1 минуту для CI и сетевых задержек
        return !start.isBefore(LocalDateTime.now().minusMinutes(1));
    }

    @AssertTrue(message = "Дата окончания не может быть в прошлом")
    public boolean isEndValid() {
        if (end == null) return true;
        return !end.isBefore(LocalDateTime.now().minusMinutes(1));
    }

    @AssertTrue(message = "Дата окончания должна быть позже даты начала")
    public boolean isEndAfterStart() {
        if (start == null || end == null) return true;
        return end.isAfter(start);
    }
}