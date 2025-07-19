package com.ainovel.app.material;

import com.ainovel.app.material.dto.*;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/materials")
public class MaterialController {
    @Autowired
    private MaterialService materialService;
    @Autowired
    private UserRepository userRepository;

    private User currentUser(UserDetails details) {
        return userRepository.findByUsername(details.getUsername()).orElseThrow();
    }

    @PostMapping
    public MaterialDto create(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody MaterialCreateRequest request) {
        return materialService.create(currentUser(principal), request);
    }

    @GetMapping
    public List<MaterialDto> list(@AuthenticationPrincipal UserDetails principal) { return materialService.list(currentUser(principal)); }

    @GetMapping("/{id}")
    public MaterialDto get(@PathVariable UUID id) { return materialService.get(id); }

    @PutMapping("/{id}")
    public MaterialDto update(@PathVariable UUID id, @RequestBody MaterialUpdateRequest request) { return materialService.update(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { materialService.delete(id); return ResponseEntity.noContent().build(); }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileImportJobDto upload(@AuthenticationPrincipal UserDetails principal, @RequestPart("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return materialService.createUploadJob(currentUser(principal), file.getOriginalFilename(), content);
    }

    @GetMapping("/upload/{jobId}")
    public FileImportJobDto uploadStatus(@PathVariable UUID jobId) { return materialService.getUploadStatus(jobId); }

    @PostMapping("/search")
    public List<MaterialSearchResultDto> search(@RequestBody MaterialSearchRequest request) { return materialService.search(request); }

    @PostMapping("/editor/auto-hints")
    public List<MaterialSearchResultDto> hints(@RequestBody AutoHintRequest request) { return materialService.autoHints(request); }

    @PostMapping("/review/pending")
    public List<MaterialDto> pending() { return materialService.pending(); }

    @PostMapping("/{id}/review/approve")
    public MaterialDto approve(@PathVariable UUID id, @RequestBody MaterialReviewRequest request) { return materialService.review(id, "approve", request); }

    @PostMapping("/{id}/review/reject")
    public MaterialDto reject(@PathVariable UUID id, @RequestBody MaterialReviewRequest request) { return materialService.review(id, "reject", request); }

    @PostMapping("/find-duplicates")
    public List<Map<String, Object>> duplicates() { return materialService.findDuplicates(); }

    @PostMapping("/merge")
    public MaterialDto merge(@RequestBody MaterialMergeRequest request) { return materialService.merge(request); }

    @GetMapping("/{id}/citations")
    public List<Map<String, Object>> citations(@PathVariable UUID id) { return materialService.citations(id); }
}
