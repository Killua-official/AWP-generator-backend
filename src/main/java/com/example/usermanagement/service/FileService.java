package com.example.usermanagement.service;

import com.example.usermanagement.model.FileData;
import com.example.usermanagement.model.RedmineRow;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private static final String DIR = "D:" + File.separator + "temp" + File.separator;
    private static final String TEMPLATE_PATH = "G:\\documents\\margulan\\work\\dbt\\report\\2024\\Шаблон.xls";

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

    public File downloadReport(String fileName, Double salary) throws IOException {
        String csvFilePath = DIR + fileName;
        String xlsxFilePath = File.createTempFile(String.valueOf(UUID.randomUUID()), "-АWР.xls").getAbsolutePath();
        var existedFile = new File(xlsxFilePath);
        if (existedFile.exists()) {
            existedFile.delete();
        }

        List<RedmineRow> allData = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFilePath)).build();
             FileInputStream templateInputStream = new FileInputStream(TEMPLATE_PATH);
             Workbook workbook = new HSSFWorkbook(templateInputStream)) {

            var data = reader.readAll();
            data.remove(0);
            for (var line : data) {
                StringBuilder sb = new StringBuilder();
                var row = new RedmineRow();
                for (var item : line) {
                    sb.append(item);
                }
                var text = sb.toString();
                var cols = text.split(";");
                row.setComment(cols[12]);
                row.setDate(cols[1]);
                row.setCreationDate(cols[2].split(" ")[0]);
                try {
                    row.setCount(Double.valueOf(cols[13]));
                } catch (NumberFormatException e) {
                    // Handle the error as appropriate
                    row.setCount(0.0); // or any default value
                    System.err.println("Invalid number format for count: " + cols[13]);
                }
                row.setTaskID(getNumberAfterHash(cols[7]));
                allData.add(row);
            }
            allData.sort(Comparator.comparing(row -> {
                try {
                    return DateUtils.parseDate(row.getDate(), "dd.MM.yyyy");
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }));

            Sheet sheet = workbook.getSheetAt(0);
            int number = 1;
            int startRow = 19;
            Row templateRow = sheet.getRow(19);


            for (var rowData : allData) {
                Row excelRow = sheet.getRow(startRow);
                if (excelRow == null) {
                    excelRow = sheet.createRow(startRow);
                }

                if (startRow != 19) {
                    copyRow((HSSFWorkbook) workbook, (HSSFSheet) sheet, 19, startRow);
                }

                for (int i = 0; i < 50; i++) {
                    Cell templateCell = templateRow.getCell(i);
                    if (templateCell != null) {
                        Cell cell = excelRow.createCell(i);
                        cell.setCellStyle(templateCell.getCellStyle());
                        if (i == 0) { // Assuming column 0 is for the number
                            cell.setCellValue(number);
                            number++;
                        }
                        if (i == 15) { // Assuming column 1 is for the merged date
                            cell.setCellValue(rowData.getDate());
                        }
                        if (i == 2) { // Assuming column 2 is for the comment
                            cell.setCellValue(rowData.getComment()+ " (#" + rowData.getTaskID() + ")");
                        }
                        if (i == 29) {
                            cell.setCellValue("часы");
                        }
                        if (i == 32) {
                            cell.setCellValue(rowData.getCount()/100);
                        }
                        if (i == 37) {
                            cell.setCellValue(salary);
                        }
                        if (i == 43) {
                            //cell.setCellValue(salary * rowData.getCount()/100);
                            cell.setCellFormula("AG" + (20 + number - 2) + "*AL" + (20 + number - 2));
                        }
                    }
                }

                excelRow.setHeightInPoints(templateRow.getHeightInPoints());



                startRow += 1; // Move to the next pair of rows
            }
            Row excelRow = sheet.getRow(startRow);
            Cell cell = excelRow.getCell(32);
            var totalCount = allData.stream().map(RedmineRow::getCount).reduce(0.0, Double::sum)/100.0;
            cell.setCellValue(totalCount);

            cell = excelRow.getCell(37);
            cell.setCellValue(salary);

            cell = excelRow.getCell(43);
            cell.setCellValue(totalCount * salary);

            try (FileOutputStream outputStream = new FileOutputStream(xlsxFilePath)) {
                workbook.write(outputStream);
            }
        } catch (CsvException e) {
            e.printStackTrace();
        }

        return new File(xlsxFilePath);
    }

    private static void copyRow(HSSFWorkbook workbook, HSSFSheet worksheet, int sourceRowNum, int destinationRowNum) {
        // Get the source / new row
        HSSFRow newRow = worksheet.getRow(destinationRowNum);
        HSSFRow sourceRow = worksheet.getRow(sourceRowNum);

        // If the row exist in destination, push down all rows by 1 else create a new row
        if (newRow != null) {
            worksheet.shiftRows(destinationRowNum, worksheet.getLastRowNum(), 1);
        } else {
            newRow = worksheet.createRow(destinationRowNum);
        }

        // Loop through source columns to add to new row
        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            // Grab a copy of the old/new cell
            HSSFCell oldCell = sourceRow.getCell(i);
            HSSFCell newCell = newRow.createCell(i);

            // If the old cell is null jump to next cell
            if (oldCell == null) {
                newCell = null;
                continue;
            }


            HSSFCellStyle newCellStyle = workbook.createCellStyle();
            newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
            newCell.setCellStyle(newCellStyle);


            if (oldCell.getCellComment() != null) {
                newCell.setCellComment(oldCell.getCellComment());
            }


            if (oldCell.getHyperlink() != null) {
                newCell.setHyperlink(oldCell.getHyperlink());
            }


            switch (oldCell.getCellType()) {
                case BLANK:
                    newCell.setCellValue(oldCell.getStringCellValue());
                    break;
                case BOOLEAN:
                    newCell.setCellValue(oldCell.getBooleanCellValue());
                    break;
                case ERROR:
                    newCell.setCellErrorValue(oldCell.getErrorCellValue());
                    break;
                case FORMULA:
                    newCell.setCellFormula(oldCell.getCellFormula());
                    break;
                case NUMERIC:
                    newCell.setCellValue(oldCell.getNumericCellValue());
                    break;
                case STRING:
                    newCell.setCellValue(oldCell.getRichStringCellValue());
                    break;
            }
        }


        for (int i = 0; i < worksheet.getNumMergedRegions(); i++) {
            CellRangeAddress cellRangeAddress = worksheet.getMergedRegion(i);
            if (cellRangeAddress.getFirstRow() == sourceRow.getRowNum()) {
                CellRangeAddress newCellRangeAddress = new CellRangeAddress(newRow.getRowNum(),
                        newRow.getRowNum() + (cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow()),
                        cellRangeAddress.getFirstColumn(),
                        cellRangeAddress.getLastColumn());
                worksheet.addMergedRegion(newCellRangeAddress);
            }
        }
    }
    private static int getNumberAfterHash(String input) {
        int hashIndex = input.indexOf('#');
        if (hashIndex != -1) {
            StringBuilder numberString = new StringBuilder();
            for (int i = hashIndex + 1; i < input.length(); i++) {
                char c = input.charAt(i);
                if (Character.isDigit(c) || c == '.') {
                    numberString.append(c);
                } else {
                    break;
                }
            }
            if (numberString.length() > 0) {
                return Integer.parseInt(numberString.toString());
            }
        }
        return 0;
    }
}
