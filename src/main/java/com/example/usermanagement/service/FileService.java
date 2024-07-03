package com.example.usermanagement.service;

import com.example.usermanagement.model.FileData;
import com.example.usermanagement.model.RedmineRow;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public File downloadReport(String fileName) throws IOException {
        String csvFilePath = DIR + fileName;
        String xlsxFilePath = File.createTempFile(String.valueOf(UUID.randomUUID()), "-АWР.xlsx").getAbsolutePath();
        var existedFile = new File(xlsxFilePath);
        if (existedFile.exists()) {
            existedFile.delete();
        }

        List<RedmineRow> allData = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFilePath)).build();
             Workbook workbook = new XSSFWorkbook()) {

            var data = reader.readAll();
            for (var line : data) {
                StringBuilder sb = new StringBuilder();
                var row = new RedmineRow();
                for (var item : line) {
                    sb.append(item);
                }
                var text = sb.toString();
                var cols = text.split(";");
                row.setComment(cols[12]);

                allData.add(row);
            }
            Sheet sheet = workbook.createSheet("AWP");
            int rowCount = 0;

            for (var row : allData) {
                Row excelRow = sheet.createRow(rowCount++);
                excelRow.createCell(0).setCellValue(row.getComment());
            }
            try (FileOutputStream outputStream = new FileOutputStream(xlsxFilePath)) {
                workbook.write(outputStream);
            }
        } catch (CsvException e) {
            e.printStackTrace();
        }

        return new File(xlsxFilePath);
    }

}
