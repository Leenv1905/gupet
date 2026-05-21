package com.ecom.gupet.modules.order.entity;

import com.ecom.gupet.common.entity.BaseEntity;
import com.ecom.gupet.modules.pet.entity.Pet;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // FK giữ lại để trace, nhưng dữ liệu hiển thị lấy từ productSnapshot
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false)
    private Integer quantity;

    // Giá tại thời điểm mua — không thay đổi dù shop sửa giá sau
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // Snapshot toàn bộ thông tin pet lúc đặt hàng:
    // name, breed, species, color, gender, weight, birthDate,
    // petChipCode, images[0] (ảnh đại diện), sellerName
    // Cấu trúc mẫu:
    // {
    //   "name": "Poodle Toy",
    //   "species": "DOG",
    //   "breed": "Poodle",
    //   "color": "Trắng kem",
    //   "gender": true,
    //   "weight": 1.5,
    //   "birthDate": "2024-01-15",
    //   "petChipCode": "CZAREP8B1LVXNV3",
    //   "thumbnailUrl": "https://...",
    //   "sellerName": "Shop Thú Cưng ABC"
    // }
    @Type(JsonType.class)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Object> productSnapshot;
}