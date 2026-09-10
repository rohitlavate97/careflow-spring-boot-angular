package com.careflow.identity.service;

import com.careflow.identity.domain.User;
import com.careflow.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService loading User aggregates with eager fetch-joined roles and permissions.
 */
@Service
public class CareFlowUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CareFlowUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameWithRolesAndPermissions(usernameOrEmail)
                .or(() -> userRepository.findByEmailWithRolesAndPermissions(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail));

        return new CustomUserDetails(user);
    }
}
