package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user() {
        return new User(1L, "Ivan", "ivan@mail.ru");
    }

    @Test
    void create_savesNewUser() {
        when(userRepository.findByEmailIgnoreCase("ivan@mail.ru")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserDto result = userService.create(new UserCreateDto("Ivan", "ivan@mail.ru"));

        assertEquals(1L, result.getId());
        assertEquals("Ivan", result.getName());
        assertEquals("ivan@mail.ru", result.getEmail());
    }

    @Test
    void create_duplicateEmail_throwsConflict() {
        when(userRepository.findByEmailIgnoreCase("ivan@mail.ru")).thenReturn(Optional.of(user()));

        assertThrows(ConflictException.class,
                () -> userService.create(new UserCreateDto("Petr", "ivan@mail.ru")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_changesNameAndEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(userRepository.findByEmailIgnoreCase("new@mail.ru")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.update(1L, new UserUpdateDto("Petr", "new@mail.ru"));

        assertEquals("Petr", result.getName());
        assertEquals("new@mail.ru", result.getEmail());
    }

    @Test
    void update_ownEmail_isAllowed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(userRepository.findByEmailIgnoreCase("ivan@mail.ru")).thenReturn(Optional.of(user()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.update(1L, new UserUpdateDto("Petr", "ivan@mail.ru"));

        assertEquals("Petr", result.getName());
    }

    @Test
    void update_emailOfAnotherUser_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(userRepository.findByEmailIgnoreCase("busy@mail.ru"))
                .thenReturn(Optional.of(new User(2L, "Other", "busy@mail.ru")));

        assertThrows(ConflictException.class,
                () -> userService.update(1L, new UserUpdateDto(null, "busy@mail.ru")));
    }

    @Test
    void update_emptyFields_keepsOldValues() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.update(1L, new UserUpdateDto(null, "  "));

        assertEquals("Ivan", result.getName());
        assertEquals("ivan@mail.ru", result.getEmail());
    }

    @Test
    void update_userNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.update(1L, new UserUpdateDto("Petr", null)));
    }

    @Test
    void getById_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        assertEquals("Ivan", userService.getById(1L).getName());
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getById(1L));
    }

    @Test
    void getAll_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user(), new User(2L, "Petr", "petr@mail.ru")));

        assertEquals(2, userService.getAll().size());
    }

    @Test
    void delete_callsRepository() {
        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }
}