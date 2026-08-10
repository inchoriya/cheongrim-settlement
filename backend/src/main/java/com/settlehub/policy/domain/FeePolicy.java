package com.settlehub.policy.domain;

import com.settlehub.common.persistence.BaseTimeEntity;
import com.settlehub.organization.domain.Agency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "fee_policies",
        indexes = @Index(name = "idx_fee_policy_agency_period", columnList = "agency_id, effective_from, effective_to")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeePolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * null이면 글로벌 기본 정책.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "platform_fee_bps", nullable = false)
    private int platformFeeBps;

    @Column(name = "agency_fee_bps", nullable = false)
    private int agencyFeeBps;

    @Column(name = "rider_fee", nullable = false)
    private long riderFee;

    @Column(name = "effective_from", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to", columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder
    private FeePolicy(
            Agency agency,
            String name,
            int platformFeeBps,
            int agencyFeeBps,
            long riderFee,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            boolean active
    ) {
        this.agency = agency;
        this.name = name;
        this.platformFeeBps = platformFeeBps;
        this.agencyFeeBps = agencyFeeBps;
        this.riderFee = riderFee;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.active = active;
    }

    public boolean isGlobal() {
        return agency == null;
    }

    public boolean isEffectiveAt(LocalDateTime at) {
        if (!active) {
            return false;
        }
        if (at.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || at.isBefore(effectiveTo);
    }
}
