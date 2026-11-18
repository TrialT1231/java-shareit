package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(value = ItemClient.class, properties = "shareit-server.url=http://localhost:9090")
class ItemClientTest {
    @Autowired
    private ItemClient client;
    @Autowired
    private MockRestServiceServer server;

    @Test
    void create_postsItemWithUserHeader() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andExpect(content().json("{\"name\":\"Drill\",\"requestId\":5}"))
                .andRespond(withSuccess("{\"id\":10}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.create(1L, new ItemCreateDto("Drill", "Power drill", true, 5L))
                .getStatusCode().value());
        server.verify();
    }

    @Test
    void update_patchesItem() {
        server.expect(requestTo("http://localhost:9090/items/10"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("{\"id\":10}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.update(1L, 10L, new ItemUpdateDto("Hammer", null, null))
                .getStatusCode().value());
        server.verify();
    }

    @Test
    void getById_getsItem() {
        server.expect(requestTo("http://localhost:9090/items/10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("{\"id\":10}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getById(1L, 10L).getStatusCode().value());
        server.verify();
    }

    @Test
    void getAllByOwner_getsItems() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getAllByOwner(1L).getStatusCode().value());
        server.verify();
    }

    @Test
    void search_passesTextAsQueryParameter() {
        server.expect(requestTo("http://localhost:9090/items/search?text=drill"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(200, client.search(1L, "drill").getStatusCode().value());
        server.verify();
    }

    @Test
    void addComment_postsToCommentEndpoint() {
        server.expect(requestTo("http://localhost:9090/items/10/comment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andExpect(content().json("{\"text\":\"Great\"}"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.addComment(2L, 10L, new CommentCreateDto("Great"))
                .getStatusCode().value());
        server.verify();
    }
}