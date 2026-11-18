package ru.practicum.shareit.item.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemDetailsDtoJsonTest {
    @Autowired
    private JacksonTester<ItemDetailsDto> json;

    @Test
    void serialize_writesBookingDatesAndComments() throws Exception {
        LocalDateTime last = LocalDateTime.of(2026, 11, 10, 9, 30, 0);
        LocalDateTime next = LocalDateTime.of(2026, 11, 25, 18, 0, 0);
        ItemDetailsDto dto = new ItemDetailsDto(1L, "Drill", "Power drill", true, last, next,
                List.of(new CommentDto(7L, "Nice", "Ivan", last)));

        JsonContent<ItemDetailsDto> result = json.write(dto);

        assertThat(result).extractingJsonPathStringValue("$.lastBooking").isEqualTo("2026-11-10T09:30:00");
        assertThat(result).extractingJsonPathStringValue("$.nextBooking").isEqualTo("2026-11-25T18:00:00");
        assertThat(result).extractingJsonPathStringValue("$.comments[0].authorName").isEqualTo("Ivan");
        assertThat(result).extractingJsonPathStringValue("$.comments[0].created").isEqualTo("2026-11-10T09:30:00");
    }

    @Test
    void deserialize_readsAllFields() throws Exception {
        String content = "{\"id\":1,\"name\":\"Drill\",\"description\":\"Power drill\",\"available\":true,"
                + "\"lastBooking\":null,\"nextBooking\":\"2026-11-25T18:00:00\",\"comments\":[]}";

        ItemDetailsDto dto = json.parseObject(content);

        assertThat(dto.getLastBooking()).isNull();
        assertThat(dto.getNextBooking()).isEqualTo(LocalDateTime.of(2026, 11, 25, 18, 0, 0));
        assertThat(dto.getComments()).isEmpty();
    }
}