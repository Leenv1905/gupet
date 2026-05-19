package com.ecom.gupet.modules.pet.repository;

import com.ecom.gupet.modules.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    @Query("SELECT p FROM Pet p LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.seller.id = :sellerId")
    List<Pet> findBySellerId(Long sellerId);

    @Query("SELECT p FROM Pet p LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.status = 'AVAILABLE'")
    List<Pet> findAvailablePets();

    @Query("SELECT p FROM Pet p LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Pet> findByIdWithSellerAndImages(Long id);

    Optional<Pet> findByPetChipCode(String petChipCode);

    boolean existsByPetChipCode(String petChipCode);
}