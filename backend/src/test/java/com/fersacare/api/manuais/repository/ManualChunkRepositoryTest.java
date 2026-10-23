package com.fersacare.api.manuais.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualChunkRepositoryTest {

    @Test
    void formataVetorNoFormatoDoPgvector() {
        assertThat(ManualChunkRepository.paraVetor(new float[] {0.5f, -1f, 1.0E-5f})).isEqualTo("[0.5,-1.0,1.0E-5]");
        assertThat(ManualChunkRepository.paraVetor(new float[] {})).isEqualTo("[]");
    }
}
