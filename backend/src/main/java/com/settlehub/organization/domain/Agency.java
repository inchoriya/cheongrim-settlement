package com.settlehub.organization.domain;

import com.settlehub.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "agencies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agency extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder
    private Agency(String code, String name, boolean active) {
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public static Agency create(String code, String name) {
        return Agency.builder()
                .code(code)
                .name(name)
                .active(true)
                .build();
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
