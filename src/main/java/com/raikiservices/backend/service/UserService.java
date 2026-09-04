package com.raikiservices.backend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.user.UserCreateRequest;
import com.raikiservices.backend.dto.user.UserResponse;
import com.raikiservices.backend.dto.user.UserUpdateRequest;
import com.raikiservices.backend.entity.BlogPost;
import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.entity.UserStatus;
import com.raikiservices.backend.exception.BusinessRuleException;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.BlogPostRepository;
import com.raikiservices.backend.repository.EmailVerificationTokenRepository;
import com.raikiservices.backend.repository.LoginHistoryRepository;
import com.raikiservices.backend.repository.NotificationPreferenceRepository;
import com.raikiservices.backend.repository.PasswordResetTokenRepository;
import com.raikiservices.backend.repository.PushSubscriptionRepository;
import com.raikiservices.backend.repository.RoleRepository;
import com.raikiservices.backend.repository.UserRepository;

/**
 * Administration des comptes.
 *
 * <p>Deux garde-fous encadrent les opérations destructrices : on ne supprime pas son propre
 * compte, et on ne retire pas les droits du dernier compte capable d'administrer les
 * utilisateurs — sans quoi plus personne ne pourrait rendre la main, faute d'écran de secours.
 */
@Service
public class UserService {

    /**
     * Permission témoin du verrouillage : tant qu'un compte actif la détient, l'administration
     * des comptes reste accessible. Raisonner sur la permission plutôt que sur le rôle ADMIN
     * couvre aussi les rôles sur mesure créés depuis le back-office.
     */
    private static final String ADMINISTRATION_PERMISSION = "UPDATE_USER";

    /** Champs sur lesquels le client peut trier — le paramètre finit dans un ORDER BY. */
    private static final Set<String> SORTABLE = Set.of(
            "username", "email", "firstName", "lastName", "position", "status", "createdAt");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // Dépendances des seules suppressions : un compte est référencé par ses jetons, son
    // historique, ses préférences et ses abonnements push, qu'il faut retirer avant lui.
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final BlogPostRepository blogPostRepository;

    public UserService(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            LoginHistoryRepository loginHistoryRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            PushSubscriptionRepository pushSubscriptionRepository,
            BlogPostRepository blogPostRepository) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.blogPostRepository = blogPostRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAll(PageQuery query) {
        Pageable pageable = query.toPageable(SORTABLE, "createdAt");
        String filter = query.filterOrNull();

        Page<User> result = filter == null
                ? userRepository.findAll(pageable)
                : userRepository.search(filter, pageable);

        return PageResponse.from(result, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    /**
     * Crée un compte immédiatement utilisable : c'est un administrateur qui le crée, l'adresse
     * n'a donc pas à être prouvée par OTP comme à l'inscription publique.
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Un compte utilise déjà l'adresse " + request.email() + ".");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Le nom d'utilisateur " + request.username() + " est déjà pris.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setPosition(request.position());
        user.setBio(request.bio());
        user.setPhotoUrl(request.photoUrl());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setStatus(request.status() == null ? UserStatus.ACTIVE : request.status());
        user.setRoles(resolveRoles(request.roleIds()));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findOrThrow(id);

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Un compte utilise déjà l'adresse " + request.email() + ".");
        }
        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Le nom d'utilisateur " + request.username() + " est déjà pris.");
        }

        Set<Role> roles = request.roleIds() == null ? user.getRoles() : resolveRoles(request.roleIds());
        boolean enabled = request.enabled() == null ? user.isEnabled() : request.enabled();
        ensureAdministrationStaysReachable(user, grantsAdministration(roles, enabled));

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setPosition(request.position());
        user.setBio(request.bio());
        user.setPhotoUrl(request.photoUrl());
        user.setEnabled(enabled);
        user.setRoles(roles);
        if (request.status() != null) {
            user.setStatus(request.status());
        }

        // Un mot de passe réécrit invalide les sessions ouvertes : c'est le geste attendu quand
        // on reprend la main sur un compte compromis. Désactiver le compte a le même effet.
        boolean passwordChanged = request.password() != null && !request.password().isBlank();
        if (passwordChanged) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        User saved = userRepository.save(user);
        if (passwordChanged || !enabled) {
            refreshTokenService.closeAllSessions(saved);
        }

        return UserResponse.from(saved);
    }

    /**
     * Supprime définitivement un compte et tout ce qui le référence. Les articles qu'il a
     * signés sont conservés, simplement détachés de leur auteur : le contenu publié ne doit
     * pas disparaître avec le compte qui l'a rédigé.
     */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        User user = findOrThrow(id);

        if (user.getId().equals(currentUserId)) {
            throw new BusinessRuleException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        ensureAdministrationStaysReachable(user, false);

        refreshTokenService.closeAllSessions(user);
        emailVerificationTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        loginHistoryRepository.deleteByUser(user);
        notificationPreferenceRepository.deleteByUser(user);
        pushSubscriptionRepository.deleteByUser(user);

        List<BlogPost> authored = blogPostRepository.findByAuthor(user);
        authored.forEach(post -> post.setAuthor(null));
        blogPostRepository.saveAll(authored);

        userRepository.delete(user);
    }

    // ──────────────── Interne ────────────────

    /**
     * Refuse l'opération si elle retirerait la capacité d'administrer les comptes au dernier
     * compte qui la détient.
     *
     * @param stillAdministratorAfterwards état visé du compte : s'il reste administrateur,
     *                                     il n'y a rien à vérifier
     */
    private void ensureAdministrationStaysReachable(User target, boolean stillAdministratorAfterwards) {
        if (stillAdministratorAfterwards) {
            return;
        }
        // L'état en base est encore celui d'avant l'opération : on cherche donc un autre
        // détenteur actif que la cible.
        boolean anotherAdministratorRemains = userRepository.findByPermission(ADMINISTRATION_PERMISSION)
                .stream()
                .anyMatch(other -> !other.getId().equals(target.getId()));

        if (!anotherAdministratorRemains) {
            throw new BusinessRuleException(
                    "Ce compte est le dernier à pouvoir administrer les utilisateurs :"
                            + " donnez ce droit à un autre compte avant de le retirer à celui-ci.");
        }
    }

    private boolean grantsAdministration(Set<Role> roles, boolean enabled) {
        if (!enabled || roles == null) {
            return false;
        }
        return roles.stream()
                .filter(role -> role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> ADMINISTRATION_PERMISSION.equals(permission.getName()));
    }

    /** Un identifiant inconnu est signalé plutôt qu'ignoré : le compte enregistré serait faux. */
    private Set<Role> resolveRoles(Set<Long> roleIds) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleIds == null) {
            return roles;
        }
        for (Long roleId : roleIds) {
            roles.add(roleRepository.findById(roleId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Rôle", roleId)));
        }
        return roles;
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", id));
    }
}
