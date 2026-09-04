package com.raikiservices.backend.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.role.RoleRequest;
import com.raikiservices.backend.dto.role.RoleResponse;
import com.raikiservices.backend.entity.Permission;
import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.exception.BusinessRuleException;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.PermissionRepository;
import com.raikiservices.backend.repository.RoleRepository;
import com.raikiservices.backend.repository.UserRepository;

/**
 * Administration des rôles.
 *
 * <p>Un rôle système est un rôle posé par {@code SecuritySeeder}. Seule sa description est
 * modifiable : son nom est repris dans les autorisations {@code ROLE_*}, et son jeu de
 * permissions est réaligné sur le code à chaque démarrage — l'éditer depuis l'API donnerait
 * l'illusion d'un changement que le prochain redémarrage effacerait.
 */
@Service
public class RoleService {

    /** Champs sur lesquels le client peut trier — le paramètre finit dans un ORDER BY. */
    private static final Set<String> SORTABLE = Set.of("name", "createdAt");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository) {

        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> getAll(PageQuery query) {
        Pageable pageable = query.toPageable(SORTABLE, "name");
        String filter = query.filterOrNull();

        Page<Role> result = filter == null
                ? roleRepository.findAll(pageable)
                : roleRepository.search(filter, pageable);

        return PageResponse.from(result, RoleResponse::from);
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return RoleResponse.from(findOrThrow(id));
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Un rôle nommé " + request.name() + " existe déjà.");
        }

        Role role = new Role(request.name());
        role.setDescription(request.description());
        role.setPermissions(resolvePermissions(request.permissionIds()));

        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = findOrThrow(id);

        if (role.isSystem()) {
            return RoleResponse.from(updateSystemRole(role, request));
        }

        if (!role.getName().equals(request.name())) {
            if (roleRepository.existsByName(request.name())) {
                throw new BusinessRuleException("Un rôle nommé " + request.name() + " existe déjà.");
            }
            role.setName(request.name());
        }

        role.setDescription(request.description());
        role.setPermissions(resolvePermissions(request.permissionIds()));

        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        Role role = findOrThrow(id);

        if (role.isSystem()) {
            throw new BusinessRuleException("Le rôle système " + role.getName() + " ne peut pas être supprimé.");
        }

        long holders = userRepository.countByRolesContaining(role);
        if (holders > 0) {
            throw new BusinessRuleException("Le rôle " + role.getName() + " est encore attribué à "
                    + holders + " compte(s). Retirez-le de ces comptes avant de le supprimer.");
        }

        roleRepository.delete(role);
    }

    // ──────────────── Interne ────────────────

    /** Sur un rôle système, seule la description est retenue ; le reste est refusé explicitement. */
    private Role updateSystemRole(Role role, RoleRequest request) {
        if (!role.getName().equals(request.name())) {
            throw new BusinessRuleException("Le rôle système " + role.getName() + " ne peut pas être renommé.");
        }
        if (request.permissionIds() != null && !samePermissions(role, request.permissionIds())) {
            throw new BusinessRuleException("Les permissions du rôle système " + role.getName()
                    + " sont définies dans le code et réappliquées à chaque démarrage :"
                    + " elles ne peuvent pas être modifiées depuis l'API.");
        }

        role.setDescription(request.description());
        return roleRepository.save(role);
    }

    private boolean samePermissions(Role role, Set<Long> permissionIds) {
        Set<Long> current = new LinkedHashSet<>();
        if (role.getPermissions() != null) {
            role.getPermissions().forEach(p -> current.add(p.getId()));
        }
        return current.equals(new LinkedHashSet<>(permissionIds));
    }

    /** Un identifiant inconnu est signalé plutôt qu'ignoré : le rôle enregistré serait faux. */
    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        Set<Permission> permissions = new LinkedHashSet<>();
        if (permissionIds == null) {
            return permissions;
        }
        for (Long permissionId : permissionIds) {
            permissions.add(permissionRepository.findById(permissionId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Permission", permissionId)));
        }
        return permissions;
    }

    private Role findOrThrow(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Rôle", id));
    }
}
