package com.cemalcigdem.wallet.repository;

import com.cemalcigdem.wallet.domain.Transaction;
import com.cemalcigdem.wallet.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findFirstByIdempotencyKey(String idempotencyKey);

    @Query("""
                select t
                from Transaction t
                where t.account.id = :accountId
                  and (:type is null or t.type = :type)
                  and (:from is null or t.createdAt >= :from)
                  and (:to is null or t.createdAt <= :to)
            """)
    Page<Transaction> findStatement(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
