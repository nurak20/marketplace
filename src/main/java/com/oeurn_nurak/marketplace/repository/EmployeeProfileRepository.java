package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.EmployeeProfile;
import com.oeurn_nurak.marketplace.entity.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long>,
        JpaSpecificationExecutor<EmployeeProfile> {
    Optional<EmployeeProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    boolean existsByEmployeeCode(String employeeCode);
    List<EmployeeProfile> findByStatusIn(Collection<EmploymentStatus> statuses);
}
