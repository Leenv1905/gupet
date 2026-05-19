package com.ecom.gupet.modules.pet.entity;

import com.ecom.gupet.common.entity.BaseEntity;
import com.ecom.gupet.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;                    // Người bán (có thể là SHOP hoặc USER)

    /** ==================== PET CHIP CODE ==================== */
    @Column(unique = true, nullable = false, length = 15, updatable = false)
    private String petChipCode;          // Ví dụ: CZAREP8B1LVXNV3 - KHÔNG ĐƯỢC THAY ĐỔI

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // Trạng thái: AVAILABLE, SOLD, RESERVED, NOT_FOR_SALE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetStatus status = PetStatus.AVAILABLE;

    // Loài: DOG, CAT, OTHER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    private String breed;
    private String color;

    private Boolean gender;                 // true = Male, false = Female

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    private LocalDate birthDate;

    // Quan hệ 1 Pet - Nhiều ảnh
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PetImage> images = new ArrayList<>();

    // Helper method để thêm ảnh an toàn
    public void addImage(PetImage image) {
        if (image != null) {
            if (this.images == null) {
                this.images = new ArrayList<>();
            }
            this.images.add(image);
            image.setPet(this);
        }
    }
}