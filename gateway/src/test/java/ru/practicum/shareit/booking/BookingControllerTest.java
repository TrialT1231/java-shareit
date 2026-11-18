package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private BookingClient bookingClient;

    private static ResponseEntity<Object> ok(Object body) {
        return ResponseEntity.ok(body);
    }

    @Test
    void bookItem_validBody_callsClient() throws Exception {
        when(bookingClient.bookItem(any(Long.class), any(BookItemRequestDto.class)))
                .thenReturn(ok(Map.of("id", 5)));
        BookItemRequestDto dto = new BookItemRequestDto(10L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/bookings")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void bookItem_endInPast_returns400() throws Exception {
        BookItemRequestDto dto = new BookItemRequestDto(10L,
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2));

        mockMvc.perform(post("/bookings")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(bookingClient);
    }

    @Test
    void getBooking_callsClient() throws Exception {
        when(bookingClient.getBooking(2L, 5L)).thenReturn(ok(Map.of("id", 5)));

        mockMvc.perform(get("/bookings/5").header(HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getBookings_defaultStateIsAll() throws Exception {
        when(bookingClient.getBookings(2L, BookingState.ALL, 0, 10)).thenReturn(ok(List.of()));

        mockMvc.perform(get("/bookings").header(HEADER, 2)).andExpect(status().isOk());

        verify(bookingClient).getBookings(2L, BookingState.ALL, 0, 10);
    }

    @Test
    void getBookings_unknownState_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/bookings").header(HEADER, 2).param("state", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: unknown"));

        verifyNoInteractions(bookingClient);
    }

    @Test
    void approve_callsClient() throws Exception {
        when(bookingClient.approve(1L, 5L, true)).thenReturn(ok(Map.of("status", "APPROVED")));

        mockMvc.perform(patch("/bookings/5").header(HEADER, 1).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_withoutApprovedParam_returns400() throws Exception {
        mockMvc.perform(patch("/bookings/5").header(HEADER, 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBookingsByOwner_callsClient() throws Exception {
        when(bookingClient.getBookingsByOwner(1L, BookingState.FUTURE)).thenReturn(ok(List.of()));

        mockMvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "future"))
                .andExpect(status().isOk());

        verify(bookingClient).getBookingsByOwner(1L, BookingState.FUTURE);
    }

    @Test
    void getBookingsByOwner_unknownState_returns400() throws Exception {
        mockMvc.perform(get("/bookings/owner").header(HEADER, 1).param("state", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown state: bad"));
    }
}