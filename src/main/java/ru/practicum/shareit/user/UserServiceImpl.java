package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserCreateDto dto) {
        checkEmailFree(dto.getEmail(), null);
        return UserMapper.toUserDto(userRepository.save(UserMapper.toUser(dto)));
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            checkEmailFree(dto.getEmail(), id);
            user.setEmail(dto.getEmail());
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto getById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toUserDto)
                .orElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден"));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toUserDto).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    private void checkEmailFree(String email, Long selfId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            if (!u.getId().equals(selfId)) {
                throw new ConflictException("Email " + email + " уже используется");
            }
        });
    }
}