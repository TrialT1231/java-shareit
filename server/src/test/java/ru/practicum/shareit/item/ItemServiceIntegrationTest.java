package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDetailsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceIntegrationTest {
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private CommentRepository commentRepository;

    private User owner;
    private User booker;
    private Item drill;
    private Item hammer;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        owner = userRepository.save(new User(null, "Owner", "owner-it@mail.ru"));
        booker = userRepository.save(new User(null, "Booker", "booker-it@mail.ru"));
        drill = itemRepository.save(new Item(null, "Drill", "Power drill", true, owner, null));
        hammer = itemRepository.save(new Item(null, "Hammer", "Heavy hammer", true, owner, null));
    }

    @Test
    void getAllByOwner_returnsItemsWithBookingDatesAndComments() {
        Booking past = bookingRepository.save(new Booking(null, now.minusDays(3), now.minusDays(2),
                drill, booker, BookingStatus.APPROVED));
        Booking future = bookingRepository.save(new Booking(null, now.plusDays(1), now.plusDays(2),
                drill, booker, BookingStatus.APPROVED));
        bookingRepository.save(new Booking(null, now.plusDays(7), now.plusDays(8),
                drill, booker, BookingStatus.WAITING));
        commentRepository.save(new Comment(null, "Works great", drill, booker, now));

        List<ItemDetailsDto> result = itemService.getAllByOwner(owner.getId());

        assertEquals(2, result.size());
        ItemDetailsDto drillDto = result.stream()
                .filter(i -> i.getId().equals(drill.getId())).findFirst().orElseThrow();
        ItemDetailsDto hammerDto = result.stream()
                .filter(i -> i.getId().equals(hammer.getId())).findFirst().orElseThrow();
        assertEquals(past.getStart(), drillDto.getLastBooking());
        assertEquals(future.getStart(), drillDto.getNextBooking());
        assertEquals(1, drillDto.getComments().size());
        assertEquals("Booker", drillDto.getComments().get(0).getAuthorName());
        assertNull(hammerDto.getLastBooking());
        assertNull(hammerDto.getNextBooking());
        assertEquals(0, hammerDto.getComments().size());
    }

    @Test
    void addComment_afterFinishedBooking_savesComment() {
        bookingRepository.save(new Booking(null, now.minusDays(3), now.minusDays(2),
                drill, booker, BookingStatus.APPROVED));

        CommentDto result = itemService.addComment(booker.getId(), drill.getId(), new CommentCreateDto("Thanks"));

        assertNotNull(result.getId());
        assertEquals("Thanks", result.getText());
        assertEquals("Booker", result.getAuthorName());
        assertEquals(1, commentRepository.findByItem_IdOrderByCreatedDesc(drill.getId()).size());
    }

    @Test
    void addComment_withoutFinishedBooking_throwsBadRequest() {
        bookingRepository.save(new Booking(null, now.plusDays(1), now.plusDays(2),
                drill, booker, BookingStatus.APPROVED));

        assertThrows(BadRequestException.class,
                () -> itemService.addComment(booker.getId(), drill.getId(), new CommentCreateDto("Too early")));
    }

    @Test
    void search_findsOnlyAvailableItemsIgnoringCase() {
        hammer.setAvailable(false);
        itemRepository.save(hammer);

        assertEquals(1, itemService.search("DRILL").size());
        assertEquals(0, itemService.search("hammer").size());
    }
}