package com.example.dataprocessor.service;

import com.monitorjbl.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.Workbook;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class ExcelToCsvService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelToCsvService.class);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Value("${app.data.path:/tmp/data}")
    private String baseDataPath;

    /**
     * Converts an uploaded Excel file to CSV format with modified scores
     * @param file MultipartFile containing the Excel data
     * @return Path to the generated CSV file
     * @throws Exception if conversion fails
     */
    public Path convertExcelToCsv(MultipartFile file) throws Exception {
        logger.info("Converting Excel file to CSV: {}", file.getOriginalFilename());
        
        // Ensure the data directory exists
        Path dataDir = Paths.get(baseDataPath);
        Files.createDirectories(dataDir);
        
        // Create temporary file for the uploaded Excel
        Path tempFile = Files.createTempFile("uploaded_excel_", ".xlsx");
        try {
            // Save uploaded file to temp location
            file.transferTo(tempFile.toFile());
            logger.debug("Saved uploaded file to temp location: {}", tempFile);
            
            // Generate output CSV filename
            String csvFilename = String.format("converted_%d.csv", System.currentTimeMillis());
            Path csvPath = dataDir.resolve(csvFilename);
            
            // Process Excel file and convert to CSV
            try (InputStream inputStream = Files.newInputStream(tempFile);
                 CSVWriter csvWriter = new CSVWriter(new FileWriter(csvPath.toFile()))) {
                
                Workbook workbook = StreamingReader.builder()
                         .rowCacheSize(100)
                         .bufferSize(4096)
                         .open(inputStream);
                
                // Write CSV header
                csvWriter.writeNext(new String[]{"studentId", "firstName", "lastName", "DOB", "class", "score"});
                
                boolean isFirstRow = true;
                for (Row row : workbook.getSheetAt(0)) {
                    // Skip header row
                    if (isFirstRow) {
                        isFirstRow = false;
                        continue;
                    }
                    
                    // Extract data from Excel row
                    String[] csvRow = extractRowData(row);
                    if (csvRow != null) {
                        csvWriter.writeNext(csvRow);
                    }
                }
                
                csvWriter.flush();
                workbook.close();
                logger.info("Successfully converted Excel to CSV: {}", csvPath.toAbsolutePath());
                return csvPath.toAbsolutePath();
            }
            
        } finally {
            // Clean up temporary file
            try {
                Files.deleteIfExists(tempFile);
                logger.debug("Cleaned up temporary file: {}", tempFile);
            } catch (IOException e) {
                logger.warn("Failed to delete temporary file: {}", tempFile, e);
            }
        }
    }
    
    /**
     * Extracts data from an Excel row and converts it to CSV format
     * @param row Excel row to process
     * @return String array representing CSV row data, or null if row is invalid
     */
    private String[] extractRowData(Row row) {
        try {
            // Ensure we have at least 6 columns
            if (row.getLastCellNum() < 6) {
                logger.warn("Row {} has insufficient columns, skipping", row.getRowNum());
                return null;
            }
            
            String[] csvRow = new String[6];
            
            // studentId (column 0)
            csvRow[0] = getCellValueAsString(row.getCell(0));
            
            // firstName (column 1)
            csvRow[1] = getCellValueAsString(row.getCell(1));
            
            // lastName (column 2)
            csvRow[2] = getCellValueAsString(row.getCell(2));
            
            // DOB (column 3) - ensure ISO format
            csvRow[3] = formatDateToIso(getCellValueAsString(row.getCell(3)));
            
            // class (column 4)
            csvRow[4] = getCellValueAsString(row.getCell(4));
            
            // score (column 5) - add 10 to original score
            String originalScore = getCellValueAsString(row.getCell(5));
            csvRow[5] = calculateNewScore(originalScore);
            
            return csvRow;
            
        } catch (Exception e) {
            logger.warn("Error processing row {}, skipping: {}", row.getRowNum(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets cell value as string, handling different cell types
     * @param cell Excel cell
     * @return String representation of cell value
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Handle dates and numbers
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DATE_FORMATTER);
                } else {
                    // Format as integer if it's a whole number, otherwise as double
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (int) numericValue) {
                        return String.valueOf((int) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * Formats date string to ISO format (yyyy-MM-dd)
     * @param dateString Input date string
     * @return ISO formatted date string
     */
    private String formatDateToIso(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return "";
        }
        
        try {
            // Try to parse and reformat the date
            LocalDate date = LocalDate.parse(dateString.trim(), DATE_FORMATTER);
            return date.format(DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // If parsing fails, try other common formats
            try {
                LocalDate date = LocalDate.parse(dateString.trim());
                return date.format(DATE_FORMATTER);
            } catch (DateTimeParseException e2) {
                logger.warn("Could not parse date: {}, returning as-is", dateString);
                return dateString;
            }
        }
    }
    
    /**
     * Calculates new score by adding 10 to the original score
     * @param originalScore Original score as string
     * @return New score as string
     */
    private String calculateNewScore(String originalScore) {
        if (originalScore == null || originalScore.trim().isEmpty()) {
            return "0";
        }
        
        try {
            int score = Integer.parseInt(originalScore.trim());
            return String.valueOf(score + 10);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse score: {}, using 0", originalScore);
            return "10"; // 0 + 10
        }
    }
}
