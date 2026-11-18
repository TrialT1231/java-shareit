package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(value = BookingClient.class, properties = "shareit-server.url=http://localhost:9090")
class BookingClientTest {
    @Autowired
    private BookingClient client;
    @Autowired
    private MockRestServiceServer server;

    @Test
    void bookItem_postsBooking() {
        server.expect(requestTo("http://localhost:9090/bookings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andExpect(content().json("{\"itemId\":10}"))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        BookItemRequestDto dto = new BookItemRequestDto(10L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        assertEquals(200, client.bookItem(2L, dto).getStatusCode().value());
        server.verify();
    }

    @Test
    void getBookings_passesStateAndPaging() {
        server.expect(requestTo("http://localhost:9090/bookings?state=ALL&from=0&size=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getBookings(2L, BookingState.ALL, 0, 10).getStatusCode().value());
        server.verify();
    }

    @Test
    void getBooking_getsBookingById() {
        server.expect(requestTo("http://localhost:9090/bookings/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getBooking(2L, 5L).getStatusCode().value());
        server.verify();
    }

    @Test
    void approve_patchesWithApprovedParameter() {
        server.expect(requestTo("http://localhost:9090/bookings/5?approved=true"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.approve(1L, 5L, true).getStatusCode().value());
        server.verify();
    }

    @Test
    void getBookingsByOwner_passesState() {
        server.expect(requestTo("http://localhost:9090/bookings/owner?state=FUTURE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getBookingsByOwner(1L, BookingState.FUTURE).getStatusCode().value());
        server.verify();
    }
}