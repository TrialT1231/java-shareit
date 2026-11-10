package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "start");

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto dto) {
        User booker = getUserOrThrow(userId);
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь " + dto.getItemId() + " не найдена"));
        if (!item.isAvailable()) {
            throw new BadRequestException("Вещь " + item.getId() + " недоступна для аренды");
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь");
        }
        if (!dto.getEnd().isAfter(dto.getStart())) {
            throw new BadRequestException("Дата окончания должна быть позже даты начала");
        }
        Booking booking = bookingRepository.save(BookingMapper.toBooking(dto, item, booker));
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    @Transactional
    public BookingDto approve(Long userId, Long bookingId, boolean approved) {
        Booking booking = getBookingOrThrow(bookingId);
        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Подтвердить бронирование может только владелец вещи");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BadRequestException("Статус бронирования уже изменён");
        }
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Бронирование " + bookingId + " недоступно пользователю " + userId);
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getByBooker(Long userId, BookingState state) {
        getUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBooker_Id(userId, NEWEST_FIRST);
            case CURRENT -> bookingRepository.findByBooker_IdAndStartIsBeforeAndEndIsAfter(
                    userId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByBooker_IdAndEndIsBefore(userId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartIsAfter(userId, now, NEWEST_FIRST);
            case WAITING -> bookingRepository.findByBooker_IdAndStatus(
                    userId, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatus(
                    userId, BookingStatus.REJECTED, NEWEST_FIRST);
        };
        return bookings.stream().map(BookingMapper::toBookingDto).toList();
    }

    @Override
    public List<BookingDto> getByOwner(Long userId, BookingState state) {
        getUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItem_Owner_Id(userId, NEWEST_FIRST);
            case CURRENT -> bookingRepository.findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(
                    userId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndIsBefore(userId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartIsAfter(userId, now, NEWEST_FIRST);
            case WAITING -> bookingRepository.findByItem_Owner_IdAndStatus(
                    userId, BookingStatus.WAITING, NEWEST_FIRST);
            case REJECTED -> bookingRepository.findByItem_Owner_IdAndStatus(
                    userId, BookingStatus.REJECTED, NEWEST_FIRST);
        };
        return bookings.stream().map(BookingMapper::toBookingDto).toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь " + userId + " не найден"));
    }

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование " + bookingId + " не найдено"));
    }
}
