package com.oeurn_nurak.marketplace.service;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.*;
import com.oeurn_nurak.marketplace.entity.*;
import com.oeurn_nurak.marketplace.exception.*;
import com.oeurn_nurak.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AccessService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRepository users;
    private final AccountService accounts;
    private final CurrentUser current;

    public Role role(Long id) {
        return roles.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    public Permission permission(Long id) {
        return permissions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }

    @Transactional(readOnly = true)
    public PageDto<Role> roles(Pageable page) { return PageDto.from(roles.findAll(page)); }

    @Transactional(readOnly = true)
    public PageDto<Permission> permissions(Pageable page) { return PageDto.from(permissions.findAll(page)); }

    public Role createRole(RoleRequest request) {
        if (roles.findByName(request.name()).isPresent()) throw new ConflictException("Role name already exists");
        return roles.save(Role.builder().name(request.name()).description(request.description()).permissions(new HashSet<>()).build());
    }

    public Role updateRole(Long id, RoleRequest request) {
        Role role = role(id);
        if (Set.of("ROLE_ADMIN", "ROLE_USER").contains(role.getName()) && !role.getName().equals(request.name()))
            throw new BadRequestException("Built-in role names cannot change");
        roles.findByName(request.name()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("Role name already exists"); });
        role.setName(request.name()); role.setDescription(request.description());
        return roles.save(role);
    }

    public void deleteRole(Long id) {
        Role role = role(id);
        if (Set.of("ROLE_ADMIN", "ROLE_USER").contains(role.getName())) throw new BadRequestException("Built-in roles cannot be deleted");
        if (users.existsByRoles_Id(id)) throw new ConflictException("Role is assigned to users; remove assignments first");
        roles.delete(role);
    }

    public Permission createPermission(PermissionRequest request) {
        if (permissions.findByName(request.name()).isPresent()) throw new ConflictException("Permission name already exists");
        return permissions.save(Permission.builder().name(request.name()).description(request.description()).build());
    }

    public Permission updatePermission(Long id, PermissionRequest request) {
        Permission permission = permission(id);
        if (!permission.getName().equals(request.name()) && roles.existsByPermissions_Id(id))
            throw new ConflictException("Assigned permissions cannot be renamed");
        permissions.findByName(request.name()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("Permission name already exists"); });
        permission.setName(request.name()); permission.setDescription(request.description());
        return permissions.save(permission);
    }

    public void deletePermission(Long id) {
        Permission permission = permission(id);
        if (roles.existsByPermissions_Id(id)) throw new ConflictException("Permission is assigned to roles; remove assignments first");
        permissions.delete(permission);
    }

    public Role setPermissions(Long roleId, Set<Long> ids) {
        Role role = role(roleId);
        Set<Permission> selected = new HashSet<>();
        ids.forEach(id -> selected.add(permission(id)));
        if (role.getName().equals("ROLE_ADMIN") &&
                !selected.stream().map(Permission::getName).toList().containsAll(List.of("admin:access", "user:manage")))
            throw new BadRequestException("Admin role must retain admin:access and user:manage");
        role.setPermissions(selected);
        return roles.save(role);
    }

    public Role addPermission(Long roleId, Long permissionId) {
        Role role = role(roleId);
        Set<Long> ids = new HashSet<>();
        role.getPermissions().forEach(p -> ids.add(p.getId()));
        ids.add(permissionId);
        return setPermissions(roleId, ids);
    }

    public Role removePermission(Long roleId, Long permissionId) {
        permission(permissionId);
        Role role = role(roleId);
        Set<Long> ids = new HashSet<>();
        role.getPermissions().stream().filter(p -> !p.getId().equals(permissionId)).forEach(p -> ids.add(p.getId()));
        return setPermissions(roleId, ids);
    }

    @Transactional(readOnly = true)
    public Set<Role> userRoles(Long userId) { return accounts.requireUser(userId).getRoles(); }

    @Transactional(readOnly = true)
    public Set<String> userPermissions(Long userId) {
        Set<String> result = new TreeSet<>();
        userRoles(userId).forEach(role -> role.getPermissions().forEach(p -> result.add(p.getName())));
        return result;
    }

    public AccountResponse setUserRoles(Long userId, Set<Long> ids) {
        User user = accounts.requireUser(userId);
        Set<Role> selected = new HashSet<>();
        ids.forEach(id -> selected.add(role(id)));
        if (selected.isEmpty()) throw new BadRequestException("User must retain at least one role");
        if (current.id().equals(userId) && selected.stream().noneMatch(r -> r.getName().equals("ROLE_ADMIN")))
            throw new BadRequestException("You cannot remove your own admin role");
        user.setRoles(selected);
        return AccountResponse.from(users.save(user));
    }

    public AccountResponse addUserRole(Long userId, Long roleId) {
        Set<Long> ids = new HashSet<>();
        userRoles(userId).forEach(r -> ids.add(r.getId()));
        ids.add(roleId);
        return setUserRoles(userId, ids);
    }

    public AccountResponse removeUserRole(Long userId, Long roleId) {
        role(roleId);
        Set<Long> ids = new HashSet<>();
        userRoles(userId).stream().filter(r -> !r.getId().equals(roleId)).forEach(r -> ids.add(r.getId()));
        return setUserRoles(userId, ids);
    }
}
