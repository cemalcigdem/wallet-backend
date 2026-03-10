package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.User;
import com.cemalcigdem.wallet.dto.UserCreateRequest;
import com.cemalcigdem.wallet.dto.UserResponse;
import com.cemalcigdem.wallet.exception.DuplicateEmailException;
import com.cemalcigdem.wallet.exception.UserNotFoundException;
import com.cemalcigdem.wallet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void create_happyPath_savesUserAndReturnsResponse() {
        // arrange
        UserCreateRequest request = new UserCreateRequest("Ada Lovelace", "ada@ex.com");
        when(userRepository.existsByEmail("ada@ex.com")).thenReturn(false);

        User saved = new User("Ada Lovelace", "ada@ex.com");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        // act
        UserResponse response = userService.create(request);

        // assert
        assertNotNull(response);
        assertEquals("Ada Lovelace", response.fullName());
        assertEquals("ada@ex.com", response.email());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmail("ada@ex.com");
        verify(userRepository).save(captor.capture());

        User toSave = captor.getValue();
        assertEquals("Ada Lovelace", toSave.getFullName());
        assertEquals("ada@ex.com", toSave.getEmail());

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void create_errorCase_duplicateEmail_throwsDuplicateEmailException_andDoesNotSave() {
        // arrange
        UserCreateRequest request = new UserCreateRequest("Ada Lovelace", "ada@ex.com");
        when(userRepository.existsByEmail("ada@ex.com")).thenReturn(true);

        // act
        assertThrows(DuplicateEmailException.class, () -> userService.create(request));

        // assert
        verify(userRepository).existsByEmail("ada@ex.com");
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getAll_happyPath_mapsEntitiesToResponses() {
        // arrange
        Pageable pageable = PageRequest.of(0, 2);

        User u1 = new User("A", "a@ex.com");
        User u2 = new User("B", "b@ex.com");

        Page<User> page = new PageImpl<>(List.of(u1, u2), pageable, 2);
        when(userRepository.findAll(pageable)).thenReturn(page);

        // act
        Page<UserResponse> result = userService.getAll(pageable);

        // assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        UserResponse r1 = result.getContent().get(0);
        assertEquals("A", r1.fullName());
        assertEquals("a@ex.com", r1.email());

        UserResponse r2 = result.getContent().get(1);
        assertEquals("B", r2.fullName());
        assertEquals("b@ex.com", r2.email());

        verify(userRepository).findAll(pageable);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getById_happyPath_returnsMappedResponse() {
        // arrange
        User user = new User("Grace Hopper", "grace@ex.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        // act
        UserResponse response = userService.getById(7L);

        // assert
        assertNotNull(response);
        assertEquals("Grace Hopper", response.fullName());
        assertEquals("grace@ex.com", response.email());

        verify(userRepository).findById(7L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getById_errorCase_notFound_throwsUserNotFoundException() {
        // arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // act
        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));

        // assert
        verify(userRepository).findById(99L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void delete_happyPath_delegatesToRepository() {
        // act
        userService.delete(5L);

        // assert
        verify(userRepository).deleteById(5L);
        verifyNoMoreInteractions(userRepository);
    }
}