package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDetailsDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemCreateDto dto) {
        User owner = getUserOrThrow(userId);
        Item item = itemRepository.save(ItemMapper.toItem(dto, owner));
        return ItemMapper.toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemUpdateDto dto) {
        getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);
        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Вещь " + itemId + " не принадлежит пользователю " + userId);
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            item.setName(dto.getName());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            item.setDescription(dto.getDescription());
        }
        if (dto.getAvailable() != null) {
            item.setAvailable(dto.getAvailable());
        }
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDetailsDto getById(Long itemId, Long userId) {
        Item item = getItemOrThrow(itemId);
        List<CommentDto> comments = commentRepository.findByItem_IdOrderByCreatedDesc(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
        LocalDateTime last = null;
        LocalDateTime next = null;
        if (userId != null && item.getOwner().getId().equals(userId)) {
            List<Booking> bookings = bookingRepository.findByItem_IdInAndStatus(
                    List.of(itemId), BookingStatus.APPROVED);
            LocalDateTime now = LocalDateTime.now();
            last = lastBookingStart(bookings, now);
            next = nextBookingStart(bookings, now);
        }
        return ItemMapper.toItemDetailsDto(item, last, next, comments);
    }

    @Override
    public List<ItemDetailsDto> getAllByOwner(Long userId) {
        getUserOrThrow(userId);
        List<Item> items = itemRepository.findByOwner_Id(userId);
        if (items.isEmpty()) {
            return List.of();
        }
        List<Long> itemIds = items.stream().map(Item::getId).toList();
        Map<Long, List<Booking>> bookingsByItem = bookingRepository
                .findByItem_IdInAndStatus(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));
        Map<Long, List<Comment>> commentsByItem = commentRepository.findByItem_IdIn(itemIds).stream()
                .collect(Collectors.groupingBy(c -> c.getItem().getId()));
        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    List<Booking> bookings = bookingsByItem.getOrDefault(item.getId(), List.of());
                    List<CommentDto> comments = commentsByItem.getOrDefault(item.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(Comment::getCreated).reversed())
                            .map(CommentMapper::toCommentDto)
                            .toList();
                    return ItemMapper.toItemDetailsDto(item,
                            lastBookingStart(bookings, now), nextBookingStart(bookings, now), comments);
                })
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto dto) {
        User author = getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);
        LocalDateTime now = LocalDateTime.now();
        boolean hasFinishedBooking = bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndIsBefore(
                userId, itemId, BookingStatus.APPROVED, now);
        if (!hasFinishedBooking) {
            throw new BadRequestException("Комментарий можно оставить только после завершённой аренды вещи");
        }
        Comment comment = CommentMapper.toComment(dto, item, author, now);
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private LocalDateTime lastBookingStart(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .map(Booking::getStart)
                .filter(start -> start.isBefore(now))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private LocalDateTime nextBookingStart(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .map(Booking::getStart)
                .filter(start -> start.isAfter(now))
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь " + userId + " не найден"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь " + itemId + " не найдена"));
    }
}