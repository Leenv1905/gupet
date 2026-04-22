package com.ecom.gupet.common.config;

import com.ecom.gupet.modules.user.entity.Role;
import com.ecom.gupet.modules.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        List<String> roleNames = List.of("ROLE_USER", "ROLE_SHOP", "ROLE_OPERATOR", "ROLE_ADMIN");

        for (String roleName : roleNames) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
                        .name(roleName)
                        .build();
                roleRepository.save(role);
                System.out.println("✅ Created role: " + roleName);
            }
        }
    }
}