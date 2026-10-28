package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Payslip;
import com.oeurn_nurak.marketplace.entity.enums.PayrollRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    Page<Payslip> findByRunId(Long runId, Pageable pageable);
    List<Payslip> findByRunIdOrderByIdAsc(Long runId);
    Optional<Payslip> findByIdAndUserId(Long id, Long userId);
    long countByRunId(Long runId);

    @Query("""
            SELECT p FROM Payslip p
            WHERE p.userId = :userId
              AND p.runId IN (SELECT r.id FROM PayrollRun r WHERE r.status IN :statuses)
            """)
    Page<Payslip> findVisibleByUserId(@Param("userId") Long userId,
                                      @Param("statuses") Collection<PayrollRunStatus> statuses,
                                      Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Payslip p WHERE p.runId = :runId")
    void deleteAllByRunId(@Param("runId") Long runId);
}
