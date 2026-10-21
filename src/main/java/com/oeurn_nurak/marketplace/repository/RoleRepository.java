package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByPermissions_Id(Long permissionId);
}
