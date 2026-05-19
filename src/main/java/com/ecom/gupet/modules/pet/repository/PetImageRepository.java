package com.ecom.gupet.modules.pet.repository;

import com.ecom.gupet.modules.pet.entity.PetImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetImageRepository extends JpaRepository<PetImage, Long> {
}