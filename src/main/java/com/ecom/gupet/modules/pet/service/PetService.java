package com.ecom.gupet.modules.pet.service;

import com.ecom.gupet.modules.pet.dto.PetRequest;
import com.ecom.gupet.modules.pet.dto.PetResponse;
import com.ecom.gupet.modules.pet.entity.Pet;
import com.ecom.gupet.modules.pet.entity.PetChip;
import com.ecom.gupet.modules.pet.entity.PetImage;
import com.ecom.gupet.modules.pet.mapper.PetMapper;
import com.ecom.gupet.modules.pet.repository.PetRepository;
import com.ecom.gupet.common.service.FileStorageService;
import com.ecom.gupet.modules.user.entity.User;
import com.ecom.gupet.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetMapper petMapper;
    private final FileStorageService fileStorageService;
    private final PetChipService petChipService;
//    private Long getSellerIdByEmail(String email) {
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found"))
//                .getId();
//    }

    @Transactional
    public PetResponse createPet(PetRequest request, Long sellerId, MultipartFile[] images) {

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        // Kiểm tra role SHOP
        boolean isShop = seller.getRoles().stream()
                .anyMatch(r -> "ROLE_SHOP".equals(r.getName()));
        if (!isShop) {
            throw new RuntimeException("Chỉ tài khoản SHOP mới được đăng bán thú cưng");
        }

        // Upload ảnh nếu có
        List<String> imageUrls = fileStorageService.uploadMultiple(images);
        request.setImageUrls(imageUrls);

        // Tạo Pet
        Pet pet = petMapper.toEntity(request);
        pet.setSeller(seller);

        Pet savedPet = petRepository.save(pet);

        // Tạo PetChip
        petChipService.createPetChip(savedPet, request.getPetChipCode());

        return petMapper.toResponse(savedPet);
    }

    // ==================== CÁC METHOD CŨ GIỮ NGUYÊN ====================
    public List<PetResponse> getAllAvailablePets() {
        return petMapper.toResponseList(petRepository.findAvailablePets());
    }

    public PetResponse getPetById(Long id) {
        Pet pet = petRepository.findByIdWithSellerAndImages(id)
                .orElseThrow(() -> new RuntimeException("Pet not found"));
        return petMapper.toResponse(pet);
    }

    public List<PetResponse> getPetsBySeller(Long sellerId) {
        List<Pet> pets = petRepository.findBySellerId(sellerId);
        return petMapper.toResponseList(pets);
    }
// Sửa thông tin thú cưng (chỉ SHOP sở hữu mới được phép)
@Transactional
public PetResponse updatePet(Long id, PetRequest request, Long sellerId, MultipartFile[] images) {
    System.out.println("=== UPDATE PET CALLED ===");
    System.out.println("Pet ID: " + id + " | Seller ID: " + sellerId);
    System.out.println("Request Name: " + request.getName());
    System.out.println("Request Price: " + request.getPrice());

    Pet pet = petRepository.findByIdWithSellerAndImages(id)
            .orElseThrow(() -> new RuntimeException("Pet not found"));

    // Kiểm tra quyền sở hữu
    if (!pet.getSeller().getId().equals(sellerId)) {
        throw new RuntimeException("Bạn chỉ được sửa pet của mình");
    }

    // === MANUAL UPDATE (bỏ mapper để debug dễ) ===
    pet.setName(request.getName());
    pet.setDescription(request.getDescription());
    pet.setPrice(request.getPrice());
    pet.setStatus(request.getStatus() != null ? request.getStatus() : pet.getStatus());
    pet.setSpecies(request.getSpecies());
    pet.setBreed(request.getBreed());
    pet.setColor(request.getColor());
    pet.setGender(request.getGender());
    pet.setWeight(request.getWeight());
    pet.setBirthDate(request.getBirthDate());

    // Không cho thay đổi PetChipCode
    if (!pet.getPetChipCode().equals(request.getPetChipCode())) {
        System.out.println("⚠️ PetChipCode bị thay đổi → giữ nguyên cũ");
    }

    // Xử lý ảnh mới (nếu có)
    if (images != null && images.length > 0) {
        List<String> newImageUrls = fileStorageService.uploadMultiple(images);
        // Xóa ảnh cũ nếu muốn (tùy bạn)
        // pet.getImages().clear();
        for (int i = 0; i < newImageUrls.size(); i++) {
            PetImage img = PetImage.builder()
                    .pet(pet)
                    .imageUrl(newImageUrls.get(i))
                    .displayOrder(i)
                    .build();
            pet.addImage(img);
        }
    }

    Pet savedPet = petRepository.save(pet);
    System.out.println("✅ Pet updated successfully. New name: " + savedPet.getName());

    return petMapper.toResponse(savedPet);
}

    @Transactional
    public void deletePet(Long id, Long sellerId) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pet not found"));

        if (!pet.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Bạn chỉ được xóa thú cưng của mình");
        }

        petRepository.delete(pet);
    }
}

