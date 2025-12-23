package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.User;
import com.cemalcigdem.wallet.dto.UserCreateRequest;
import com.cemalcigdem.wallet.dto.UserResponse;
import com.cemalcigdem.wallet.exception.DuplicateEmailException;
import com.cemalcigdem.wallet.exception.UserNotFoundException;
import com.cemalcigdem.wallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        User user = new User(request.getFullName(), request.getEmail());
        return toResponse(userRepository.save(user));
    }

    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
