package com.silvaldeweb.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.silvaldeweb.dto.user.UserCreateRequest;
import com.silvaldeweb.dto.user.UserResponse;
import com.silvaldeweb.dto.user.UserUpdateRequest;
import com.silvaldeweb.exception.user.UserAlreadyExistsException;
import com.silvaldeweb.exception.user.UserNotFoundException;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createHashesPasswordAndPersists() {
        UserCreateRequest request = new UserCreateRequest(
                "user@example.com",
                "plainPassword",
                "John Doe",
                "123456789",
                true,
                Role.USER
        );

        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertEquals("user@example.com", captor.getValue().getEmail());
        assertEquals("hashedPassword", captor.getValue().getPassword());
        assertEquals("John Doe", captor.getValue().getName());
        assertEquals(Role.USER, captor.getValue().getRole());
        assertEquals(true, captor.getValue().getActive());
        assertEquals(1L, response.id());
        assertEquals("user@example.com", response.email());
        assertEquals(Role.USER, response.role());
    }

    @Test
    void createDefaultsRoleToUserWhenNull() {
        UserCreateRequest request = new UserCreateRequest(
                "new@example.com",
                "plainPassword",
                "Jane",
                null,
                null,
                null
        );

        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.USER, captor.getValue().getRole());
        assertEquals(true, captor.getValue().getActive());
    }

    @Test
    void createThrowsWhenEmailExists() {
        UserCreateRequest request = new UserCreateRequest(
                "dup@example.com",
                "plainPassword",
                "Dup",
                null,
                null,
                Role.USER
        );
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.create(request));
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateKeepsPasswordWhenNotProvided() {
        User existing = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("existingHash")
                .name("Old Name")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = new UserUpdateRequest(
                "user@example.com",
                null,
                "New Name",
                null,
                null,
                null
        );

        userService.update(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("existingHash", captor.getValue().getPassword());
        assertEquals("New Name", captor.getValue().getName());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateHashesPasswordWhenProvided() {
        User existing = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("existingHash")
                .name("Old Name")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPlain")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = new UserUpdateRequest(
                "user@example.com",
                "newPlain",
                "New Name",
                null,
                null,
                null
        );

        userService.update(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("newHash", captor.getValue().getPassword());
    }

    @Test
    void updateThrowsWhenEmailTakenByOther() {
        User existing = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("hash")
                .name("Name")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCase("other@example.com")).thenReturn(true);

        UserUpdateRequest request = new UserUpdateRequest(
                "other@example.com",
                null,
                "New Name",
                null,
                null,
                null
        );

        assertThrows(UserAlreadyExistsException.class, () -> userService.update(1L, request));
    }

    @Test
    void getThrowsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.get(99L));
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.delete(99L));
    }
}
