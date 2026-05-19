package com.ecom.gupet.modules.pet.dto;

import com.ecom.gupet.modules.pet.entity.PetStatus;
import com.ecom.gupet.modules.pet.entity.Species;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PetResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private PetStatus status;
    private Species species;
    private String breed;
    private String color;
    private Boolean gender;
    private BigDecimal weight;
    private LocalDate birthDate;
    private List<PetImageDto> images;
    private String sellerName;   // tên người bán
}