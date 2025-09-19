package com.example.dataprocessor.service;

import com.example.dataprocessor.model.Student;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {
    
    @Value("${app.data.path}")
    private String dataPath;
    
    public String exportToCsv(List<Student> students) throws IOException {
        Path dataDir = Paths.get(dataPath);
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        
        String fileName = "students_export_" + System.currentTimeMillis() + ".csv";
        Path filePath = dataDir.resolve(fileName);
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            // Write header
            writer.writeNext(new String[]{"Student ID", "First Name", "Last Name", "Date of Birth", "Class", "Score"});
            
            // Write data
            for (Student student : students) {
                writer.writeNext(new String[]{
                    student.getStudentId().toString(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getDob() != null ? student.getDob().toString() : "",
                    student.getClazz(),
                    student.getScore() != null ? student.getScore().toString() : ""
                });
            }
        }
        
        return filePath.toString();
    }
    
    public String exportToExcel(List<Student> students) throws IOException {
        Path dataDir = Paths.get(dataPath);
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        
        String fileName = "students_export_" + System.currentTimeMillis() + ".xlsx";
        Path filePath = dataDir.resolve(fileName);
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Students");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Student ID");
            headerRow.createCell(1).setCellValue("First Name");
            headerRow.createCell(2).setCellValue("Last Name");
            headerRow.createCell(3).setCellValue("Date of Birth");
            headerRow.createCell(4).setCellValue("Class");
            headerRow.createCell(5).setCellValue("Score");
            
            // Write data
            int rowNum = 1;
            for (Student student : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(student.getStudentId());
                row.createCell(1).setCellValue(student.getFirstName());
                row.createCell(2).setCellValue(student.getLastName());
                row.createCell(3).setCellValue(student.getDob() != null ? student.getDob().toString() : "");
                row.createCell(4).setCellValue(student.getClazz());
                row.createCell(5).setCellValue(student.getScore() != null ? student.getScore() : 0);
            }
            
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }
        }
        
        return filePath.toString();
    }
    
    public String exportToPdf(List<Student> students) throws IOException {
        Path dataDir = Paths.get(dataPath);
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        
        String fileName = "students_export_" + System.currentTimeMillis() + ".pdf";
        Path filePath = dataDir.resolve(fileName);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Student Data Export");
                contentStream.endText();
                
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Student ID | First Name | Last Name | Date of Birth | Class | Score");
                contentStream.endText();
                
                float yPosition = 700;
                for (Student student : students) {
                    if (yPosition < 50) {
                        contentStream.close();
                        PDPage newPage = new PDPage();
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage);
                        yPosition = 750;
                    }
                    
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 8);
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText(String.format("%d | %s | %s | %s | %s | %d",
                            student.getStudentId(),
                            student.getFirstName(),
                            student.getLastName(),
                            student.getDob() != null ? student.getDob().toString() : "",
                            student.getClazz(),
                            student.getScore() != null ? student.getScore() : 0));
                    contentStream.endText();
                    
                    yPosition -= 15;
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            
            document.save(filePath.toFile());
        }
        
        return filePath.toString();
    }
}
