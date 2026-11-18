package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(value = UserClient.class, properties = "shareit-server.url=http://localhost:9090")
class UserClientTest {
    @Autowired
    private UserClient client;
    @Autowired
    private MockRestServiceServer server;

    @Test
    void create_postsBodyToUsers() {
        server.expect(requestTo("http://localhost:9090/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"name\":\"Ivan\",\"email\":\"ivan@mail.ru\"}"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = client.create(new UserCreateDto("Ivan", "ivan@mail.ru"));

        assertEquals(200, response.getStatusCode().value());
        server.verify();
    }

    @Test
    void update_patchesUserById() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = client.update(1L, new UserUpdateDto("Petr", null));

        assertEquals(200, response.getStatusCode().value());
        server.verify();
    }

    @Test
    void getById_getsUser() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getById(1L).getStatusCode().value());
        server.verify();
    }

    @Test
    void getAll_getsUsers() {
        server.expect(requestTo("http://localhost:9090/users"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(200, client.getAll().getStatusCode().value());
        server.verify();
    }

    @Test
    void delete_deletesUser() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        assertEquals(200, client.delete(1L).getStatusCode().value());
        server.verify();
    }

    @Test
    void serverError_isPassedThroughWithSameStatus() {
        server.expect(requestTo("http://localhost:9090/users/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"not found\"}"));

        ResponseEntity<Object> response = client.getById(99L);

        assertEquals(404, response.getStatusCode().value());
        server.verify();
    }
}