package com.ainovel.app.economy.repo;

import com.ainovel.app.economy.model.CreditLog;
import com.ainovel.app.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface CreditLogRepository extends JpaRepository<CreditLog, UUID> {
    Page<CreditLog> findByUser(User user, Pageable pageable);

    @Query("select coalesce(sum(case when c.amount < 0 then -c.amount else 0 end), 0) from CreditLog c")
    double totalConsumed();

    @Query("select coalesce(sum(case when c.amount < 0 then -c.amount else 0 end), 0) from CreditLog c where c.createdAt >= :since")
    double consumedSince(@Param("since") Instant since);
}

