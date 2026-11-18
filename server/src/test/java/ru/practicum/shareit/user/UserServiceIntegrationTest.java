package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Test
    void createUpdateAndGet_workWithRealDatabase() {
        UserDto created = userService.create(new UserCreateDto("Ivan", "ivan-us@mail.ru"));

        UserDto updated = userService.update(created.getId(), new UserUpdateDto("Petr", "petr-us@mail.ru"));

        assertEquals("Petr", updated.getName());
        assertEquals("petr-us@mail.ru", userService.getById(created.getId()).getEmail());
    }

    @Test
    void create_sameEmailInDifferentCase_throwsConflict() {
        userService.create(new UserCreateDto("Ivan", "case-us@mail.ru"));

        assertThrows(ConflictException.class,
                () -> userService.create(new UserCreateDto("Other", "CASE-US@mail.ru")));
    }
}