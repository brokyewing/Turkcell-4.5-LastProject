package com.turkcell.libraryapp.fineservice.controller;

import com.turkcell.libraryapp.fineservice.entity.Fine;
import com.turkcell.libraryapp.fineservice.security.IdentityGuard;
import com.turkcell.libraryapp.fineservice.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;
    private final IdentityGuard identityGuard;

    @GetMapping
    public ResponseEntity<List<Fine>> getAllFines(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Boolean isPaid) {
        if (studentId != null) {
            // Sahiplik ihlali 403 değil 404: başkasının cezası "yok" gibi görünür (varlık oracle'ı kapalı).
            if (!canAccessOwner(studentId)) {
                return ResponseEntity.notFound().build();
            }
        } else {
            identityGuard.requireLibrarian();
        }
        
        List<Fine> fines;
        if (studentId != null && isPaid != null) {
            fines = fineService.findFinesByStudentIdAndIsPaid(studentId, isPaid);
        } else if (studentId != null) {
            fines = fineService.findFinesByStudentId(studentId);
        } else if (isPaid != null && !isPaid) {
            fines = fineService.findUnpaidFines();
        } else {
            fines = fineService.getAllFines();
        }
        
        return ResponseEntity.ok(fines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Fine> getFineById(@PathVariable Long id) {
        Optional<Fine> fine = fineService.getFineById(id);
        // Kayıt yoksa da başkasınınsa da aynı 404 → varlık oracle'ı yok.
        if (fine.isEmpty() || !canAccessOwner(fine.get().getStudentId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fine.get());
    }

    @PostMapping
    public ResponseEntity<Fine> createFine(@RequestBody Fine fine) {
        identityGuard.requireLibrarian();
        Fine created = fineService.createFine(fine);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Fine> updateFine(@PathVariable Long id, @RequestBody Fine fine) {
        identityGuard.requireLibrarian();
        Fine updated = fineService.updateFine(id, fine);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<Fine> payFine(@PathVariable Long id) {
        identityGuard.requireLibrarian();
        Fine paid = fineService.payFine(id);
        return ResponseEntity.ok(paid);
    }

    @GetMapping("/students/{studentId}/total")
    public ResponseEntity<Double> getTotalUnpaidFines(@PathVariable Long studentId) {
        if (!canAccessOwner(studentId)) {
            return ResponseEntity.notFound().build();
        }
        Double total = fineService.getTotalUnpaidFinesByStudentId(studentId);
        return ResponseEntity.ok(total);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFine(@PathVariable Long id) {
        identityGuard.requireLibrarian();
        fineService.deleteFine(id);
        return ResponseEntity.ok().build();
    }

    /** İç çağrı ve LIBRARIAN her kaydı görür; STUDENT yalnız kendi kaydını. */
    private boolean canAccessOwner(Long ownerStudentId) {
        if (identityGuard.isInternalCall() || identityGuard.isLibrarian()) {
            return true;
        }
        Long current = identityGuard.currentStudentId();
        return current != null && current.equals(ownerStudentId);
    }
}







