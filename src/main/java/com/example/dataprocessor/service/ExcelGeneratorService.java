package com.example.dataprocessor.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class ExcelGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelGeneratorService.class);
    
    private static final String[] CLASSES = {"Class1", "Class2", "Class3", "Class4", "Class5"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Value("${app.data.path:/tmp/data}")
    private String baseDataPath;

    /**
     * Generates an Excel file with student data
     * @param count Number of student records to generate
     * @return Absolute path to the generated Excel file
     * @throws IOException if file operations fail
     */
    public Path generateExcel(long count) throws IOException {
        logger.info("Generating Excel file with {} student records", count);
        
        // Ensure the data directory exists
        Path dataDir = Paths.get(baseDataPath);
        Files.createDirectories(dataDir);
        
        // Generate unique filename with timestamp
        String filename = String.format("students_%d_%d.xlsx", count, System.currentTimeMillis());
        Path filePath = dataDir.resolve(filename);
        
        Random random = new Random();
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Students");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("studentId");
            headerRow.createCell(1).setCellValue("firstName");
            headerRow.createCell(2).setCellValue("lastName");
            headerRow.createCell(3).setCellValue("DOB");
            headerRow.createCell(4).setCellValue("class");
            headerRow.createCell(5).setCellValue("score");
            
            // Generate data rows
            for (int i = 1; i <= count; i++) {
                Row row = sheet.createRow(i);
                
                // studentId = i
                row.createCell(0).setCellValue(i);
                
                // firstName, lastName = random alphabetic strings length 3..8
                row.createCell(1).setCellValue(randomAlpha(random, 3, 8));
                row.createCell(2).setCellValue(randomAlpha(random, 3, 8));
                
                // DOB = random date between 2000-01-01 and 2010-12-31
                row.createCell(3).setCellValue(generateRandomDate(random));
                
                // class = random from predefined classes
                row.createCell(4).setCellValue(CLASSES[random.nextInt(CLASSES.length)]);
                
                // score = random int between 55 and 75 inclusive
                row.createCell(5).setCellValue(random.nextInt(21) + 55);
            }
            
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }
            
            // Dispose of temporary files
            workbook.dispose();
            
            logger.info("Excel file generated successfully: {}", filePath.toAbsolutePath());
            return filePath.toAbsolutePath();
        }
    }
    
    /**
     * Generates a random alphabetic string of specified length
     * @param random Random instance
     * @param minLength Minimum length (inclusive)
     * @param maxLength Maximum length (inclusive)
     * @return Random alphabetic string
     */
    private String randomAlpha(Random random, int minLength, int maxLength) {
        int length = random.nextInt(maxLength - minLength + 1) + minLength;
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            char c = (char) (random.nextInt(26) + 'A');
            sb.append(c);
        }
        
        return sb.toString();
    }
    
    /**
     * Generates a random date between 2000-01-01 and 2010-12-31
     * @param random Random instance
     * @return Random date as string in yyyy-MM-dd format
     */
    private String generateRandomDate(Random random) {
        LocalDate startDate = LocalDate.of(2000, 1, 1);
        LocalDate endDate = LocalDate.of(2010, 12, 31);
        
        long daysBetween = endDate.toEpochDay() - startDate.toEpochDay();
        long randomDays = random.nextLong(daysBetween + 1);
        
        LocalDate randomDate = startDate.plusDays(randomDays);
        return randomDate.format(DATE_FORMATTER);
    }
}
