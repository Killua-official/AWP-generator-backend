package com.example.usermanagement.controller;

import com.example.usermanagement.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @GetMapping("/download-report")
    public ResponseEntity<InputStreamResource> downloadAWPFile(@RequestParam String fileName, @RequestParam Double salary, HttpServletRequest request, @RequestParam String iin, @RequestParam String docNumber, @RequestParam String contractNumber) {
        Principal principal = request.getUserPrincipal();
        String username = principal.getName();
        try {
            var data = fileService.downloadReport(fileName, salary, username, iin, docNumber, contractNumber);
            ResponseEntity<InputStreamResource> response = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + UriEncoder.encode(data.getName()))
                    .contentLength(data.length())
                    .body(new InputStreamResource(new FileInputStream(data)));
            return response;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String fileName, HttpServletRequest request) throws IOException {
        Principal principal = request.getUserPrincipal();
        String username = principal.getName();
        var result = fileService.download(fileName, username);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + result.getName())
                .contentLength(result.getSize())
                .body(result.getResource());
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String username = principal.getName();
        try {
            fileService.upload(file, username);
            return "Файл успешно загружен: " + file.getOriginalFilename();
        } catch (IOException e) {
            return "Ошибка при загрузке файла: " + e.getMessage();
        }
    }

    @GetMapping
    public List<String> listUploadedFiles(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        String username = principal.getName();
        return fileService.get(username);
    }
}

