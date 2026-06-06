package com.grupo3aor.innovationlab.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Outbound Data Transfer Object representing a Physiological System summary.
 * <p>
 * I designed this wrapper to serve as a data firewall, safely extracting 
 * necessary presentation fields from the persistence layer while intentionally 
 * masking sensitive infrastructure data like origin IPs or logical delete flags.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhysiologicalSystemResponse {

    /**
     * Public system identifier.
     */
    private Long id;

    /**
     * The name of the physiological system.
     */
    private String systemName;

    /**
     * Timestamp marking when the system was registered.
     */
    private LocalDateTime createdAt;

    /**
     * Operator identity who inserted the record.
     */
    private String createdBy;
}
