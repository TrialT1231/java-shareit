package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceIntegrationTest {
    @Autowired
    private ItemRequestService requestService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void getById_returnsRequestWithItemCreatedInResponse() {
        User requestor = userRepository.save(new User(null, "Requestor", "req-it@mail.ru"));
        User owner = userRepository.save(new User(null, "Owner", "own-it@mail.ru"));
        ItemRequestDto request = requestService.create(requestor.getId(), new ItemRequestCreateDto("Need a drill"));
        itemService.create(owner.getId(), new ItemCreateDto("Drill", "Power drill", true, request.getId()));

        ItemRequestDto result = requestService.getById(owner.getId(), request.getId());

        assertNotNull(result.getCreated());
        assertEquals("Need a drill", result.getDescription());
        assertEquals(1, result.getItems().size());
        assertEquals("Drill", result.getItems().get(0).getName());
        assertEquals(owner.getId(), result.getItems().get(0).getOwnerId());
    }

    @Test
    void getOwnAndGetAllOthers_splitRequestsByAuthor() {
        User first = userRepository.save(new User(null, "First", "first-it@mail.ru"));
        User second = userRepository.save(new User(null, "Second", "second-it@mail.ru"));
        requestService.create(first.getId(), new ItemRequestCreateDto("First request"));
        requestService.create(second.getId(), new ItemRequestCreateDto("Second request"));

        List<ItemRequestDto> own = requestService.getOwn(first.getId());
        List<ItemRequestDto> others = requestService.getAllOthers(first.getId());

        assertEquals(1, own.size());
        assertEquals("First request", own.get(0).getDescription());
        assertTrue(others.stream().allMatch(r -> !r.getDescription().equals("First request")));
        assertTrue(others.stream().anyMatch(r -> r.getDescription().equals("Second request")));
    }
}