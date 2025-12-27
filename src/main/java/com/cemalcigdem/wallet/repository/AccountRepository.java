package com.cemalcigdem.wallet.repository;

import com.cemalcigdem.wallet.domain.Account;
import com.cemalcigdem.wallet.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    boolean existsByUserAndCurrency(User user, String currency);
}