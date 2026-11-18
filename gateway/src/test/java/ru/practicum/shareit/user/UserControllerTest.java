package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private UserClient userClient;

    private static ResponseEntity<Object> ok(Object body) {
        return ResponseEntity.ok(body);
    }

    @Test
    void create_validBody_callsClient() throws Exception {
        when(userClient.create(any(UserCreateDto.class))).thenReturn(ok(Map.of("id", 1)));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateDto("Ivan", "ivan@mail.ru"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_invalidEmail_returns400WithoutCallingServer() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateDto("Ivan", "user.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(userClient);
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateDto(" ", "ivan@mail.ru"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutEmail_returns400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ivan\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_validBody_callsClient() throws Exception {
        when(userClient.update(eq(1L), any(UserUpdateDto.class))).thenReturn(ok(Map.of("id", 1)));

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserUpdateDto("Petr", null))))
                .andExpect(status().isOk());
    }

    @Test
    void update_invalidEmail_returns400() throws Exception {
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserUpdateDto(null, "bad"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userClient);
    }

    @Test
    void getById_callsClient() throws Exception {
        when(userClient.getById(1L)).thenReturn(ok(Map.of("name", "Ivan")));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ivan"));
    }

    @Test
    void getAll_callsClient() throws Exception {
        when(userClient.getAll()).thenReturn(ok(java.util.List.of()));

        mockMvc.perform(get("/users")).andExpect(status().isOk());
    }

    @Test
    void delete_callsClient() throws Exception {
        when(userClient.delete(1L)).thenReturn(ok(null));

        mockMvc.perform(delete("/users/1")).andExpect(status().isOk());

        verify(userClient).delete(1L);
    }
}