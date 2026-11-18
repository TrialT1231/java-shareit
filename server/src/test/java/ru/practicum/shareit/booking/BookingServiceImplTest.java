package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceImplTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;
    private Booking booking;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.now().plusDays(1);
        end = LocalDateTime.now().plusDays(2);
        owner = new User(1L, "Owner", "owner@mail.ru");
        booker = new User(2L, "Booker", "booker@mail.ru");
        item = new Item(10L, "Drill", "Power drill", true, owner, null);
        booking = new Booking(5L, start, end, item, booker, BookingStatus.WAITING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
    }

    @Test
    void create_savesWaitingBooking() {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(5L);
            return b;
        });

        BookingDto result = bookingService.create(2L, new BookingCreateDto(10L, start, end));

        assertEquals(5L, result.getId());
        assertEquals(BookingStatus.WAITING, result.getStatus());
        assertEquals(2L, result.getBooker().getId());
        assertEquals(10L, result.getItem().getId());
    }

    @Test
    void create_userNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class,
                () -> bookingService.create(99L, new BookingCreateDto(10L, start, end)));
    }

    @Test
    void create_itemNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class,
                () -> bookingService.create(2L, new BookingCreateDto(99L, start, end)));
    }

    @Test
    void create_unavailableItem_throwsBadRequest() {
        item.setAvailable(false);

        assertThrows(BadRequestException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, start, end)));
    }

    @Test
    void create_ownerBooksOwnItem_throwsNotFound() {
        assertThrows(NotFoundException.class,
                () -> bookingService.create(1L, new BookingCreateDto(10L, start, end)));
    }

    @Test
    void create_endNotAfterStart_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, start, start)));
    }

    @Test
    void create_itemAlreadyBooked_throwsBadRequest() {
        when(bookingRepository.existsByItem_IdAndStatusAndStartIsBeforeAndEndIsAfter(
                eq(10L), eq(BookingStatus.APPROVED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> bookingService.create(2L, new BookingCreateDto(10L, start, end)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approve_true_setsApproved() {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.approve(1L, 5L, true);

        assertEquals(BookingStatus.APPROVED, result.getStatus());
    }

    @Test
    void approve_false_setsRejected() {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = bookingService.approve(1L, 5L, false);

        assertEquals(BookingStatus.REJECTED, result.getStatus());
    }

    @Test
    void approve_notOwner_throwsForbidden() {
        assertThrows(ForbiddenException.class, () -> bookingService.approve(2L, 5L, true));
    }

    @Test
    void approve_alreadyProcessed_throwsBadRequest() {
        booking.setStatus(BookingStatus.APPROVED);

        assertThrows(BadRequestException.class, () -> bookingService.approve(1L, 5L, true));
    }

    @Test
    void approve_bookingNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> bookingService.approve(1L, 99L, true));
    }

    @Test
    void getById_forBooker_returnsBooking() {
        assertEquals(5L, bookingService.getById(2L, 5L).getId());
    }

    @Test
    void getById_forOwner_returnsBooking() {
        assertEquals(5L, bookingService.getById(1L, 5L).getId());
    }

    @Test
    void getById_forStranger_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> bookingService.getById(3L, 5L));
    }

    @Test
    void getByBooker_allStates_returnMappedBookings() {
        List<Booking> list = List.of(booking);
        when(bookingRepository.findByBooker_Id(eq(2L), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByBooker_IdAndStartIsBeforeAndEndIsAfter(
                eq(2L), any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByBooker_IdAndEndIsBefore(
                eq(2L), any(LocalDateTime.class), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByBooker_IdAndStartIsAfter(
                eq(2L), any(LocalDateTime.class), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByBooker_IdAndStatus(eq(2L), any(BookingStatus.class), any(Sort.class)))
                .thenReturn(list);

        for (BookingState state : BookingState.values()) {
            assertEquals(1, bookingService.getByBooker(2L, state).size(), state.name());
        }
    }

    @Test
    void getByBooker_userNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> bookingService.getByBooker(99L, BookingState.ALL));
    }

    @Test
    void getByOwner_allStates_returnMappedBookings() {
        List<Booking> list = List.of(booking);
        when(bookingRepository.findByItem_Owner_Id(eq(1L), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByItem_Owner_IdAndEndIsBefore(
                eq(1L), any(LocalDateTime.class), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByItem_Owner_IdAndStartIsAfter(
                eq(1L), any(LocalDateTime.class), any(Sort.class))).thenReturn(list);
        when(bookingRepository.findByItem_Owner_IdAndStatus(eq(1L), any(BookingStatus.class), any(Sort.class)))
                .thenReturn(list);

        for (BookingState state : BookingState.values()) {
            assertEquals(1, bookingService.getByOwner(1L, state).size(), state.name());
        }
    }

    @Test
    void getByOwner_userNotFound_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> bookingService.getByOwner(anyLongValue(), BookingState.ALL));
    }

    private static long anyLongValue() {
        return 99L;
    }
}