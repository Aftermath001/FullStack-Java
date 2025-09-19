package com.example.dataprocessor.controller;

import com.example.dataprocessor.model.Student;
import com.example.dataprocessor.repository.StudentRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    
    private final StudentRepository studentRepository;
    
    /**
     * GET /api/students - Retrieve paginated students with optional filtering
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param studentId Optional student ID filter
     * @param clazz Optional class filter
     * @param search Optional search term for first/last name
     * @return Page<Student> as JSON
     */
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
        
        // Create specification for filtering
        Specification<Student> spec = createStudentSpecification(studentId, clazz, search);
        
        Page<Student> students = studentRepository.findAll(spec, pageable);
        
        return ResponseEntity.ok(students);
    }
    
    /**
     * GET /api/students/export - Export students in various formats
     * @param format Export format (csv, xlsx, pdf)
     * @param page Page number (default: 0)
     * @param size Page size (default: 100)
     * @param studentId Optional student ID filter
     * @param clazz Optional class filter
     * @param search Optional search term for first/last name
     * @return File download response
     */
    @GetMapping("/students/export")
    public ResponseEntity<byte[]> exportStudents(
            @RequestParam String format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String clazz,
            @RequestParam(required = false) String search) {
        
        try {
            log.info("Exporting students - format: {}, page: {}, size: {}, studentId: {}, clazz: {}, search: {}", 
                    format, page, size, studentId, clazz, search);
            
            // Create specification for filtering
            Specification<Student> spec = createStudentSpecification(studentId, clazz, search);
            
            // Get students based on pagination
            Pageable pageable = PageRequest.of(page, size);
            Page<Student> studentPage = studentRepository.findAll(spec, pageable);
            List<Student> students = studentPage.getContent();
            
            byte[] fileContent;
            String fileName;
            String contentType;
            
            switch (format.toLowerCase()) {
                case "csv":
                    fileContent = exportToCsv(students);
                    fileName = "students_export_" + System.currentTimeMillis() + ".csv";
                    contentType = "text/csv";
                    break;
                case "xlsx":
                    fileContent = exportToXlsx(students);
                    fileName = "students_export_" + System.currentTimeMillis() + ".xlsx";
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    break;
                case "pdf":
                    fileContent = exportToPdf(students);
                    fileName = "students_export_" + System.currentTimeMillis() + ".pdf";
                    contentType = "application/pdf";
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(fileContent.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
                    
        } catch (Exception e) {
            log.error("Error exporting students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Creates JPA Specification for filtering students
     */
    private Specification<Student> createStudentSpecification(Long studentId, String clazz, String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("studentId"), studentId));
            }
            
            if (clazz != null && !clazz.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("clazz"), clazz));
            }
            
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")), searchPattern);
                Predicate lastNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")), searchPattern);
                predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Export students to CSV format using OpenCSV
     */
    private byte[] exportToCsv(List<Student> students) throws IOException {
        StringWriter stringWriter = new StringWriter();
        
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            // Write header
            csvWriter.writeNext(new String[]{"Student ID", "First Name", "Last Name", "Date of Birth", "Class", "Score"});
            
            // Write data rows
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Student student : students) {
                csvWriter.writeNext(new String[]{
                    student.getStudentId().toString(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getDob() != null ? student.getDob().format(dateFormatter) : "",
                    student.getClazz(),
                    student.getScore() != null ? student.getScore().toString() : ""
                });
            }
        }
        
        return stringWriter.toString().getBytes("UTF-8");
    }
    
    /**
     * Export students to XLSX format using Apache POI SXSSFWorkbook
     */
    private byte[] exportToXlsx(List<Student> students) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Students");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Student ID");
            headerRow.createCell(1).setCellValue("First Name");
            headerRow.createCell(2).setCellValue("Last Name");
            headerRow.createCell(3).setCellValue("Date of Birth");
            headerRow.createCell(4).setCellValue("Class");
            headerRow.createCell(5).setCellValue("Score");
            
            // Write data rows
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            int rowNum = 1;
            for (Student student : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(student.getStudentId());
                row.createCell(1).setCellValue(student.getFirstName());
                row.createCell(2).setCellValue(student.getLastName());
                row.createCell(3).setCellValue(student.getDob() != null ? 
                    student.getDob().format(dateFormatter) : "");
                row.createCell(4).setCellValue(student.getClazz());
                row.createCell(5).setCellValue(student.getScore() != null ? student.getScore() : 0);
            }
            
            // Auto-size columns
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.dispose();
            
            return outputStream.toByteArray();
        }
    }
    
    /**
     * Export students to PDF format using Apache PDFBox
     */
    private byte[] exportToPdf(List<Student> students) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                // Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Student Data Export");
                contentStream.endText();
                
                // Header row
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Student ID | First Name | Last Name | Date of Birth | Class | Score");
                contentStream.endText();
                
                // Data rows
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                float yPosition = 700;
                int recordsPerPage = 30;
                int currentRecord = 0;
                
                for (Student student : students) {
                    if (currentRecord > 0 && currentRecord % recordsPerPage == 0) {
                        contentStream.close();
                        PDPage newPage = new PDPage();
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage);
                        yPosition = 750;
                    }
                    
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 8);
                    contentStream.newLineAtOffset(50, yPosition);
                    
                    String dobString = student.getDob() != null ? 
                        student.getDob().format(dateFormatter) : "";
                    String scoreString = student.getScore() != null ? 
                        student.getScore().toString() : "0";
                    
                    contentStream.showText(String.format("%d | %s | %s | %s | %s | %s",
                            student.getStudentId(),
                            student.getFirstName(),
                            student.getLastName(),
                            dobString,
                            student.getClazz(),
                            scoreString));
                    contentStream.endText();
                    
                    yPosition -= 15;
                    currentRecord++;
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
