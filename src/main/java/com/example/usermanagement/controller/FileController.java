package com.example.usermanagement.controller;

import com.example.usermanagement.service.FileService;
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
import java.util.List;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            fileService.upload(file);
            return "Файл успешно загружен: " + file.getOriginalFilename();
        } catch (IOException e) {
            return "Ошибка при загрузке файла: " + e.getMessage();
        }
    }

    @GetMapping
    public List<String> listUploadedFiles() {
        return fileService.get();
    }

}

