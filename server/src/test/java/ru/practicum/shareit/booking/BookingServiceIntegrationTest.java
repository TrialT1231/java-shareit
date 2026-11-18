package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceIntegrationTest {
    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User booker;
    private Item drill;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        start = LocalDateTime.now().plusDays(1);
        end = LocalDateTime.now().plusDays(2);
        owner = userRepository.save(new User(null, "Owner", "owner-bk@mail.ru"));
        booker = userRepository.save(new User(null, "Booker", "booker-bk@mail.ru"));
        drill = itemRepository.save(new Item(null, "Drill", "Power drill", true, owner, null));
    }

    @Test
    void createAndApprove_changesStatusAndAppearsInLists() {
        BookingDto created = bookingService.create(booker.getId(), new BookingCreateDto(drill.getId(), start, end));

        assertNotNull(created.getId());
        assertEquals(BookingStatus.WAITING, created.getStatus());
        assertEquals(1, bookingService.getByOwner(owner.getId(), BookingState.WAITING).size());
        assertTrue(bookingService.getByOwner(owner.getId(), BookingState.REJECTED).isEmpty());

        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);

        assertEquals(BookingStatus.APPROVED, approved.getStatus());
        assertTrue(bookingService.getByOwner(owner.getId(), BookingState.WAITING).isEmpty());
        assertEquals(1, bookingService.getByBooker(booker.getId(), BookingState.FUTURE).size());
        assertTrue(bookingService.getByBooker(booker.getId(), BookingState.PAST).isEmpty());
        assertEquals(1, bookingService.getByBooker(booker.getId(), BookingState.ALL).size());
    }

    @Test
    void approve_byBooker_throwsForbidden() {
        BookingDto created = bookingService.create(booker.getId(), new BookingCreateDto(drill.getId(), start, end));

        assertThrows(ForbiddenException.class,
                () -> bookingService.approve(booker.getId(), created.getId(), true));
    }

    @Test
    void create_overlappingApprovedBooking_throwsBadRequest() {
        BookingDto first = bookingService.create(booker.getId(), new BookingCreateDto(drill.getId(), start, end));
        bookingService.approve(owner.getId(), first.getId(), true);
        User another = userRepository.save(new User(null, "Another", "another-bk@mail.ru"));

        assertThrows(BadRequestException.class, () -> bookingService.create(another.getId(),
                new BookingCreateDto(drill.getId(), start.plusHours(1), end.plusHours(1))));
    }

    @Test
    void getById_returnsBookingForBookerAndOwner() {
        BookingDto created = bookingService.create(booker.getId(), new BookingCreateDto(drill.getId(), start, end));

        List<Long> viewers = List.of(booker.getId(), owner.getId());
        for (Long viewer : viewers) {
            assertEquals(created.getId(), bookingService.getById(viewer, created.getId()).getId());
        }
    }
}