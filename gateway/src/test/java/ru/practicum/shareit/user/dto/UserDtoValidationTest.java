package ru.practicum.shareit.user.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoValidationTest {
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createDto_valid_hasNoViolations() {
        assertThat(validator.validate(new UserCreateDto("Ivan", "ivan@mail.ru"))).isEmpty();
    }

    @Test
    void createDto_invalidEmail_isViolation() {
        assertThat(validator.validate(new UserCreateDto("Ivan", "user.com"))).hasSize(1);
    }

    @Test
    void createDto_blankNameAndEmail_hasViolations() {
        assertThat(validator.validate(new UserCreateDto("", null))).isNotEmpty();
    }

    @Test
    void updateDto_emptyIsAllowed() {
        assertThat(validator.validate(new UserUpdateDto(null, null))).isEmpty();
    }

    @Test
    void updateDto_invalidEmail_isViolation() {
        assertThat(validator.validate(new UserUpdateDto("Ivan", "bad"))).hasSize(1);
    }
}