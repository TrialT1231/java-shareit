package ru.practicum.shareit.booking.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookItemRequestDtoTest {
    private static Validator validator;

    @Autowired
    private JacksonTester<BookItemRequestDto> json;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void futureDates_haveNoViolations() {
        BookItemRequestDto dto = new BookItemRequestDto(10L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void endInPast_isViolation() {
        BookItemRequestDto dto = new BookItemRequestDto(10L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().minusDays(1));

        assertThat(validator.validate(dto)).hasSize(1);
    }

    @Test
    void startInPast_isViolation() {
        BookItemRequestDto dto = new BookItemRequestDto(10L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2));

        assertThat(validator.validate(dto)).hasSize(1);
    }

    @Test
    void deserialize_parsesIsoDates() throws Exception {
        String content = "{\"itemId\":10,\"start\":\"2030-01-01T10:00:00\",\"end\":\"2030-01-02T10:00:00\"}";

        BookItemRequestDto dto = json.parseObject(content);

        assertThat(dto.getItemId()).isEqualTo(10L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 1, 10, 0, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 2, 10, 0, 0));
    }
}