//package com.ecom.gupet.modules.pet.service;
//
//import com.ecom.gupet.modules.pet.dto.PetRequest;
//import com.ecom.gupet.modules.pet.dto.PetResponse;
//import com.ecom.gupet.modules.pet.entity.Pet;
//import com.ecom.gupet.modules.pet.entity.PetChip;
//import com.ecom.gupet.modules.pet.entity.PetImage;
//import com.ecom.gupet.modules.pet.mapper.PetMapper;
//import com.ecom.gupet.modules.pet.repository.PetRepository;
//import com.ecom.gupet.modules.user.entity.User;
//import com.ecom.gupet.modules.user.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Service quản lý Pet (Thú cưng)
// */
//@Service
//@RequiredArgsConstructor
//public class PetService {
//
//    private final PetRepository petRepository;
//    private final UserRepository userRepository;
//    private final PetMapper petMapper;
//    private final PetChipService petChipService;   // ← Đã inject
//
//    /**
//     * Đăng bán thú cưng mới (Chỉ SHOP được phép)
//     */
//    @Transactional
//    public PetResponse createPet(PetRequest request, Long sellerId) {
//        // Tìm người bán
//        User seller = userRepository.findById(sellerId)
//                .orElseThrow(() -> new RuntimeException("Seller not found"));
//
//        // Kiểm tra quyền SHOP
//        boolean isShop = seller.getRoles().stream()
//                .anyMatch(role -> "ROLE_SHOP".equals(role.getName()));
//
//        if (!isShop) {
//            throw new RuntimeException("Chỉ tài khoản SHOP mới được đăng bán thú cưng");
//        }
//
//        // Kiểm tra PetChipCode đã tồn tại chưa
//        if (petChipService.existsByChipCode(request.getPetChipCode())) {
//            throw new RuntimeException("Pet Chip Code đã tồn tại: " + request.getPetChipCode());
//        }
//
//        // Convert DTO → Entity
//        Pet pet = petMapper.toEntity(request);
//        pet.setSeller(seller);
//        pet.setPetChipCode(request.getPetChipCode());   // Set thủ công vì là unique & immutable
//
//        // Xử lý ảnh
//        if (pet.getImages() == null) {
//            pet.setImages(new ArrayList<>());
//        }
//
//        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
//            for (int i = 0; i < request.getImageUrls().size(); i++) {
//                PetImage image = PetImage.builder()
//                        .imageUrl(request.getImageUrls().get(i))
//                        .displayOrder(i)
//                        .build();
//                pet.addImage(image);
//            }
//        }
//
//        // Lưu Pet
//        Pet savedPet = petRepository.save(pet);
//
//        // Tạo PetChip record (quản lý y tế sau này)
//        petChipService.createPetChip(savedPet, request.getPetChipCode());
//
//        return petMapper.toResponse(savedPet);
//    }
//
//    // ==================== READ ====================
//
//    public List<PetResponse> getAllAvailablePets() {
//        List<Pet> pets = petRepository.findAvailablePets();
//        return petMapper.toResponseList(pets);
//    }
//
//    public List<PetResponse> getPetsBySeller(Long sellerId) {
//        List<Pet> pets = petRepository.findBySellerId(sellerId);
//        return petMapper.toResponseList(pets);
//    }
//
//    public PetResponse getPetById(Long id) {
//        Pet pet = petRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Pet not found"));
//        return petMapper.toResponse(pet);
//    }
//
//    // ==================== DELETE ====================
//
//    @Transactional
//    public void deletePet(Long id, Long sellerId) {
//        Pet pet = petRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Pet not found"));
//
//        if (!pet.getSeller().getId().equals(sellerId)) {
//            throw new RuntimeException("You can only delete your own pet");
//        }
//
//        petRepository.delete(pet);
//    }
//}