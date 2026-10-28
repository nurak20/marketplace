package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    boolean existsByPeriodYearAndPeriodMonth(Integer periodYear, Integer periodMonth);
}
