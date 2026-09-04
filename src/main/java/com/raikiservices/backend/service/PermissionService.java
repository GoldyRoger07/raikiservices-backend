package com.raikiservices.backend.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.permission.PermissionRequest;
import com.raikiservices.backend.dto.permission.PermissionResponse;
import com.raikiservices.backend.entity.Permission;
import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.exception.BusinessRuleException;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.PermissionRepository;
import com.raikiservices.backend.repository.RoleRepository;

/**
 * Administration du référentiel de permissions.
 *
 * <p>Les permissions système sont posées par {@code SecuritySeeder} et leurs noms sont écrits
 * en dur dans les {@code @PreAuthorize} des contrôleurs : les renommer ou les supprimer
 * ouvrirait silencieusement des endpoints, ces deux opérations leur sont donc interdites. Le
 * reste — module, action, description — n'a qu'une valeur d'affichage et reste modifiable.
 */
@Service
public class PermissionService {

    /** Champs sur lesquels le client peut trier — le paramètre finit dans un ORDER BY. */
    private static final Set<String> SORTABLE = Set.of("name", "module", "action", "createdAt");

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionService(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getAll(PageQuery query) {
        Pageable pageable = query.toPageable(SORTABLE, "name");
        String filter = query.filterOrNull();

        Page<Permission> result = filter == null
                ? permissionRepository.findAll(pageable)
                : permissionRepository.search(filter, pageable);

        return PageResponse.from(result, PermissionResponse::from);
    }

    @Transactional(readOnly = true)
    public PermissionResponse getById(Long id) {
        return PermissionResponse.from(findOrThrow(id));
    }

    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Une permission nommée " + request.name() + " existe déjà.");
        }

        Permission permission = new Permission(request.name());
        permission.setModule(request.module());
        permission.setAction(request.action());
        permission.setDescription(request.description());

        return PermissionResponse.from(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionResponse update(Long id, PermissionRequest request) {
        Permission permission = findOrThrow(id);

        if (!permission.getName().equals(request.name())) {
            if (permission.isSystem()) {
                throw new BusinessRuleException(
                        "La permission système " + permission.getName() + " ne peut pas être renommée : "
                                + "son nom est référencé dans les règles de sécurité du code.");
            }
            if (permissionRepository.existsByName(request.name())) {
                throw new BusinessRuleException("Une permission nommée " + request.name() + " existe déjà.");
            }
            permission.setName(request.name());
        }

        permission.setModule(request.module());
        permission.setAction(request.action());
        permission.setDescription(request.description());

        return PermissionResponse.from(permissionRepository.save(permission));
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = findOrThrow(id);

        if (permission.isSystem()) {
            throw new BusinessRuleException(
                    "La permission système " + permission.getName() + " ne peut pas être supprimée.");
        }

        // Supprimer une permission encore rattachée laisserait des lignes orphelines dans la
        // table de jointure : on demande plutôt de la retirer des rôles concernés.
        List<Role> holders = roleRepository.findByPermissionsContaining(permission);
        if (!holders.isEmpty()) {
            String names = holders.stream().map(Role::getName).sorted().reduce((a, b) -> a + ", " + b).orElse("");
            throw new BusinessRuleException(
                    "La permission " + permission.getName() + " est encore attribuée aux rôles suivants : "
                            + names + ". Retirez-la de ces rôles avant de la supprimer.");
        }

        permissionRepository.delete(permission);
    }

    private Permission findOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", id));
    }
}
