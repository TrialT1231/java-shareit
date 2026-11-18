package ru.practicum.shareit.item.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemCreateDtoTest {
    private static Validator validator;

    @Autowired
    private JacksonTester<ItemCreateDto> json;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validDto_hasNoViolations() {
        assertThat(validator.validate(new ItemCreateDto("Drill", "Power drill", true, null))).isEmpty();
    }

    @Test
    void blankName_isViolation() {
        assertThat(validator.validate(new ItemCreateDto(" ", "Power drill", true, null))).hasSize(1);
    }

    @Test
    void blankDescription_isViolation() {
        assertThat(validator.validate(new ItemCreateDto("Drill", "", true, null))).hasSize(1);
    }

    @Test
    void nullAvailable_isViolation() {
        assertThat(validator.validate(new ItemCreateDto("Drill", "Power drill", null, null))).hasSize(1);
    }

    @Test
    void deserialize_readsOptionalRequestId() throws Exception {
        String content = "{\"name\":\"Drill\",\"description\":\"Power drill\",\"available\":true,\"requestId\":5}";

        ItemCreateDto dto = json.parseObject(content);

        assertThat(dto.getRequestId()).isEqualTo(5L);
        assertThat(dto.getAvailable()).isTrue();
    }

    @Test
    void deserialize_withoutRequestId_leavesNull() throws Exception {
        ItemCreateDto dto = json.parseObject(
                "{\"name\":\"Drill\",\"description\":\"Power drill\",\"available\":false}");

        assertThat(dto.getRequestId()).isNull();
    }
}