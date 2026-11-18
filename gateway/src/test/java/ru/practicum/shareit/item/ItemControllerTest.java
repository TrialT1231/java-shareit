package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    private ItemClient itemClient;

    private static ResponseEntity<Object> ok(Object body) {
        return ResponseEntity.ok(body);
    }

    @Test
    void create_validBody_callsClient() throws Exception {
        when(itemClient.create(eq(1L), any(ItemCreateDto.class))).thenReturn(ok(Map.of("id", 10)));

        mockMvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemCreateDto("Drill", "Power drill", true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void create_withRequestId_isAccepted() throws Exception {
        when(itemClient.create(eq(1L), any(ItemCreateDto.class))).thenReturn(ok(Map.of("id", 11)));

        mockMvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemCreateDto("Drill", "Power drill", true, 5L))))
                .andExpect(status().isOk());
    }

    @Test
    void create_blankName_returns400WithErrorField() throws Exception {
        mockMvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemCreateDto("", "Power drill", true, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(itemClient);
    }

    @Test
    void create_withoutDescription_returns400() throws Exception {
        mockMvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"available\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutAvailable_returns400() throws Exception {
        mockMvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Power drill\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutUserHeader_returns400WithErrorField() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemCreateDto("Drill", "Power drill", true, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void update_callsClient() throws Exception {
        when(itemClient.update(eq(1L), eq(10L), any(ItemUpdateDto.class))).thenReturn(ok(Map.of("id", 10)));

        mockMvc.perform(patch("/items/10")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemUpdateDto("Hammer", null, null))))
                .andExpect(status().isOk());
    }

    @Test
    void getById_callsClient() throws Exception {
        when(itemClient.getById(1L, 10L)).thenReturn(ok(Map.of("name", "Drill")));

        mockMvc.perform(get("/items/10").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    void getById_notNumericId_returns400() throws Exception {
        mockMvc.perform(get("/items/abc").header(HEADER, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getAllByOwner_callsClient() throws Exception {
        when(itemClient.getAllByOwner(1L)).thenReturn(ok(List.of()));

        mockMvc.perform(get("/items").header(HEADER, 1)).andExpect(status().isOk());
    }

    @Test
    void search_withText_callsClient() throws Exception {
        when(itemClient.search(1L, "drill")).thenReturn(ok(List.of(Map.of("name", "Drill"))));

        mockMvc.perform(get("/items/search").header(HEADER, 1).param("text", "drill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Drill"));
    }

    @Test
    void search_blankText_returnsEmptyListWithoutCallingServer() throws Exception {
        mockMvc.perform(get("/items/search").header(HEADER, 1).param("text", "  "))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verifyNoInteractions(itemClient);
    }

    @Test
    void addComment_validBody_callsClient() throws Exception {
        when(itemClient.addComment(eq(2L), eq(10L), any(CommentCreateDto.class)))
                .thenReturn(ok(Map.of("text", "Great")));

        mockMvc.perform(post("/items/10/comment")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateDto("Great"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Great"));
    }

    @Test
    void addComment_blankText_returns400() throws Exception {
        mockMvc.perform(post("/items/10/comment")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateDto(" "))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemClient);
    }
}