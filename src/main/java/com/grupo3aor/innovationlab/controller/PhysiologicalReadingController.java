package com.grupo3aor.innovationlab.controller;

import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import com.grupo3aor.innovationlab.service.PhysiologicalReadingService;
import com.grupo3aor.innovationlab.service.BioGearsParserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.io.InputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MappingIterator;

/**
 * REST Controller responsible for handling the ingestion and retrieval of physiological readings.
 * Provides endpoints for single inserts, continuous streaming, and batch uploads from BioGears.
 */
@RestController
@RequestMapping("/api/readings")
@PreAuthorize("isAuthenticated()")
public class PhysiologicalReadingController {

    private final PhysiologicalReadingService service;
    private final BioGearsParserService parserService;
    private final ObjectMapper objectMapper;

    public PhysiologicalReadingController(PhysiologicalReadingService service, BioGearsParserService parserService, ObjectMapper objectMapper) {
        this.service = service;
        this.parserService = parserService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<PhysiologicalReadingDTO> postReading(
            @Valid @RequestBody PhysiologicalReadingDTO dto,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.createReading(dto, authentication.getName(), request.getRemoteAddr()));
    }

    /**
     * Ingests a physiological reading coming from a continuous stream.
     *
     * @param dto            The reading payload
     * @param authentication The security context of the user performing the request
     * @param request        The HTTP request used to extract the origin IP
     * @return A ResponseEntity containing the saved PhysiologicalReadingDTO
     */
    @PostMapping("/stream")
    public ResponseEntity<?> postReadingStream(
            @Valid @RequestBody PhysiologicalReadingDTO dto,
            Authentication authentication,
            HttpServletRequest request) {
            
        if (!service.isSimulationActive(dto.getSimulationId())) {
            return ResponseEntity.badRequest().body("Simulation is not active.");
        }
        
        service.createReadingAsync(dto, authentication.getName(), request.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    /**
     * Ingests physiological readings coming from a continuous HTTP stream (NDJSON format).
     * This fulfills the stream submission requirement (5.4.2) using an HTTP continuous connection.
     */
    @PostMapping(value = "/stream-continuous", consumes = "application/x-ndjson")
    public ResponseEntity<?> postReadingContinuousStream(
            InputStream inputStream,
            Authentication authentication,
            HttpServletRequest request) {
        String userEmail = authentication.getName();
        String ipAddress = request.getRemoteAddr();

        try (MappingIterator<PhysiologicalReadingDTO> iterator = objectMapper.readerFor(PhysiologicalReadingDTO.class).readValues(inputStream)) {
            while (iterator.hasNextValue()) {
                PhysiologicalReadingDTO dto = iterator.nextValue();
                service.createReadingAsync(dto, userEmail, ipAddress);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error reading continuous stream", e);
        }

        return ResponseEntity.accepted().build();
    }

    /**
     * Ingests physiological readings coming from a continuous WebSocket stream.
     * This fulfills the stream submission requirement (5.4.2) using a persistent connection.
     *
     * @param dto       The reading payload sent over STOMP
     * @param principal The security principal (if available)
     */
    @MessageMapping("/readings/stream")
    public void streamReadingWebSocket(@Payload @Valid PhysiologicalReadingDTO dto, Principal principal) {
        String userEmail = principal != null ? principal.getName() : "system@stream";
        service.createReadingAsync(dto, userEmail, "websocket");
    }

    /**
     * Processes a bulk upload of BioGears historical data via a Multipart CSV file.
     * The file is parsed into IEEE 11073 SDC standards and immediately saved as a bulk transaction.
     *
     * @param simulationId   The target simulation identifier
     * @param file           The MultipartFile containing the CSV data
     * @param authentication The security context of the user performing the request
     * @param request        The HTTP request used to extract the origin IP
     * @return A ResponseEntity containing the list of all ingested PhysiologicalReadingDTOs
     */
    @PostMapping(value = "/simulation/{simulationId}/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<java.util.Map<String, String>> uploadBioGearsBatch(
            @PathVariable UUID simulationId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            List<PhysiologicalReadingDTO> readings = parserService.parseCsv(file, simulationId);
            String userEmail = authentication.getName();
            String ipAddress = request.getRemoteAddr();

            service.createReadingBatchAsync(readings, userEmail, ipAddress);

            return ResponseEntity.accepted().body(java.util.Map.of("message", "Batch upload accepted and processed successfully."));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse CSV file: " + e.getMessage());
        }
    }

    /**
     * Retrieves all physiological readings associated with a specific simulation.
     *
     * @param simulationId The simulation identifier
     * @return A ResponseEntity containing a list of PhysiologicalReadingDTOs
     */
    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<List<PhysiologicalReadingDTO>> getBySimulation(@PathVariable UUID simulationId) {
        return ResponseEntity.ok(service.getReadingsBySimulation(simulationId));
    }

    /**
     * Ingests physiological readings coming from a standard JSON array in a single batch request.
     * This fulfills the batch submission requirement (5.4.1) using an explicit REST structure.
     *
     * @param dtos           The list of reading payloads
     * @param authentication The security context
     * @param request        The HTTP request
     * @return A 202 Accepted response
     */
    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postReadingBatch(
            @Valid @RequestBody List<PhysiologicalReadingDTO> dtos,
            Authentication authentication,
            HttpServletRequest request) {
        
        if (dtos == null || dtos.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Empty batch."));
        }
        
        if (!service.isSimulationActive(dtos.get(0).getSimulationId())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Simulation is not active."));
        }
        
        String userEmail = authentication.getName();
        String ipAddress = request.getRemoteAddr();

        service.createReadingBatchAsync(dtos, userEmail, ipAddress);
        
        return ResponseEntity.accepted().body(java.util.Map.of("message", "JSON Batch upload accepted and processed successfully."));
    }
}