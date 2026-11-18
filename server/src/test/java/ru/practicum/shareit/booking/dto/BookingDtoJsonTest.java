package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {
    @Autowired
    private JacksonTester<BookingDto> json;
    @Autowired
    private JacksonTester<BookingCreateDto> createJson;

    @Test
    void serialize_writesIsoDatesAndNestedObjects() throws Exception {
        BookingDto dto = new BookingDto(1L, LocalDateTime.of(2026, 11, 20, 10, 0, 0),
                LocalDateTime.of(2026, 11, 21, 10, 0, 0), BookingStatus.WAITING,
                new UserDto(2L, "Ivan", "ivan@mail.ru"),
                new ItemDto(3L, "Drill", "Power drill", true, null));

        JsonContent<BookingDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.start").isEqualTo("2026-11-20T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.end").isEqualTo("2026-11-21T10:00:00");
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");
        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Drill");
    }

    @Test
    void deserialize_createDto_parsesIsoDates() throws Exception {
        String content = "{\"itemId\":10,\"start\":\"2026-11-20T10:00:00\",\"end\":\"2026-11-21T10:00:00\"}";

        BookingCreateDto dto = createJson.parseObject(content);

        assertThat(dto.getItemId()).isEqualTo(10L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2026, 11, 20, 10, 0, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2026, 11, 21, 10, 0, 0));
    }
}