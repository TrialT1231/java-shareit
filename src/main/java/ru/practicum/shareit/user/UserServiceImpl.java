package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserStorage storage;

    @Override
    public UserDto create(UserDto dto) {
        checkEmailFree(dto.getEmail(), null);
        return UserMapper.toUserDto(storage.save(UserMapper.toUser(dto)));
    }

    @Override
    public UserDto update(Long id, UserDto dto) {
        User user = storage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            checkEmailFree(dto.getEmail(), id);
            user.setEmail(dto.getEmail());
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        return UserMapper.toUserDto(storage.update(user));
    }

    @Override
    public UserDto getById(Long id) {
        return storage.findById(id)
                .map(UserMapper::toUserDto)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
    }

    @Override
    public List<UserDto> getAll() {
        return storage.findAll().stream().map(UserMapper::toUserDto).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        storage.delete(id);
    }

    private void checkEmailFree(String email, Long selfId) {
        storage.findByEmail(email).ifPresent(u -> {
            if (!u.getId().equals(selfId)) {
                throw new ConflictException("Email " + email + " уже используется");
            }
        });
    }
}
