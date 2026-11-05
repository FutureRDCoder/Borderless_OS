
package com.borderlessos.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Role {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    public Role(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
}