package com.example.usermanagement.controller;

import com.example.usermanagement.service.WorkReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@RestController
@RequestMapping("/api/v1/work-reports")
public class WorkReportController {

    private static final Logger logger = LoggerFactory.getLogger(WorkReportController.class);

    @Autowired
    private WorkReportService workReportService;

    @GetMapping("/generate")
    public void generateXlsxReport(HttpServletResponse response) throws IOException {
        logger.info("Received request to generate XLSX report");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=work-report.xlsx");

        try (OutputStream out = response.getOutputStream()) {
            workReportService.generateXlsxReport(out);
        } catch (IOException e) {
            logger.error("Error while writing XLSX file", e);
            throw e;
        }

        logger.info("XLSX report generated successfully");
    }
}
