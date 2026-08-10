package com.settlehub.organization.domain;

import com.settlehub.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "merchants",
        uniqueConstraints = @UniqueConstraint(name = "uk_merchant_agency_code", columnNames = {"agency_id", "code"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Merchant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    @Column(name = "toss_seller_id", length = 40)
    private String tossSellerId;

    @Builder
    private Merchant(
            Agency agency,
            String code,
            String name,
            boolean active,
            String bankCode,
            String accountNumber,
            String accountHolder,
            String tossSellerId
    ) {
        this.agency = agency;
        this.code = code;
        this.name = name;
        this.active = active;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.tossSellerId = tossSellerId;
    }

    public static Merchant create(Agency agency, String code, String name) {
        return Merchant.builder()
                .agency(agency)
                .code(code)
                .name(name)
                .active(true)
                .build();
    }

    public static Merchant createWithPayoutAccount(
            Agency agency,
            String code,
            String name,
            String bankCode,
            String accountNumber,
            String accountHolder
    ) {
        return Merchant.builder()
                .agency(agency)
                .code(code)
                .name(name)
                .active(true)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .build();
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void assignTossSellerId(String tossSellerId) {
        this.tossSellerId = tossSellerId;
    }

    public String refSellerId() {
        // 토스 refSellerId: 7~20자
        String raw = "MCH" + code.replace("-", "");
        if (raw.length() < 7) {
            raw = (raw + "0000000").substring(0, 7);
        }
        return raw.length() > 20 ? raw.substring(0, 20) : raw;
    }
}
