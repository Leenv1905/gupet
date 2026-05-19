package com.ecom.gupet.modules.pet.controller;

import com.ecom.gupet.common.util.SecurityUtils;
import com.ecom.gupet.modules.pet.dto.PetRequest;
import com.ecom.gupet.modules.pet.dto.PetResponse;
import com.ecom.gupet.modules.pet.service.PetService;
import com.ecom.gupet.modules.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
@Tag(name = "Pet Management", description = "API quản lý thú cưng")
public class PetController {

    private final PetService petService;
    private final UserRepository userRepository;

    // Helper lấy sellerId từ email trong JWT
    private Long getCurrentSellerId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) return null;
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElse(null);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tạo thú cưng mới với upload ảnh (Chỉ SHOP)")
    public ResponseEntity<PetResponse> createPet(
            @RequestPart("data") PetRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {

//        Long sellerId = (Long) SecurityContextHolder.getContext()
//                .getAuthentication().getDetails();
//
//        if (sellerId == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        PetResponse response = petService.createPet(request, sellerId, images);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
        Long sellerId = getCurrentSellerId();
        if (sellerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        PetResponse response = petService.createPet(request, sellerId, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== CÁC API CŨ ====================
    @GetMapping("/available")
    public ResponseEntity<List<PetResponse>> getAvailablePets() {
        return ResponseEntity.ok(petService.getAllAvailablePets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetResponse> getPetById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @GetMapping("/my-pets")
    @Operation(summary = "Lấy danh sách thú cưng của shop hiện tại")
//    @Transactional(readOnly = true)
    public ResponseEntity<List<PetResponse>> getMyPets() {
        Long sellerId = getCurrentSellerId();
        if (sellerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(petService.getPetsBySeller(sellerId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Shop cập nhật thông tin thú cưng của mình")
//    @Transactional(readOnly = true)
    public ResponseEntity<PetResponse> updatePet(
            @PathVariable Long id,
            @RequestPart("data") PetRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {

        Long sellerId = getCurrentSellerId();
        if (sellerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        PetResponse response = petService.updatePet(id, request, sellerId, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        Long sellerId = getCurrentSellerId();
        if (sellerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        petService.deletePet(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}

//package com.ecom.gupet.modules.pet.controller;
//
//import com.ecom.gupet.modules.pet.dto.PetRequest;
//import com.ecom.gupet.modules.pet.dto.PetResponse;
//import com.ecom.gupet.modules.pet.service.PetService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/pet")
//@RequiredArgsConstructor
//@Tag(name = "Pet Management", description = "API quản lý thú cưng")
//public class PetController {
//
//    private final PetService petService;
//
//    @PostMapping
//    @Operation(summary = "Tạo thú cưng mới (Chỉ SHOP)")
//    public ResponseEntity<PetResponse> createPet(
//            @Valid @RequestBody PetRequest request,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        // Lấy sellerId từ JWT (đã cải tiến)
//        Long sellerId = (Long) SecurityContextHolder.getContext()
//                .getAuthentication().getDetails();
//
//        if (sellerId == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(null); // Hoặc throw exception
//        }
//
//        PetResponse response = petService.createPet(request, sellerId);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    // ==================== READ ====================
//    @GetMapping("/available")
//    @Operation(summary = "Lấy danh sách thú cưng đang bán")
//    public ResponseEntity<List<PetResponse>> getAvailablePets() {
//        List<PetResponse> pets = petService.getAllAvailablePets();
//        return ResponseEntity.ok(pets);
//    }
//
//    @GetMapping("/{id}")
//    @Operation(summary = "Lấy chi tiết thú cưng")
//    public ResponseEntity<PetResponse> getPetById(@PathVariable Long id) {
//        PetResponse pet = petService.getPetById(id);
//        return ResponseEntity.ok(pet);
//    }
//
//    @GetMapping("/my-pets")
//    @Operation(summary = "Lấy danh sách thú cưng của tôi")
//    public ResponseEntity<List<PetResponse>> getMyPets(
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        Long sellerId = (Long) SecurityContextHolder.getContext()
//                .getAuthentication().getDetails();
//
//        List<PetResponse> pets = petService.getPetsBySeller(sellerId);
//        return ResponseEntity.ok(pets);
//    }
//
//    @DeleteMapping("/{id}")
//    @Operation(summary = "Xóa thú cưng của tôi")
//    public ResponseEntity<Void> deletePet(
//            @PathVariable Long id,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        Long sellerId = (Long) SecurityContextHolder.getContext()
//                .getAuthentication().getDetails();
//
//        petService.deletePet(id, sellerId);
//        return ResponseEntity.noContent().build();
//    }
//}