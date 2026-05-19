package com.ecom.gupet.modules.pet.dto;

import com.ecom.gupet.modules.pet.entity.PetStatus;
import com.ecom.gupet.modules.pet.entity.Species;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PetRequest {

    @NotBlank(message = "Pet Chip Code không được để trống")
    @Pattern(regexp = "^[A-Z0-9]{15}$",
            message = "Pet Chip Code phải là đúng 15 ký tự chữ hoa và số")
    private String petChipCode;

    @NotBlank(message = "Tên thú cưng không được để trống")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @Positive(message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull
    private PetStatus status = PetStatus.AVAILABLE;

    @NotNull
    private Species species;

    private String breed;
    private String color;

    private Boolean gender;
    private BigDecimal weight;

    private LocalDate birthDate;

    @NotEmpty(message = "Phải có ít nhất 1 ảnh")
    private List<String> imageUrls;
}