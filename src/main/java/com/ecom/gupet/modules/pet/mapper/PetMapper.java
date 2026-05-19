package com.ecom.gupet.modules.pet.mapper;

import com.ecom.gupet.modules.pet.dto.PetImageDto;
import com.ecom.gupet.modules.pet.dto.PetRequest;
import com.ecom.gupet.modules.pet.dto.PetResponse;
import com.ecom.gupet.modules.pet.entity.Pet;
import com.ecom.gupet.modules.pet.entity.PetImage;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper chuyển đổi giữa PetRequest, Pet Entity và PetResponse
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PetMapper {

    /**
     * Map từ Request → Entity khi tạo mới Pet
     */
    @Mapping(target = "seller", ignore = true)           // Set thủ công trong Service
    @Mapping(target = "images", ignore = true)           // Xử lý riêng trong Service
    @Mapping(target = "petChipCode", source = "petChipCode")
    @Mapping(target = "birthDate", source = "birthDate")
    Pet toEntity(PetRequest request);

    /**
     * Map từ Entity → Response
     */
    @Mapping(target = "sellerName", source = "seller.fullName")
    @Mapping(target = "images", source = "images")
    PetResponse toResponse(Pet pet);

    List<PetResponse> toResponseList(List<Pet> pets);

    // Map PetImage
    PetImageDto toImageDto(PetImage petImage);
    List<PetImageDto> toImageDtoList(List<PetImage> images);

    /**
     *  chức năng edit
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "petChipCode", ignore = true)   // Không cho update chip code
    void updateEntityFromRequest(PetRequest request, @MappingTarget Pet pet);
}