package com.htc.dgft.service;

import com.htc.dgft.dto.request.DgftOrmMasterRequest;
import com.htc.dgft.mapper.OrmMapper;
import com.htc.dgft.entity.DgftOrmMaster;
import com.htc.dgft.repository.DgftOrmMasterRepository;
import com.htc.dgft.repository.DgftPurposeCodeMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScreenUploadService {

    private final DgftOrmMasterRepository ormMasterRepository;
    private final DgftPurposeCodeMasterRepository purposeCodeMasterRepository;
    private final Validator validator;
    private final OrmMapper ormMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String processCsvUpload(MultipartFile file) {
        List<DgftOrmMaster> validRecords = new ArrayList<>();
        List<String> ackLogs = new ArrayList<>();
        int failedCount = 0;
        
        ackLogs.add("--- UPLOAD ACKNOWLEDGMENT ---");
        ackLogs.add("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ackLogs.add("Original File: " + file.getOriginalFilename());
        ackLogs.add("-----------------------------");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip header
                    lineNumber++;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }

                String[] columns = line.split("\\|");
                if (columns.length < 13) {
                    ackLogs.add("Line " + lineNumber + " - FAILED: Insufficient columns.");
                    failedCount++;
                    lineNumber++;
                    continue;
                }

                try {
                    DgftOrmMasterRequest dto = new DgftOrmMasterRequest();
                    dto.setOrmNumber(columns[0].trim());
                    dto.setOrmIssueDate(parseDate(columns[1].trim()));
                    dto.setIfscCode(columns[2].trim());
                    dto.setAdCode(columns[3].trim());
                    dto.setOrmDate(parseDate(columns[4].trim()));
                    dto.setOrmAmount(columns[5].trim().isEmpty() ? null : new BigDecimal(columns[5].trim()));
                    dto.setOrmCurrency(columns[6].trim());
                    dto.setInrPayableAmount(columns[7].trim().isEmpty() ? null : new BigDecimal(columns[7].trim()));
                    dto.setIeCode(columns[8].trim());
                    dto.setPanNumber(columns[9].trim());
                    dto.setBeneficiaryName(columns[10].trim());
                    dto.setBeneficiaryCountry(columns[11].trim());
                    
                    String purposeCode = columns[12].trim();
                    dto.setPurposeCode(purposeCode);
                    
                    if (columns.length >= 14) {
                        dto.setReferenceIrm(columns[13].trim());
                    }

                    // 1. Run JSR-380 validation
                    Set<ConstraintViolation<DgftOrmMasterRequest>> violations = validator.validate(dto);
                    if (!violations.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (ConstraintViolation<DgftOrmMasterRequest> violation : violations) {
                            sb.append(violation.getMessage()).append("; ");
                        }
                        ackLogs.add("Line " + lineNumber + " [" + dto.getOrmNumber() + "] - FAILED: " + sb.toString());
                        failedCount++;
                        lineNumber++;
                        continue;
                    }

                    // 2. Programmatic Date Comparison Check
                    if (dto.getOrmDate() != null && dto.getOrmIssueDate() != null && dto.getOrmDate().isAfter(dto.getOrmIssueDate())) {
                        ackLogs.add("Line " + lineNumber + " [" + dto.getOrmNumber() + "] - FAILED: ORM Date cannot be after ORM Issue Date; ");
                        failedCount++;
                        lineNumber++;
                        continue;
                    }

                    // 3. Database Check: Purpose Code must exist
                    if (purposeCodeMasterRepository.findByCode(purposeCode).isEmpty()) {
                        ackLogs.add("Line " + lineNumber + " [" + dto.getOrmNumber() + "] - FAILED: Purpose Code " + purposeCode + " is invalid.");
                        failedCount++;
                        lineNumber++;
                        continue;
                    }

                    // 4. Map to Entity
                    DgftOrmMaster orm = ormMapper.toEntity(dto);

                    // Default values for a fresh record
                    orm.setStatus("ACTIVE");
                    orm.setFlag("N"); // N implies workflow required
                    orm.setDgftFlag("F"); // F for Fresh
                    orm.setDgftStatus("Awaiting request initiated");

                    validRecords.add(orm);
                    ackLogs.add("Line " + lineNumber + " [" + orm.getOrmNumber() + "] - SUCCESS");
                } catch (Exception e) {
                    ackLogs.add("Line " + lineNumber + " - FAILED: Error parsing data - " + e.getMessage());
                    failedCount++;
                }

                lineNumber++;
            }

            ormMasterRepository.saveAll(validRecords);
            
            // Write ACK file
            String baseFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().replace(".csv", "") : "upload";
            String ackFilename = "ack_" + baseFilename + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
            Path ackDirPath = Paths.get("acks");
            if (!Files.exists(ackDirPath)) {
                Files.createDirectories(ackDirPath);
            }
            Path ackFilePath = ackDirPath.resolve(ackFilename);
            Files.write(ackFilePath, ackLogs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            if (validRecords.isEmpty()) {
                return "Upload failed. 0 valid records processed. Failed records: " + failedCount + ". ACK file generated at: " + ackFilePath.toAbsolutePath().toString();
            }

            return "Upload completed. Processed " + validRecords.size() + " valid records. Failed records: " + failedCount + ". ACK file generated at: " + ackFilePath.toAbsolutePath().toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}
