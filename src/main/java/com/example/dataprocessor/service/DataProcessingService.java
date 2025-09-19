package com.example.dataprocessor.service;

import com.example.dataprocessor.model.Student;
import com.example.dataprocessor.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataProcessingService {
    
    private final StudentRepository studentRepository;
    private final ExcelGeneratorService excelGeneratorService;
    private final ExcelToCsvService excelToCsvService;
    private final CsvToDbService csvToDbService;
    
    public String generateExcelFile(int count) throws IOException {
        log.info("Generating Excel file with {} records", count);
        Path filePath = excelGeneratorService.generateExcel(count);
        return filePath.toString();
    }
    
    public String convertExcelToCsv(MultipartFile file) throws Exception {
        log.info("Converting Excel file to CSV: {}", file.getOriginalFilename());
        Path csvPath = excelToCsvService.convertExcelToCsv(file);
        return csvPath.toString();
    }
    
    public void importCsvToDatabase(MultipartFile file) throws Exception {
        log.info("Importing CSV file to database: {}", file.getOriginalFilename());
        csvToDbService.importCsvToDb(file);
    }
    
    public Page<Student> getStudents(Long studentId, String clazz, String search, Pageable pageable) {
        return studentRepository.findByFilters(studentId, clazz, search, pageable);
    }
    
    public List<Student> getAllStudentsForExport(Long studentId, String clazz, String search) {
        if (studentId != null && clazz != null) {
            return studentRepository.findByFilters(studentId, clazz, search, Pageable.unpaged()).getContent();
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