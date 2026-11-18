package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private UserService userService;

    @Test
    void create_returnsCreatedUser() throws Exception {
        when(userService.create(any(UserCreateDto.class))).thenReturn(new UserDto(1L, "Ivan", "ivan@mail.ru"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateDto("Ivan", "ivan@mail.ru"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        when(userService.create(any(UserCreateDto.class))).thenThrow(new ConflictException("Email занят"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateDto("Ivan", "ivan@mail.ru"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email занят"));
    }

    @Test
    void update_returnsUpdatedUser() throws Exception {
        when(userService.update(any(Long.class), any(UserUpdateDto.class)))
                .thenReturn(new UserDto(1L, "Petr", "petr@mail.ru"));

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserUpdateDto("Petr", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Petr"));
    }

    @Test
    void getById_returnsUser() throws Exception {
        when(userService.getById(1L)).thenReturn(new UserDto(1L, "Ivan", "ivan@mail.ru"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(userService.getById(99L)).thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Пользователь не найден"));
    }

    @Test
    void getAll_returnsList() throws Exception {
        when(userService.getAll()).thenReturn(List.of(
                new UserDto(1L, "Ivan", "ivan@mail.ru"), new UserDto(2L, "Petr", "petr@mail.ru")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void delete_callsService() throws Exception {
        mockMvc.perform(delete("/users/1")).andExpect(status().isOk());

        verify(userService).delete(1L);
    }
}