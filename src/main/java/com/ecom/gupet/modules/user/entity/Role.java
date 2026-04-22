package com.ecom.gupet.modules.user.entity;

import com.ecom.gupet.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;   // ROLE_USER, ROLE_SHOP, ROLE_OPERATOR, ROLE_ADMIN
}