package com.example.dataprocessor.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

@Service
public class CsvToDbService {

    private static final Logger logger = LoggerFactory.getLogger(CsvToDbService.class);
    
    @Autowired
    private DataSource dataSource;

    /**
     * Imports CSV file to database with score adjustment
     * @param csvFile MultipartFile containing the CSV data
     * @throws Exception if import fails
     */
    public void importCsvToDb(MultipartFile csvFile) throws Exception {
        logger.info("Starting CSV import to database: {}", csvFile.getOriginalFilename());
        
        Path tempCsvFile = null;
        Path tempImportFile = null;
        
        try {
            // Save uploaded CSV to temp file
            tempCsvFile = Files.createTempFile("uploaded_csv_", ".csv");
            csvFile.transferTo(tempCsvFile.toFile());
            logger.debug("Saved uploaded CSV to temp file: {}", tempCsvFile);
            
            // Create temp import CSV with adjusted scores
            tempImportFile = Files.createTempFile("import_csv_", ".csv");
            adjustScoresAndCreateImportFile(tempCsvFile, tempImportFile);
            logger.debug("Created temp import file with adjusted scores: {}", tempImportFile);
            
            // Import to database using PostgreSQL CopyManager
            importToDatabase(tempImportFile);
            
            logger.info("Successfully imported CSV to database");
            
        } finally {
            // Cleanup temp files
            cleanupTempFile(tempCsvFile);
            cleanupTempFile(tempImportFile);
        }
    }
    
    /**
     * Adjusts scores in CSV and creates import file
     * CSV score = (Excel score + 10), DB score = (Excel score + 5) ==> DB score = csvScore - 5
     * @param sourceCsv Source CSV file
     * @param targetCsv Target CSV file for import
     * @throws Exception if processing fails
     */
    private void adjustScoresAndCreateImportFile(Path sourceCsv, Path targetCsv) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(sourceCsv.toFile()));
             CSVWriter writer = new CSVWriter(new FileWriter(targetCsv.toFile()))) {
            
            String[] header = reader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            // Write header to import file
            writer.writeNext(header);
            
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 6) {
                    logger.warn("Skipping row with insufficient columns: {}", String.join(",", row));
                    continue;
                }
                
                // Adjust score: DB score = CSV score - 5
                try {
                    int csvScore = Integer.parseInt(row[5].trim());
                    int dbScore = csvScore - 5;
                    row[5] = String.valueOf(dbScore);
                    
                    logger.debug("Adjusted score: CSV={}, DB={}", csvScore, dbScore);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid score format in row, using 0: {}", row[5]);
                    row[5] = "-5"; // 0 - 5 = -5
                }
                
                writer.writeNext(row);
            }
            
            writer.flush();
        }
    }
    
    /**
     * Imports CSV file to database using PostgreSQL CopyManager
     * @param csvFile Path to CSV file for import
     * @throws Exception if import fails
     */
    private void importToDatabase(Path csvFile) throws Exception {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            BaseConnection baseConnection = connection.unwrap(BaseConnection.class);
            CopyManager copyManager = new CopyManager(baseConnection);
            
            try (InputStream inputStream = Files.newInputStream(csvFile)) {
                String copySql = "COPY students(studentid,firstname,lastname,dob,clazz,score) FROM STDIN WITH CSV HEADER";
                
                long rowsImported = copyManager.copyIn(copySql, inputStream);
                logger.info("Successfully imported {} rows to database", rowsImported);
            }
            
        } catch (SQLException e) {
            logger.error("Database import failed", e);
            throw new Exception("Failed to import CSV to database: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.warn("Failed to close database connection", e);
                }
            }
        }
    }
    
    /**
     * Cleans up temporary file
     * @param tempFile Path to temporary file to delete
     */
    private void cleanupTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
                logger.debug("Cleaned up temp file: {}", tempFile);
            } catch (Exception e) {
                logger.warn("Failed to delete temp file: {}", tempFile, e);
            }
        }
    }
}
