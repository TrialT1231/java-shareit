package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemRequestServiceImplTest {
    @Mock
    private ItemRequestRepository itemRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl service;

    private User requestor;
    private User owner;
    private ItemRequest request;
    private Item answer;

    @BeforeEach
    void setUp() {
        requestor = new User(1L, "Requestor", "req@mail.ru");
        owner = new User(2L, "Owner", "owner@mail.ru");
        request = new ItemRequest(5L, "Need a drill", requestor, LocalDateTime.now());
        answer = new Item(10L, "Drill", "Power drill", true, owner, request);
        when(userRepository.findById(1L)).thenReturn(Optional.of(requestor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
    }

    @Test
    void create_savesRequestWithEmptyAnswers() {
        when(itemRequestRepository.save(any(ItemRequest.class))).thenAnswer(inv -> {
            ItemRequest r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        ItemRequestDto result = service.create(1L, new ItemRequestCreateDto("Need a drill"));

        assertEquals(5L, result.getId());
        assertEquals("Need a drill", result.getDescription());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void create_userNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.create(99L, new ItemRequestCreateDto("Need a drill")));
    }

    @Test
    void getOwn_returnsRequestsWithAnswers() {
        when(itemRequestRepository.findByRequestor_IdOrderByCreatedDesc(1L)).thenReturn(List.of(request));
        when(itemRepository.findByRequest_IdIn(List.of(5L))).thenReturn(List.of(answer));

        List<ItemRequestDto> result = service.getOwn(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getItems().size());
        assertEquals(10L, result.get(0).getItems().get(0).getItemId());
        assertEquals(2L, result.get(0).getItems().get(0).getOwnerId());
    }

    @Test
    void getOwn_noRequests_returnsEmptyListWithoutItemQuery() {
        when(itemRequestRepository.findByRequestor_IdOrderByCreatedDesc(1L)).thenReturn(List.of());

        assertTrue(service.getOwn(1L).isEmpty());
        verifyNoInteractions(itemRepository);
    }

    @Test
    void getAllOthers_returnsRequestsOfOtherUsers() {
        when(itemRequestRepository.findByRequestor_IdNotOrderByCreatedDesc(2L)).thenReturn(List.of(request));
        when(itemRepository.findByRequest_IdIn(List.of(5L))).thenReturn(List.of());

        List<ItemRequestDto> result = service.getAllOthers(2L);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getItems().isEmpty());
    }

    @Test
    void getById_returnsRequestWithAnswers() {
        when(itemRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(itemRepository.findByRequest_IdIn(List.of(5L))).thenReturn(List.of(answer));

        ItemRequestDto result = service.getById(2L, 5L);

        assertEquals(5L, result.getId());
        assertEquals("Drill", result.getItems().get(0).getName());
    }

    @Test
    void getById_requestNotFound_throwsNotFound() {
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(1L, 99L));
    }

    @Test
    void getById_userNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getById(99L, 5L));
    }
}