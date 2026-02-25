package com.cemalcigdem.wallet.repository;

import com.cemalcigdem.wallet.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
