package com.ecom.gupet.modules.pet.service;

import com.ecom.gupet.modules.pet.entity.Pet;
import com.ecom.gupet.modules.pet.entity.PetChip;
import com.ecom.gupet.modules.pet.repository.PetChipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service quản lý PetChip - Mã chip định danh vĩnh viễn của thú cưng
 *
 * Mỗi PetChip tương ứng 1:1 với một Pet.
 * Dùng để quản lý thông tin y tế, lịch sử tiêm chủng sau này.
 */
@Service
@RequiredArgsConstructor
public class PetChipService {

    private final PetChipRepository petChipRepository;

    /**
     * Tạo PetChip khi đăng bán thú cưng mới
     *
     * @param pet      Thú cưng đã được lưu
     * @param chipCode Mã chip 15 ký tự (ví dụ: CZAREP8B1LVXNV3)
     * @return PetChip đã tạo
     */
    @Transactional
    public PetChip createPetChip(Pet pet, String chipCode) {
        // Kiểm tra chip code đã tồn tại chưa
        if (petChipRepository.existsByChipCode(chipCode)) {
            throw new RuntimeException("Pet Chip Code đã tồn tại: " + chipCode);
        }

        PetChip petChip = PetChip.builder()
                .chipCode(chipCode)
                .pet(pet)
                .currentHealthStatus("Healthy")
                .lastHealthCheckDate(LocalDate.now())
                .build();

        return petChipRepository.save(petChip);
    }

    /**
     * Tìm PetChip theo mã chip
     */
    public Optional<PetChip> findByChipCode(String chipCode) {
        return petChipRepository.findByChipCode(chipCode);
    }

    /**
     * Tìm PetChip theo ID của Pet
     */
    public Optional<PetChip> findByPetId(Long petId) {
        return petChipRepository.findByPetId(petId);
    }

    /**
     * Kiểm tra PetChipCode đã tồn tại chưa
     */
    public boolean existsByChipCode(String chipCode) {
        return petChipRepository.existsByChipCode(chipCode);
    }

    /**
     * Cập nhật thông tin sức khỏe (sẽ dùng sau này)
     */
    @Transactional
    public PetChip updateHealthInfo(Long petChipId, String healthStatus, String medicalNotes) {
        PetChip petChip = petChipRepository.findById(petChipId)
                .orElseThrow(() -> new RuntimeException("PetChip not found"));

        if (healthStatus != null) {
            petChip.setCurrentHealthStatus(healthStatus);
        }
        if (medicalNotes != null) {
            petChip.setMedicalNotes(medicalNotes);
        }
        petChip.setLastHealthCheckDate(LocalDate.now());

        return petChipRepository.save(petChip);
    }
}