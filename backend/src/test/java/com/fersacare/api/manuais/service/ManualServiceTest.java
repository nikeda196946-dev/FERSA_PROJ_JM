package com.fersacare.api.manuais.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualServiceTest {

    @Test
    void nomeSeguroTiraPastaDoCaminho() {
        assertThat(ManualService.nomeSeguro("C:\\Users\\joao\\Manual FRS.pdf")).isEqualTo("Manual FRS.pdf");
        assertThat(ManualService.nomeSeguro("../../etc/manual.pdf")).isEqualTo("manual.pdf");
        assertThat(ManualService.nomeSeguro(null)).isEqualTo("manual.pdf");
        assertThat(ManualService.nomeSeguro("")).isEqualTo("manual.pdf");
    }

    @Test
    void tituloPadraoVemDoNomeDoArquivo() {
        assertThat(ManualService.tituloPadrao("manual_instalacao-FRS-1001.PDF")).isEqualTo("manual instalacao FRS 1001");
        assertThat(ManualService.tituloPadrao(".pdf")).isEqualTo("Manual sem título");
    }
}
