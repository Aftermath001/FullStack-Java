package com.example.dataprocessor.service;

import com.example.dataprocessor.model.Student;
import com.example.dataprocessor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataProcessingService {
    
    private final StudentRepository studentRepository;
    
    @Value("${app.data.path}")
    private String dataPath;
    
    public String generateExcelFile(int count) throws IOException {
        Path dataDir = Paths.get(dataPath);
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        
        String fileName = "students_" + System.currentTimeMillis() + ".xlsx";
        Path filePath = dataDir.resolve(fileName);
        
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
            
            // Generate random data
            Random random = new Random();
            String[] firstNames = {"John", "Jane", "Mike", "Sarah", "David", "Lisa", "Tom", "Emma"};
            String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"};
            String[] classes = {"Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5"};
            
            for (int i = 1; i <= count; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(i);
                row.createCell(1).setCellValue(firstNames[random.nextInt(firstNames.length)]);
                row.createCell(2).setCellValue(lastNames[random.nextInt(lastNames.length)]);
                row.createCell(3).setCellValue(LocalDate.now().minusYears(random.nextInt(10) + 5).toString());
                row.createCell(4).setCellValue(classes[random.nextInt(classes.length)]);
                row.createCell(5).setCellValue(random.nextInt(100) + 1);
            }
            
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }
        }
        
        return filePath.toString();
    }
    
    public String convertExcelToCsv(MultipartFile file) throws IOException {
        Path dataDir = Paths.get(dataPath);
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        
        String csvFileName = "converted_" + System.currentTimeMillis() + ".csv";
        Path csvPath = dataDir.resolve(csvFileName);
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            StringBuilder csvContent = new StringBuilder();
            
            for (Row row : sheet) {
                StringBuilder rowData = new StringBuilder();
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    if (i > 0) rowData.append(",");
                    String cellValue = "";
                    if (row.getCell(i) != null) {
                        cellValue = row.getCell(i).toString();
                    }
                    rowData.append("\"").append(cellValue).append("\"");
                }
                csvContent.append(rowData).append("\n");
            }
            
            Files.write(csvPath, csvContent.toString().getBytes());
        }
        
        return csvPath.toString();
    }
    
    public void importCsvToDatabase(MultipartFile file) throws IOException {
        String content = new String(file.getBytes());
        String[] lines = content.split("\n");
        
        for (int i = 1; i < lines.length; i++) { // Skip header
            String[] values = lines[i].split(",");
            if (values.length >= 6) {
                Student student = new Student();
                student.setStudentId(Long.parseLong(values[0].replace("\"", "")));
                student.setFirstName(values[1].replace("\"", ""));
                student.setLastName(values[2].replace("\"", ""));
                student.setDob(LocalDate.parse(values[3].replace("\"", "")));
                student.setClazz(values[4].replace("\"", ""));
                student.setScore(Integer.parseInt(values[5].replace("\"", "")));
                
                studentRepository.save(student);
            }
        }
    }
    
    public Page<Student> getStudents(Long studentId, String clazz, Pageable pageable) {
        return studentRepository.findByFilters(studentId, clazz, pageable);
    }
    
    public List<Student> getAllStudentsForExport(Long studentId, String clazz) {
        if (studentId != null && clazz != null) {
            return studentRepository.findByFilters(studentId, clazz, Pageable.unpaged()).getContent();
        } else if (studentId != null) {
            return studentRepository.findAll().stream()
                    .filter(s -> s.getStudentId().equals(studentId))
                    .toList();
        } else if (clazz != null) {
            return studentRepository.findAll().stream()
                    .filter(s -> clazz.equals(s.getClazz()))
                    .toList();
        } else {
            return studentRepository.findAll();
        }
    }
}
