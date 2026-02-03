package com.cemalcigdem.wallet.repository;

import com.cemalcigdem.wallet.domain.Account;
import com.cemalcigdem.wallet.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccount(Account account);

    @Query("""
            select t
            from Transaction t
            where t.account.id = :accountId
            order by t.createdAt desc
            """)
    List<Transaction> findLatestByAccountId(@Param("accountId") Long accountId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findFirstByIdempotencyKey(String idempotencyKey);
}
