package com.oeurn_nurak.marketplace.controller;

import com.oeurn_nurak.marketplace.dto.request.*;
import com.oeurn_nurak.marketplace.dto.response.ResponseDto;
import com.oeurn_nurak.marketplace.service.AccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin:access')")
public class AccessController {
    private final AccessService access;

    @GetMapping("/roles")
    public ResponseDto<?> roles(Pageable page) { return ResponseDto.ok(access.roles(page)); }
    @GetMapping("/roles/{id}")
    public ResponseDto<?> role(@PathVariable Long id) { return ResponseDto.ok(access.role(id)); }
    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDto<?> createRole(@Valid @RequestBody RoleRequest request) { return ResponseDto.created("Role created", access.createRole(request)); }
    @PutMapping("/roles/{id}")
    public ResponseDto<?> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) { return ResponseDto.ok(access.updateRole(id, request)); }
    @DeleteMapping("/roles/{id}")
    public ResponseDto<?> deleteRole(@PathVariable Long id) { access.deleteRole(id); return ResponseDto.message("Role deleted"); }
    @GetMapping("/roles/{id}/permissions")
    public ResponseDto<?> rolePermissions(@PathVariable Long id) { return ResponseDto.ok(access.role(id).getPermissions()); }
    @PutMapping("/roles/{id}/permissions")
    public ResponseDto<?> setPermissions(@PathVariable Long id, @Valid @RequestBody IdSetRequest request) { return ResponseDto.ok(access.setPermissions(id, request.ids())); }
    @PutMapping("/roles/{id}/permissions/{permissionId}")
    public ResponseDto<?> addPermission(@PathVariable Long id, @PathVariable Long permissionId) { return ResponseDto.ok(access.addPermission(id, permissionId)); }
    @DeleteMapping("/roles/{id}/permissions/{permissionId}")
    public ResponseDto<?> removePermission(@PathVariable Long id, @PathVariable Long permissionId) { return ResponseDto.ok(access.removePermission(id, permissionId)); }

    @GetMapping("/permissions")
    public ResponseDto<?> permissions(Pageable page) { return ResponseDto.ok(access.permissions(page)); }
    @GetMapping("/permissions/{id}")
    public ResponseDto<?> permission(@PathVariable Long id) { return ResponseDto.ok(access.permission(id)); }
    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDto<?> createPermission(@Valid @RequestBody PermissionRequest request) { return ResponseDto.created("Permission created", access.createPermission(request)); }
    @PutMapping("/permissions/{id}")
    public ResponseDto<?> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) { return ResponseDto.ok(access.updatePermission(id, request)); }
    @DeleteMapping("/permissions/{id}")
    public ResponseDto<?> deletePermission(@PathVariable Long id) { access.deletePermission(id); return ResponseDto.message("Permission deleted"); }

    @GetMapping("/users/{id}/roles")
    public ResponseDto<?> userRoles(@PathVariable Long id) { return ResponseDto.ok(access.userRoles(id)); }
    @GetMapping("/users/{id}/permissions")
    public ResponseDto<?> userPermissions(@PathVariable Long id) { return ResponseDto.ok(access.userPermissions(id)); }
    @PutMapping("/users/{id}/roles")
    public ResponseDto<?> setUserRoles(@PathVariable Long id, @Valid @RequestBody IdSetRequest request) { return ResponseDto.ok(access.setUserRoles(id, request.ids())); }
    @PutMapping("/users/{id}/roles/{roleId}")
    public ResponseDto<?> addUserRole(@PathVariable Long id, @PathVariable Long roleId) { return ResponseDto.ok(access.addUserRole(id, roleId)); }
    @DeleteMapping("/users/{id}/roles/{roleId}")
    public ResponseDto<?> removeUserRole(@PathVariable Long id, @PathVariable Long roleId) { return ResponseDto.ok(access.removeUserRole(id, roleId)); }
}
