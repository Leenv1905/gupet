package com.ecom.gupet.modules.pet.repository;

import com.ecom.gupet.modules.pet.entity.PetChip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetChipRepository extends JpaRepository<PetChip, Long> {

    Optional<PetChip> findByChipCode(String chipCode);

    boolean existsByChipCode(String chipCode);

    Optional<PetChip> findByPetId(Long petId);
}