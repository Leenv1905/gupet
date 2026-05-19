package com.ecom.gupet.modules.pet.entity;

import com.ecom.gupet.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pet_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false)
    private String imageUrl;

    private String thumbnailUrl;   // Optional: ảnh nhỏ

    @Column(nullable = false)
    private Integer displayOrder = 0;   // Thứ tự hiển thị ảnh
}