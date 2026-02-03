package com.cemalcigdem.wallet.repository;

import com.cemalcigdem.wallet.domain.Transaction;
import com.cemalcigdem.wallet.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
            select t
            from Transaction t
            where t.account.id = :accountId
            order by t.createdAt desc
            """)
    List<Transaction> findLatestByAccountId(@Param("accountId") Long accountId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByIdempotencyKeyAndType(String key, TransactionType type);
}
