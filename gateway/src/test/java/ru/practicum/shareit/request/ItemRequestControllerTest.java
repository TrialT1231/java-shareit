package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {
    private static final String HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ItemRequestClient requestClient;

    private static ResponseEntity<Object> ok(Object body) {
        return ResponseEntity.ok(body);
    }

    @Test
    void create_validBody_callsClient() throws Exception {
        when(requestClient.create(eq(1L), any(ItemRequestCreateDto.class))).thenReturn(ok(Map.of("id", 5)));

        mockMvc.perform(post("/requests")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestCreateDto("Need a drill"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_blankDescription_returns400() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestCreateDto(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(requestClient);
    }

    @Test
    void getOwn_callsClient() throws Exception {
        when(requestClient.getOwn(1L)).thenReturn(ok(List.of()));

        mockMvc.perform(get("/requests").header(HEADER, 1)).andExpect(status().isOk());
    }

    @Test
    void getAllOthers_callsClient() throws Exception {
        when(requestClient.getAllOthers(2L)).thenReturn(ok(List.of()));

        mockMvc.perform(get("/requests/all").header(HEADER, 2)).andExpect(status().isOk());
    }

    @Test
    void getById_callsClient() throws Exception {
        when(requestClient.getById(2L, 5L)).thenReturn(ok(Map.of("id", 5)));

        mockMvc.perform(get("/requests/5").header(HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }
}