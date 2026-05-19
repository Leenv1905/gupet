package com.ecom.gupet.modules.pet.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetImageDto {
    private Long id;
    private String imageUrl;
    private String thumbnailUrl;
    private Integer displayOrder;
}