package com.oeurn_nurak.marketplace.repository;

import com.oeurn_nurak.marketplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByRoles_Id(Long roleId);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roles AND u.isActive = true")
    List<User> findActiveByRoleNames(@Param("roles") Collection<String> roles);
}
