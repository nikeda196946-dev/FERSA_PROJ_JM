package com.fersacare.api.manuais.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PartNumberDetectorTest {

    private static final UUID FRS1001 = UUID.randomUUID();
    private static final UUID FRS1003 = UUID.randomUUID();
    private static final UUID HM = UUID.randomUUID();
    private static final List<PartNumberDetector.Peca> PECAS = List.of(
        new PartNumberDetector.Peca(FRS1001, "FRS-1001"),
        new PartNumberDetector.Peca(FRS1003, "FRS-1003"),
        new PartNumberDetector.Peca(HM, "HM212049/10"));

    private final PartNumberDetector detector = new PartNumberDetector();

    @Test
    void aceitaOsJeitosComunsDeEscreverOCodigo() {
        Map<UUID, Integer> r = detector.detectar(
            "Manual do FRS-1001. Também vale para o frs 1001, o FRS1001 e o (FRS.1001).", PECAS);
        assertThat(r).containsEntry(FRS1001, 4).doesNotContainKey(FRS1003);
    }

    @Test
    void naoConfundeCodigoMaiorQueContemOutro() {
        assertThat(detector.detectar("Rolamento FRS-10010 e ABCFRS-1001", PECAS)).isEmpty();
    }

    @Test
    void codigoComBarraEVariosManuaisNoMesmoTexto() {
        Map<UUID, Integer> r = detector.detectar("Kit HM212049/10 com FRS-1003; ver também HM 212049-10.", PECAS);
        assertThat(r).containsEntry(HM, 2).containsEntry(FRS1003, 1);
    }

    @Test
    void codigoCurtoDemaisNaoEDetectado() {
        UUID curto = UUID.randomUUID();
        assertThat(detector.detectar("A1 e 10 aparecem em todo lugar", List.of(new PartNumberDetector.Peca(curto, "A1")))).isEmpty();
        assertThat(detector.detectar(null, PECAS)).isEmpty();
    }
}
