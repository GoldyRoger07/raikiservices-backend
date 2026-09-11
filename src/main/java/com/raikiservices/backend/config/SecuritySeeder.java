package com.raikiservices.backend.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.entity.Permission;
import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.entity.UserStatus;
import com.raikiservices.backend.repository.PermissionRepository;
import com.raikiservices.backend.repository.RoleRepository;
import com.raikiservices.backend.repository.UserRepository;

/**
 * Crée au démarrage les permissions et rôles système, puis le compte administrateur initial.
 *
 * <p>Le nommage suit la convention du backend Secogroupe — {@code ACTION_RESSOURCE} — pour que
 * le code porté depuis ce projet et un frontend écrit contre lui fonctionnent sans retouche.
 *
 * <p>Idempotent : chaque exécution complète ce qui manque sans toucher à l'existant. Le mot de
 * passe d'un compte administrateur déjà présent n'est jamais réécrit.
 */
@Component
public class SecuritySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecuritySeeder.class);

    /** Nom de la permission -> {module, action, description}. */
    private static final Map<String, String[]> PERMISSIONS = new LinkedHashMap<>();
    static {
        // Blog
        perm("CREATE_BLOG", "BLOG", "CREATE", "Créer un article de blog");
        perm("READ_BLOG", "BLOG", "READ", "Voir les articles, brouillons compris");
        perm("UPDATE_BLOG", "BLOG", "UPDATE", "Modifier un article et le publier");
        perm("DELETE_BLOG", "BLOG", "DELETE", "Supprimer un article");
        // Réalisations présentées sur le site vitrine
        perm("CREATE_PROJECT", "PROJECTS", "CREATE", "Créer une réalisation");
        perm("READ_PROJECT", "PROJECTS", "READ", "Voir les réalisations, brouillons compris");
        perm("UPDATE_PROJECT", "PROJECTS", "UPDATE",
                "Modifier une réalisation, la publier et réordonner la vitrine");
        perm("DELETE_PROJECT", "PROJECTS", "DELETE", "Supprimer une réalisation");
        // Bibliothèque d'images
        perm("CREATE_MEDIA", "MEDIA", "CREATE", "Envoyer des images dans la bibliothèque");
        perm("READ_MEDIA", "MEDIA", "READ", "Voir la bibliothèque d'images");
        perm("UPDATE_MEDIA", "MEDIA", "UPDATE", "Modifier le texte alternatif d'une image");
        perm("DELETE_MEDIA", "MEDIA", "DELETE", "Supprimer une image de la bibliothèque");
        // Messages du formulaire de contact
        perm("READ_CONTACT", "CONTACT", "READ", "Voir les messages du formulaire de contact");
        perm("UPDATE_CONTACT", "CONTACT", "UPDATE", "Changer le statut et annoter un message");
        perm("DELETE_CONTACT", "CONTACT", "DELETE", "Supprimer un message de contact");
        // Comptes
        perm("CREATE_USER", "USERS", "CREATE", "Créer un utilisateur");
        perm("READ_USER", "USERS", "READ", "Voir les utilisateurs");
        perm("UPDATE_USER", "USERS", "UPDATE", "Modifier un utilisateur");
        perm("DELETE_USER", "USERS", "DELETE", "Supprimer un utilisateur");
        // Rôles
        perm("CREATE_ROLE", "ROLES", "CREATE", "Créer un rôle");
        perm("READ_ROLE", "ROLES", "READ", "Voir les rôles");
        perm("UPDATE_ROLE", "ROLES", "UPDATE", "Modifier un rôle");
        perm("DELETE_ROLE", "ROLES", "DELETE", "Supprimer un rôle");
        // Permissions
        perm("CREATE_PERMISSION", "PERMISSIONS", "CREATE", "Créer une permission");
        perm("READ_PERMISSION", "PERMISSIONS", "READ", "Voir les permissions");
        perm("UPDATE_PERMISSION", "PERMISSIONS", "UPDATE", "Modifier une permission");
        perm("DELETE_PERMISSION", "PERMISSIONS", "DELETE", "Supprimer une permission");
        // Sessions actives
        perm("READ_SESSION", "SESSIONS", "READ", "Voir les sessions actives");
        perm("DELETE_SESSION", "SESSIONS", "DELETE", "Révoquer une session");
        // Réglages du site vitrine (offre de lancement)
        perm("READ_SETTING", "SETTINGS", "READ", "Voir les réglages du site");
        perm("UPDATE_SETTING", "SETTINGS", "UPDATE", "Modifier les réglages du site, dont l'offre de lancement");
        // Notifications
        perm("READ_NOTIFICATION_SETTINGS", "NOTIFICATIONS", "READ", "Voir les paramètres de notifications");
        perm("UPDATE_NOTIFICATION_SETTINGS", "NOTIFICATIONS", "UPDATE", "Configurer les notifications");
    }

    private static void perm(String name, String module, String action, String description) {
        PERMISSIONS.put(name, new String[] { module, action, description });
    }

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EDITOR = "EDITOR";

    private static final List<String> EDITOR_PERMISSIONS = List.of(
            "CREATE_BLOG", "READ_BLOG", "UPDATE_BLOG",
            "CREATE_PROJECT", "READ_PROJECT", "UPDATE_PROJECT",
            "CREATE_MEDIA", "READ_MEDIA", "UPDATE_MEDIA",
            "READ_CONTACT", "UPDATE_CONTACT");

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public SecuritySeeder(PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {

        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Permission> permissions = seedPermissions();
        Role adminRole = seedRole(ROLE_ADMIN, "Accès complet à l'administration",
                new LinkedHashSet<>(permissions.values()));
        seedRole(ROLE_EDITOR, "Rédaction du blog, gestion des réalisations et suivi des messages",
                collect(permissions, EDITOR_PERMISSIONS));
        seedAdminUser(adminRole);
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> result = new LinkedHashMap<>();
        PERMISSIONS.forEach((name, meta) -> {
            Permission permission = permissionRepository.findByName(name).orElseGet(() -> {
                Permission created = new Permission(name);
                created.setModule(meta[0]);
                created.setAction(meta[1]);
                created.setDescription(meta[2]);
                created.setSystem(true);
                log.info("Permission système créée : {}", name);
                return permissionRepository.save(created);
            });
            result.put(name, permission);
        });
        return result;
    }

    private Role seedRole(String name, String description, Set<Permission> permissions) {
        Role role = roleRepository.findByName(name).orElseGet(() -> {
            Role created = new Role(name);
            created.setDescription(description);
            log.info("Rôle système créé : {}", name);
            return created;
        });
        role.setSystem(true);
        // Les permissions des rôles système sont réalignées à chaque démarrage : ajouter une
        // permission au code suffit à la propager, sans migration manuelle.
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    private Set<Permission> collect(Map<String, Permission> permissions, List<String> names) {
        Set<Permission> result = new LinkedHashSet<>();
        names.forEach(name -> result.add(permissions.get(name)));
        return result;
    }

    private void seedAdminUser(Role adminRole) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("app.admin.email / app.admin.password non renseignés : "
                    + "aucun compte administrateur initial créé.");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername(adminEmail.substring(0, adminEmail.indexOf('@')));
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        log.warn("Compte administrateur initial créé pour {} — changez ce mot de passe "
                + "puis retirez app.admin.password de la configuration.", adminEmail);
    }
}