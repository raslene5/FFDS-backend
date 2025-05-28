package com.backend.backend.controller;

import com.backend.backend.dto.FraudRequest;
import com.backend.backend.dto.FraudResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FraudDetectionController {

    private static final List<String> VALID_TYPES = Arrays.asList("CASH_OUT", "TRANSFER", "PAYMENT", "CASH_IN", "DEBIT");
    private static final List<String> VALID_PAIR_CODES = Arrays.asList("cc", "cm");
    private static final List<String> VALID_PARTS_OF_DAY = Arrays.asList("morning", "afternoon", "evening", "night");

    @PostMapping("/fraud-detect")
    public ResponseEntity<?> detectFraud(@RequestBody FraudRequest request) {
        // Validate input fields
        if (!VALID_TYPES.contains(request.getType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid transaction type: " + request.getType());
        }
        if (!VALID_PAIR_CODES.contains(request.getTransaction_pair_code())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid transaction_pair_code: " + request.getTransaction_pair_code());
        }
        if (!VALID_PARTS_OF_DAY.contains(request.getPart_of_the_day())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid part_of_the_day: " + request.getPart_of_the_day());
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String inputJson = mapper.writeValueAsString(request);

            // Escape double quotes for Windows cmd
            String escapedJson = inputJson.replace("\"", "\\\"");

            // Build the command
            String command = String.format("python predict.py \"%s\"", escapedJson);

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.directory(new File("C:\\Users\\Lenovo\\financialfrauddetectionsys-backend\\backend\\src\\main\\resources"));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Python script exited with code: " + exitCode);
            }

            // Convert output to FraudResponse
            FraudResponse fraudResponse = mapper.readValue(output.toString(), FraudResponse.class);

            return ResponseEntity.ok(fraudResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during fraud detection: " + e.getMessage());
        }
    }
}