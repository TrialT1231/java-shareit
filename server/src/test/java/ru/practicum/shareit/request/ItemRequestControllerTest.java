package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private ItemRequestService requestService;

    private final ItemRequestDto requestDto = new ItemRequestDto(5L, "Need a drill",
            LocalDateTime.of(2026, 11, 15, 12, 0, 0), List.of(new ItemShortDto(10L, "Drill", 2L)));

    @Test
    void create_returnsRequest() throws Exception {
        when(requestService.create(eq(1L), any(ItemRequestCreateDto.class))).thenReturn(requestDto);

        mockMvc.perform(post("/requests")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestCreateDto("Need a drill"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.created").value("2026-11-15T12:00:00"))
                .andExpect(jsonPath("$.items[0].name").value("Drill"));
    }

    @Test
    void getOwn_returnsList() throws Exception {
        when(requestService.getOwn(1L)).thenReturn(List.of(requestDto));

        mockMvc.perform(get("/requests").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllOthers_returnsList() throws Exception {
        when(requestService.getAllOthers(2L)).thenReturn(List.of(requestDto));

        mockMvc.perform(get("/requests/all").header(HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Need a drill"));
    }

    @Test
    void getById_returnsRequest() throws Exception {
        when(requestService.getById(2L, 5L)).thenReturn(requestDto);

        mockMvc.perform(get("/requests/5").header(HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ownerId").value(2));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(requestService.getById(2L, 99L)).thenThrow(new NotFoundException("Запрос не найден"));

        mockMvc.perform(get("/requests/99").header(HEADER, 2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Запрос не найден"));
    }
}