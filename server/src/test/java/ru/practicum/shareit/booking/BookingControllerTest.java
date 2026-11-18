package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {
    private static final String HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private BookingService bookingService;

    private final LocalDateTime start = LocalDateTime.of(2026, 11, 20, 10, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 11, 21, 10, 0, 0);
    private final BookingDto bookingDto = new BookingDto(5L, start, end, BookingStatus.WAITING,
            new UserDto(2L, "Booker", "booker@mail.ru"),
            new ItemDto(10L, "Drill", "Power drill", true, null));

    @Test
    void create_returnsBooking() throws Exception {
        when(bookingService.create(eq(2L), any(BookingCreateDto.class))).thenReturn(bookingDto);

        mockMvc.perform(post("/bookings")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(10L, start, end))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.start").value("2026-11-20T10:00:00"))
                .andExpect(jsonPath("$.booker.id").value(2))
                .andExpect(jsonPath("$.item.name").value("Drill"));
    }

    @Test
    void approve_returnsBooking() throws Exception {
        when(bookingService.approve(1L, 5L, true)).thenReturn(bookingDto);

        mockMvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void approve_notOwner_returns403() throws Exception {
        when(bookingService.approve(2L, 5L, true)).thenThrow(new ForbiddenException("Только владелец"));

        mockMvc.perform(patch("/bookings/5").header(HEADER, 2).param("approved", "true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Только владелец"));
    }

    @Test
    void getById_returnsBooking() throws Exception {
        when(bookingService.getById(2L, 5L)).thenReturn(bookingDto);

        mockMvc.perform(get("/bookings/5").header(HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.id").value(10));
    }

    @Test
    void getByBooker_usesStateParameter() throws Exception {
        when(bookingService.getByBooker(2L, BookingState.FUTURE)).thenReturn(List.of(bookingDto));

        mockMvc.perform(get("/bookings").header(HEADER, 2).param("state", "future"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getByBooker_defaultStateIsAll() throws Exception {
        when(bookingService.getByBooker(2L, BookingState.ALL)).thenReturn(List.of());

        mockMvc.perform(get("/bookings").header(HEADER, 2)).andExpect(status().isOk());

        verify(bookingService).getByBooker(2L, BookingState.ALL);
    }

    @Test
    void getByBooker_unknownState_returns400() throws Exception {
        mockMvc.perform(get("/bookings").header(HEADER, 2).param("state", "BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: BAD"));
    }

    @Test
    void getByOwner_usesStateParameter() throws Exception {
        when(bookingService.getByOwner(1L, BookingState.WAITING)).thenReturn(List.of(bookingDto));

        mockMvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    void getByOwner_unknownState_returns400() throws Exception {
        mockMvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "oops"))
                .andExpect(status().isBadRequest());
    }
}