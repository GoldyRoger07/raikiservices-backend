package com.raikiservices.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.entity.LoginHistory;
import com.raikiservices.backend.repository.LoginHistoryRepository;
import com.raikiservices.backend.repository.UserRepository;

@Service
public class LoginHistoryService {

    private static final int DEVICE_MAX_LENGTH = 400;

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    public LoginHistoryService(LoginHistoryRepository loginHistoryRepository,
            UserRepository userRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Consigne une tentative de connexion.
     *
     * <p>Une tentative sur un identifiant inconnu n'est rattachée à aucun compte et n'est donc
     * pas enregistrée : créer une ligne orpheline reviendrait à constituer une liste des
     * identifiants essayés, sans valeur de suivi pour l'utilisateur légitime.
     *
     * <p>Écrit dans sa propre transaction, pour qu'un échec de connexion — qui remonte une
     * exception — n'annule pas la trace qui vient d'être écrite.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void record(String login, String ipAddress, String device, boolean success) {
        userRepository.findByEmailOrUsername(login).ifPresent(user -> {
            LoginHistory history = new LoginHistory();
            history.setUser(user);
            history.setIpAddress(ipAddress);
            history.setDevice(truncate(device));
            history.setSuccess(success);
            loginHistoryRepository.save(history);
        });
    }

    private String truncate(String device) {
        if (device == null) {
            return null;
        }
        return device.length() <= DEVICE_MAX_LENGTH ? device : device.substring(0, DEVICE_MAX_LENGTH);
    }
}
