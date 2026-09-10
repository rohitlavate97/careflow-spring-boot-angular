package com.careflow.identity.service;

import com.careflow.identity.domain.User;
import com.careflow.identity.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring Security UserDetails adapter wrapping CareFlow User entity.
 */
public class CustomUserDetails implements UserDetails {

    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final boolean accountNonLocked;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.status = user.getStatus();
        this.accountNonLocked = user.isAccountNonLocked();

        Set<GrantedAuthority> auths = new HashSet<>();
        user.getRoles().forEach(role -> {
            auths.add(new SimpleGrantedAuthority(role.getName().name()));
            role.getPermissions().forEach(permission ->
                    auths.add(new SimpleGrantedAuthority(permission.getName())));
        });
        this.authorities = auths;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
