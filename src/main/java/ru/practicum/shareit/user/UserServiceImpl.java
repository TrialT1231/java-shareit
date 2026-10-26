package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserStorage storage;

    @Override
    public UserDto create(UserCreateDto dto) {
        checkEmailFree(dto.getEmail(), null);
        return UserMapper.toUserDto(storage.save(UserMapper.toUser(dto)));
    }

    @Override
    public UserDto update(Long id, UserUpdateDto dto) {
        User current = storage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
        String name = current.getName();
        String email = current.getEmail();
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            checkEmailFree(dto.getEmail(), id);
            email = dto.getEmail();
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            name = dto.getName();
        }
        // создаём новый объект, чтобы хранилище видело старый email и обновило индекс
        return UserMapper.toUserDto(storage.update(new User(id, name, email)));
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