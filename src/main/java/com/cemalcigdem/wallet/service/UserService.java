package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.User;
import com.cemalcigdem.wallet.dto.UserCreateRequest;
import com.cemalcigdem.wallet.dto.UserResponse;
import com.cemalcigdem.wallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return null;
        return toResponse(user);
    }

    public UserResponse create(UserCreateRequest request) {
        User user = new User(request.getFullName(), request.getEmail());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
