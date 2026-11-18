package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemServiceImplTest {
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User booker;
    private Item item;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        owner = new User(1L, "Owner", "owner@mail.ru");
        booker = new User(2L, "Booker", "booker@mail.ru");
        item = new Item(10L, "Drill", "Power drill", true, owner, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
    }

    @Test
    void create_withoutRequest_savesItem() {
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(10L);
            return i;
        });

        ItemDto result = itemService.create(1L, new ItemCreateDto("Drill", "Power drill", true, null));

        assertEquals(10L, result.getId());
        assertEquals("Drill", result.getName());
        assertNull(result.getRequestId());
    }

    @Test
    void create_withRequest_linksRequest() {
        ItemRequest request = new ItemRequest(5L, "Need a drill", booker, now);
        when(itemRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(11L);
            return i;
        });

        ItemDto result = itemService.create(1L, new ItemCreateDto("Drill", "Power drill", true, 5L));

        assertEquals(5L, result.getRequestId());
    }

    @Test
    void create_requestNotFound_throwsNotFound() {
        when(itemRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.create(1L, new ItemCreateDto("Drill", "Power drill", true, 99L)));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void create_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.create(99L, new ItemCreateDto("Drill", "Power drill", true, null)));
    }

    @Test
    void update_byOwner_changesFields() {
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemDto result = itemService.update(1L, 10L, new ItemUpdateDto("Hammer", "Heavy hammer", false));

        assertEquals("Hammer", result.getName());
        assertEquals("Heavy hammer", result.getDescription());
        assertEquals(false, result.getAvailable());
    }

    @Test
    void update_emptyFields_keepsOldValues() {
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemDto result = itemService.update(1L, 10L, new ItemUpdateDto(" ", null, null));

        assertEquals("Drill", result.getName());
        assertEquals("Power drill", result.getDescription());
        assertEquals(true, result.getAvailable());
    }

    @Test
    void update_notOwner_throwsNotFound() {
        assertThrows(NotFoundException.class,
                () -> itemService.update(2L, 10L, new ItemUpdateDto("Hammer", null, null)));
    }

    @Test
    void update_itemNotFound_throwsNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.update(1L, 99L, new ItemUpdateDto("Hammer", null, null)));
    }

    @Test
    void getById_forOwner_addsLastAndNextBooking() {
        Booking past = new Booking(1L, now.minusDays(3), now.minusDays(2), item, booker, BookingStatus.APPROVED);
        Booking soon = new Booking(2L, now.plusDays(1), now.plusDays(2), item, booker, BookingStatus.APPROVED);
        Booking later = new Booking(3L, now.plusDays(5), now.plusDays(6), item, booker, BookingStatus.APPROVED);
        when(bookingRepository.findByItem_IdInAndStatus(List.of(10L), BookingStatus.APPROVED))
                .thenReturn(List.of(past, later, soon));
        when(commentRepository.findByItem_IdOrderByCreatedDesc(10L))
                .thenReturn(List.of(new Comment(1L, "Nice", item, booker, now)));

        ItemDetailsDto result = itemService.getById(10L, 1L);

        assertEquals(past.getStart(), result.getLastBooking());
        assertEquals(soon.getStart(), result.getNextBooking());
        assertEquals(1, result.getComments().size());
        assertEquals("Booker", result.getComments().get(0).getAuthorName());
    }

    @Test
    void getById_forOwnerWithoutBookings_hasNullDates() {
        when(bookingRepository.findByItem_IdInAndStatus(List.of(10L), BookingStatus.APPROVED))
                .thenReturn(List.of());

        ItemDetailsDto result = itemService.getById(10L, 1L);

        assertNull(result.getLastBooking());
        assertNull(result.getNextBooking());
        assertTrue(result.getComments().isEmpty());
    }

    @Test
    void getById_forNotOwner_hidesBookingDates() {
        ItemDetailsDto result = itemService.getById(10L, 2L);

        assertNull(result.getLastBooking());
        assertNull(result.getNextBooking());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getById_withoutUserId_hidesBookingDates() {
        ItemDetailsDto result = itemService.getById(10L, null);

        assertNull(result.getLastBooking());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getById_itemNotFound_throwsNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.getById(99L, 1L));
    }

    @Test
    void getAllByOwner_returnsItemsWithBookingsAndComments() {
        Booking past = new Booking(1L, now.minusDays(3), now.minusDays(2), item, booker, BookingStatus.APPROVED);
        Booking soon = new Booking(2L, now.plusDays(1), now.plusDays(2), item, booker, BookingStatus.APPROVED);
        Comment older = new Comment(1L, "Old", item, booker, now.minusDays(1));
        Comment newer = new Comment(2L, "New", item, booker, now);
        when(itemRepository.findByOwner_Id(1L)).thenReturn(List.of(item));
        when(bookingRepository.findByItem_IdInAndStatus(List.of(10L), BookingStatus.APPROVED))
                .thenReturn(List.of(past, soon));
        when(commentRepository.findByItem_IdIn(List.of(10L))).thenReturn(List.of(older, newer));

        List<ItemDetailsDto> result = itemService.getAllByOwner(1L);

        assertEquals(1, result.size());
        assertEquals(past.getStart(), result.get(0).getLastBooking());
        assertEquals(soon.getStart(), result.get(0).getNextBooking());
        assertEquals("New", result.get(0).getComments().get(0).getText());
    }

    @Test
    void getAllByOwner_noItems_returnsEmptyList() {
        when(itemRepository.findByOwner_Id(1L)).thenReturn(List.of());

        assertTrue(itemService.getAllByOwner(1L).isEmpty());
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void getAllByOwner_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.getAllByOwner(99L));
    }

    @Test
    void search_blankText_returnsEmptyListWithoutQuery() {
        assertTrue(itemService.search("  ").isEmpty());
        assertTrue(itemService.search(null).isEmpty());
        verifyNoInteractions(itemRepository);
    }

    @Test
    void search_text_returnsMatches() {
        when(itemRepository.search("dri")).thenReturn(List.of(item));

        List<ItemDto> result = itemService.search("dri");

        assertEquals(1, result.size());
        assertEquals("Drill", result.get(0).getName());
    }

    @Test
    void addComment_afterFinishedBooking_savesComment() {
        when(bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndIsBefore(
                eq2(2L), eq2(10L), eq2(BookingStatus.APPROVED), any(LocalDateTime.class))).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CommentDto result = itemService.addComment(2L, 10L, new CommentCreateDto("Great"));

        assertEquals(1L, result.getId());
        assertEquals("Great", result.getText());
        assertEquals("Booker", result.getAuthorName());
    }

    @Test
    void addComment_withoutFinishedBooking_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> itemService.addComment(2L, 10L, new CommentCreateDto("Great")));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_userNotFound_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.addComment(99L, 10L, new CommentCreateDto("Great")));
    }

    @Test
    void addComment_itemNotFound_throwsNotFound() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.addComment(2L, 99L, new CommentCreateDto("Great")));
    }

    private static <T> T eq2(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}