package com.example.ainovel.service.material;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.ainovel.dto.material.FileImportJobResponse;
import com.example.ainovel.model.material.FileImportJob;
import com.example.ainovel.model.material.FileImportStatus;
import com.example.ainovel.repository.material.FileImportJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件上传导入服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileImportService {

    private final MaterialService materialService;
    private final FileImportJobRepository fileImportJobRepository;

    @Value("${app.material.upload-dir:./data/materials}")
    private String uploadDir;

    @Transactional
    public FileImportJobResponse startFileImportJob(Long workspaceId, Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        Path storagePath = initStorageDirectory();
        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "material.txt";
        String storedFilename = System.currentTimeMillis() + "-" + sanitizeFilename(originalFilename);
        Path target = storagePath.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        FileImportJob job = new FileImportJob();
        job.setWorkspaceId(workspaceId);
        job.setUploaderId(userId);
        job.setFileName(originalFilename);
        job.setFilePath(target.toAbsolutePath().toString());
        job.setStatus(FileImportStatus.PROCESSING);
        job = fileImportJobRepository.save(job);

        try {
            String extractedText = extractText(target);
            materialService.createMaterialFromImport(
                deriveTitle(originalFilename),
                buildSummary(extractedText),
                extractedText,
                workspaceId,
                userId,
                job.getId(),
                null
            );
            job.setStatus(FileImportStatus.COMPLETED);
            job.setFinishedAt(LocalDateTime.now());
        } catch (Exception ex) {
            job.setStatus(FileImportStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            log.error("文件导入失败 (jobId={}): {}", job.getId(), ex.getMessage(), ex);
        }

        fileImportJobRepository.save(job);
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public FileImportJobResponse getJobStatus(Long jobId, Long workspaceId) {
        FileImportJob job = fileImportJobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
            .orElseThrow(() -> new IllegalArgumentException("未找到对应的上传任务"));
        return toResponse(job);
    }

    private Path initStorageDirectory() throws IOException {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(path);
        return path;
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String deriveTitle(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "导入素材";
        }
        String name = filename;
        int idx = filename.lastIndexOf('.');
        if (idx > 0) {
            name = filename.substring(0, idx);
        }
        return name.trim();
    }

    private String extractText(Path filePath) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            parser.parse(inputStream, handler, metadata);
        }
        return handler.toString();
    }

    private String buildSummary(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200) + "...";
    }

    private FileImportJobResponse toResponse(FileImportJob job) {
        FileImportJobResponse response = new FileImportJobResponse();
        response.setId(job.getId());
        response.setWorkspaceId(job.getWorkspaceId());
        response.setUploaderId(job.getUploaderId());
        response.setFileName(job.getFileName());
        response.setStatus(job.getStatus());
        response.setErrorMessage(job.getErrorMessage());
        response.setCreatedAt(job.getCreatedAt());
        response.setFinishedAt(job.getFinishedAt());
        return response;
    }
}
