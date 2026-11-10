package ru.practicum.shareit.user;

import ru.practicum.shareit.user.dto.UserCreateDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserUpdateDto;

import java.util.List;

public interface UserService {
    UserDto create(UserCreateDto dto);

    UserDto update(Long id, UserUpdateDto dto);

    UserDto getById(Long id);

    List<UserDto> getAll();

    void delete(Long id);
}