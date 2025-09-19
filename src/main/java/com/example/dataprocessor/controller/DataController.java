package com.example.dataprocessor.controller;

import com.example.dataprocessor.model.Student;
import com.example.dataprocessor.service.DataProcessingService;
import com.example.dataprocessor.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DataController {
    
    private final DataProcessingService dataProcessingService;
    private final ExportService exportService;
    
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateExcelFile(@RequestParam int count) {
        try {
            log.info("Generating Excel file with {} records", count);
            String filePath = dataProcessingService.generateExcelFile(count);
            String fileName = Paths.get(filePath).getFileName().toString();
            
            Map<String, String> response = new HashMap<>();
            response.put("filePath", filePath);
            response.put("downloadLink", "/api/download/" + fileName);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error generating Excel file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/convert")
    public ResponseEntity<Map<String, String>> convertExcelToCsv(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Converting Excel file to CSV: {}", file.getOriginalFilename());
            String csvPath = dataProcessingService.convertExcelToCsv(file);
            String fileName = Paths.get(csvPath).getFileName().toString();
            
            Map<String, String> response = new HashMap<>();
            response.put("csvPath", csvPath);
            response.put("downloadLink", "/api/download/" + fileName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error converting Excel to CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/upload-csv")
    public ResponseEntity<Map<String, String>> uploadCsv(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading CSV file: {}", file.getOriginalFilename());
            dataProcessingService.importCsvToDatabase(file);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "CSV file uploaded and imported successfully");
            response.put("recordsProcessed", "Data imported to database");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading CSV file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/students")
    public ResponseEntity<Page<Student>> getStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String clazz,
            @RequestParam(required = false) String search) {
        
        log.info("Fetching students - page: {}, size: {}, studentId: {}, clazz: {}, search: {}", 
                page, size, studentId, clazz, search);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Student> students = dataProcessingService.getStudents(studentId, clazz, search, pageable);
        
        return ResponseEntity.ok(students);
    }
    
    @GetMapping("/students/export")
    public ResponseEntity<Map<String, String>> exportStudents(
            @RequestParam String format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String clazz,
            @RequestParam(required = false) String search) {
        
        try {
            log.info("Exporting students - format: {}, page: {}, size: {}, studentId: {}, clazz: {}, search: {}", 
                    format, page, size, studentId, clazz, search);
            
            List<Student> students = dataProcessingService.getAllStudentsForExport(studentId, clazz, search);
            
            String filePath;
            switch (format.toLowerCase()) {
                case "csv":
                    filePath = exportService.exportToCsv(students);
                    break;
                case "xlsx":
                    filePath = exportService.exportToExcel(students);
                    break;
                case "pdf":
                    filePath = exportService.exportToPdf(students);
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            String fileName = Paths.get(filePath).getFileName().toString();
            
            Map<String, String> response = new HashMap<>();
            response.put("filePath", filePath);
            response.put("downloadLink", "/api/download/" + fileName);
            response.put("format", format);
            response.put("recordCount", String.valueOf(students.size()));
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error exporting students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        try {
            // Use the configured data path instead of temp directory
            Path filePath = Paths.get(System.getProperty("user.home"), "var", "log", "applications", "API", "dataprocessing", fileName);
            if (!filePath.toFile().exists()) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);
            
            String contentType = "application/octet-stream";
            if (fileName.endsWith(".csv")) {
                contentType = "text/csv";
            } else if (fileName.endsWith(".xlsx")) {
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else if (fileName.endsWith(".pdf")) {
                contentType = "application/pdf";
            }
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", contentType)
                    .body(fileContent);
        } catch (IOException e) {
            log.error("Error downloading file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
