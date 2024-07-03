package com.example.usermanagement.service;

import com.example.usermanagement.model.FileData;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final String DIR = "D:" + File.separator + "temp" + File.separator;

    public List<String> get() {
        File folder = new File(DIR);
        File[] listOfFiles = folder.listFiles();
        List<String> fileNames = new ArrayList<>();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames;
    }

    public void upload(MultipartFile file) throws IOException {
        String uploadDir = DIR;
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        file.transferTo(new File(uploadDir + File.separator + file.getOriginalFilename()));
    }

    public FileData download(String fileName) throws FileNotFoundException {
        File file = new File(DIR + fileName);
        return FileData.builder()
                .resource(new InputStreamResource(new FileInputStream(file)))
                .size(file.length())
                .name(file.getName())
                .build();
    }
}
