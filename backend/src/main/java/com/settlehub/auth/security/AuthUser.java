package com.settlehub.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.settlehub.organization.domain.UserAccount;
import com.settlehub.organization.domain.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AuthUser(
        Long id,
        String email,
        UserRole role,
        Long agencyId,
        Long merchantId,
        @JsonIgnore String passwordHash
) implements UserDetails {

    public static AuthUser from(UserAccount account) {
        Long agencyId = account.getAgency() == null ? null : account.getAgency().getId();
        Long merchantId = account.getMerchant() == null ? null : account.getMerchant().getId();
        return new AuthUser(
                account.getId(),
                account.getEmail(),
                account.getRole(),
                agencyId,
                merchantId,
                account.getPasswordHash()
        );
    }

    public static AuthUser fromToken(
            Long id,
            String email,
            UserRole role,
            Long agencyId,
            Long merchantId
    ) {
        return new AuthUser(id, email, role, agencyId, merchantId, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
