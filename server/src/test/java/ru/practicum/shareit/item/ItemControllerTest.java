package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {
    private static final String HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ItemService itemService;

    private final ItemDto itemDto = new ItemDto(10L, "Drill", "Power drill", true, null);

    @Test
    void create_returnsItem() throws Exception {
        when(itemService.create(eq(1L), any(ItemCreateDto.class))).thenReturn(itemDto);

        mockMvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemCreateDto("Drill", "Power drill", true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Drill"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void create_withoutHeader_returns400() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemCreateDto("Drill", "Power drill", true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_userNotFound_returns404() throws Exception {
        when(itemService.create(eq(99L), any(ItemCreateDto.class)))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(post("/items")
                        .header(HEADER, 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemCreateDto("Drill", "Power drill", true, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsItem() throws Exception {
        when(itemService.update(eq(1L), eq(10L), any(ItemUpdateDto.class))).thenReturn(itemDto);

        mockMvc.perform(patch("/items/10")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemUpdateDto("Drill", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getById_returnsDetails() throws Exception {
        LocalDateTime last = LocalDateTime.of(2026, 11, 10, 10, 0, 0);
        when(itemService.getById(10L, 1L)).thenReturn(new ItemDetailsDto(10L, "Drill", "Power drill", true,
                last, null, List.of(new CommentDto(1L, "Nice", "Ivan", last))));

        mockMvc.perform(get("/items/10").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBooking").value("2026-11-10T10:00:00"))
                .andExpect(jsonPath("$.comments[0].authorName").value("Ivan"));
    }

    @Test
    void getById_withoutHeader_isAllowed() throws Exception {
        when(itemService.getById(10L, null)).thenReturn(
                new ItemDetailsDto(10L, "Drill", "Power drill", true, null, null, List.of()));

        mockMvc.perform(get("/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    void getAllByOwner_returnsList() throws Exception {
        when(itemService.getAllByOwner(1L)).thenReturn(List.of(
                new ItemDetailsDto(10L, "Drill", "Power drill", true, null, null, List.of())));

        mockMvc.perform(get("/items").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void search_returnsMatches() throws Exception {
        when(itemService.search("dri")).thenReturn(List.of(itemDto));

        mockMvc.perform(get("/items/search").param("text", "dri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Drill"));
    }

    @Test
    void addComment_returnsComment() throws Exception {
        when(itemService.addComment(eq(2L), eq(10L), any(CommentCreateDto.class)))
                .thenReturn(new CommentDto(1L, "Great", "Booker", LocalDateTime.of(2026, 11, 15, 12, 0, 0)));

        mockMvc.perform(post("/items/10/comment")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateDto("Great"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Great"))
                .andExpect(jsonPath("$.authorName").value("Booker"));
    }
}