package com.example.usermanagement.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.InputStreamResource;

import java.io.File;

@Builder
@Data
public class FileData {

    private InputStreamResource resource;
    private long size;
    private String name;
    private File file;

}
