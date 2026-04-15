package com.serviceconnect.config;

import com.serviceconnect.entity.User;
import com.serviceconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed.enabled:true}")
    private boolean adminSeedEnabled;

    @Value("${app.admin.name:System Admin}")
    private String adminName;

    @Value("${app.admin.phone:1111111111}")
    private String adminPhone;

    @Value("${app.admin.email:admin@serviceconnect.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedAdminAccount();
    }

    private void seedAdminAccount() {
        if (!adminSeedEnabled) {
            log.info("Admin seed disabled via configuration.");
            return;
        }

        if (!userRepository.existsByPhone(adminPhone)) {
            User admin = new User();
            admin.setName(adminName);
            admin.setPhone(adminPhone);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.admin);
            userRepository.save(admin);
            log.info("Admin account seeded: phone={}", adminPhone);
        } else {
            log.info("Admin account already exists, skipping seed.");
        }
    }
}
