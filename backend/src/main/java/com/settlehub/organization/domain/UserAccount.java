package com.settlehub.organization.domain;

import com.settlehub.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder
    private UserAccount(
            String email,
            String passwordHash,
            String name,
            UserRole role,
            Agency agency,
            Merchant merchant,
            boolean active
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.agency = agency;
        this.merchant = merchant;
        this.active = active;
    }

    public static UserAccount admin(String email, String passwordHash, String name) {
        return UserAccount.builder()
                .email(email)
                .passwordHash(passwordHash)
                .name(name)
                .role(UserRole.ADMIN)
                .active(true)
                .build();
    }

    public static UserAccount agencyUser(String email, String passwordHash, String name, Agency agency) {
        return UserAccount.builder()
                .email(email)
                .passwordHash(passwordHash)
                .name(name)
                .role(UserRole.AGENCY)
                .agency(agency)
                .active(true)
                .build();
    }

    public static UserAccount merchantUser(String email, String passwordHash, String name, Merchant merchant) {
        return UserAccount.builder()
                .email(email)
                .passwordHash(passwordHash)
                .name(name)
                .role(UserRole.MERCHANT)
                .agency(merchant.getAgency())
                .merchant(merchant)
                .active(true)
                .build();
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void changeEmail(String email) {
        this.email = email;
    }
}
