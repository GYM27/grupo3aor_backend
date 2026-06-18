package com.grupo3aor.innovationlab.service;

import com.grupo3aor.innovationlab.dto.PhysiologicalReadingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BioGearsParserServiceTest {

    private BioGearsParserService parserService;
    private UUID simulationId;

    @BeforeEach
    void setUp() {
        parserService = new BioGearsParserService();
        simulationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("parseCsv: deve parsear corretamente um ficheiro CSV do BioGears com colunas SDC e multiplicar SpO2 por 100")
    void parseCsv_shouldParseValidCsvAndMapToSdc() throws Exception {
        // ARRANGE
        String csvContent = "Time(s),HeartRate(1/min),OxygenSaturation,SystolicArterialPressure(mmHg),CoreTemperature(C)\n" +
                "0.0,75.5,0.98,120.0,37.1\n" +
                "1.0,76.0,0.99,121.5,37.2\n";
        
        MultipartFile file = new MockMultipartFile("file", "biogears.csv", "text/csv", csvContent.getBytes());

        // ACT
        List<PhysiologicalReadingDTO> readings = parserService.parseCsv(file, simulationId);

        // ASSERT
        // 2 lines of data * 4 metrics (HR, SpO2, SBP, TEMP) = 8 readings
        assertThat(readings).hasSize(8);

        // Check HeartRate (HR)
        PhysiologicalReadingDTO firstHr = readings.stream().filter(r -> r.getHandle().equals("HR")).findFirst().orElseThrow();
        assertThat(firstHr.getValue()).isEqualTo(75.5);
        assertThat(firstHr.getUnit()).isEqualTo("1/min");

        // Check OxygenSaturation (SpO2) -> must be multiplied by 100
        PhysiologicalReadingDTO firstSpo2 = readings.stream().filter(r -> r.getHandle().equals("SpO2")).findFirst().orElseThrow();
        assertThat(firstSpo2.getValue()).isEqualTo(98.0); // 0.98 * 100
        assertThat(firstSpo2.getUnit()).isEqualTo("%");

        // Check SystolicArterialPressure (SBP)
        PhysiologicalReadingDTO firstSbp = readings.stream().filter(r -> r.getHandle().equals("SBP")).findFirst().orElseThrow();
        assertThat(firstSbp.getValue()).isEqualTo(120.0);
        assertThat(firstSbp.getUnit()).isEqualTo("mmHg");

        // Check CoreTemperature (TEMP)
        PhysiologicalReadingDTO firstTemp = readings.stream().filter(r -> r.getHandle().equals("TEMP")).findFirst().orElseThrow();
        assertThat(firstTemp.getValue()).isEqualTo(37.1);
        assertThat(firstTemp.getUnit()).isEqualTo("C");
    }

    @Test
    @DisplayName("parseCsv: deve lançar exceção se o ficheiro estiver vazio")
    void parseCsv_shouldThrowExceptionIfEmpty() {
        MultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", "".getBytes());

        assertThatThrownBy(() -> parserService.parseCsv(file, simulationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSV file is empty");
    }

    @Test
    @DisplayName("parseCsv: deve lançar exceção se faltar a coluna Time")
    void parseCsv_shouldThrowExceptionIfTimeMissing() {
        String csvContent = "HeartRate(1/min),OxygenSaturation\n75.5,0.98\n";
        MultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", csvContent.getBytes());

        assertThatThrownBy(() -> parserService.parseCsv(file, simulationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSV missing 'Time(s)' column");
    }

    @Test
    @DisplayName("parseCsv: deve ignorar linhas em branco ou mal formatadas sem rebentar")
    void parseCsv_shouldIgnoreBlankLines() throws Exception {
        String csvContent = "Time(s),HeartRate(1/min)\n" +
                "0.0,75.5\n" +
                "\n" + // Blank line
                "1.0,\n" + // Missing HR value
                "2.0,80.0\n";
        
        MultipartFile file = new MockMultipartFile("file", "skips.csv", "text/csv", csvContent.getBytes());

        List<PhysiologicalReadingDTO> readings = parserService.parseCsv(file, simulationId);

        // Expected: 0.0 gives HR=75.5, 2.0 gives HR=80.0. The blank line and missing HR are skipped.
        assertThat(readings).hasSize(2);
        assertThat(readings.get(0).getValue()).isEqualTo(75.5);
        assertThat(readings.get(1).getValue()).isEqualTo(80.0);
    }
}
