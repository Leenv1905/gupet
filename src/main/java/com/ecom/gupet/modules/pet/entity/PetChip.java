package com.ecom.gupet.modules.pet.entity;

import com.ecom.gupet.common.entity.BaseEntity;
import com.ecom.gupet.modules.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "pet_chips")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetChip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 15, updatable = false)
    private String chipCode;                    // Mã chip vĩnh viễn

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false, unique = true)
    private Pet pet;

    // Thông tin y tế cơ bản (có thể mở rộng sau)
    private String currentHealthStatus = "Healthy";
    private LocalDate lastHealthCheckDate;
    private LocalDate nextVaccinationDate;
    @Column(columnDefinition = "TEXT")
    private String medicalNotes;
}