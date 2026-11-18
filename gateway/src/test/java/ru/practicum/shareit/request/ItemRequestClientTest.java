package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(value = ItemRequestClient.class, properties = "shareit-server.url=http://localhost:9090")
class ItemRequestClientTest {
    @Autowired
    private ItemRequestClient client;
    @Autowired
    private MockRestServiceServer server;

    @Test
    void create_postsRequest() {
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().json("{\"description\":\"Need a drill\"}"))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.create(1L, new ItemRequestCreateDto("Need a drill"))
                .getStatusCode().value());
        server.verify();
    }

    @Test
    void getOwn_getsOwnRequests() {
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getOwn(1L).getStatusCode().value());
        server.verify();
    }

    @Test
    void getAllOthers_getsRequestsOfOtherUsers() {
        server.expect(requestTo("http://localhost:9090/requests/all"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getAllOthers(1L).getStatusCode().value());
        server.verify();
    }

    @Test
    void getById_getsRequest() {
        server.expect(requestTo("http://localhost:9090/requests/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getById(1L, 5L).getStatusCode().value());
        server.verify();
    }
}