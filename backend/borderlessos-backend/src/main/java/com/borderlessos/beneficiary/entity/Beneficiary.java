package com.borderlessos.beneficiary.entity;

import com.borderlessos.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_beneficiaries_customer")
    )
    private User customer;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "destination_country",
            nullable = false,
            columnDefinition = "char(2)"
    )
    private String destinationCountry;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "destination_currency",
            nullable = false,
            columnDefinition = "char(3)"
    )
    private String destinationCurrency;

    @Column(name = "account_reference", nullable = false, length = 64)
    private String accountReference;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Beneficiary(
            User customer,
            String recipientName,
            String destinationCountry,
            String destinationCurrency,
            String accountReference
    ) {
        this.customer = customer;
        this.recipientName = recipientName;
        this.destinationCountry = destinationCountry;
        this.destinationCurrency = destinationCurrency;
        this.accountReference = accountReference;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
    }
}