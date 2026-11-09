package br.junior.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PoliticaSlaTest {
    private static final LocalDate SEXTA = LocalDate.of(2026, 11, 6);

    @Test
    void prazosRespeitamDiasUteis() {
        var sla = PoliticaSla.padrao();
        assertEquals(SEXTA, sla.prazo(SEXTA, Chamado.Prioridade.CRITICA));
        assertEquals(LocalDate.of(2026, 11, 9), sla.prazo(SEXTA, Chamado.Prioridade.ALTA));
        assertEquals(LocalDate.of(2026, 11, 11), sla.prazo(SEXTA, Chamado.Prioridade.MEDIA));
        assertEquals(LocalDate.of(2026, 11, 13), sla.prazo(SEXTA, Chamado.Prioridade.BAIXA));
    }

    @Test
    void aberturaEmFimDeSemanaVaiParaSegunda() {
        var sla = PoliticaSla.padrao();
        assertEquals(LocalDate.of(2026, 11, 9),
                sla.prazo(LocalDate.of(2026, 11, 7), Chamado.Prioridade.CRITICA));
        assertEquals(LocalDate.of(2026, 11, 10),
                sla.prazo(LocalDate.of(2026, 11, 8), Chamado.Prioridade.ALTA));
    }

    @Test
    void feriadoPulaDiaUtil() {
        var sla = PoliticaSla.padrao().comFeriado(LocalDate.of(2026, 11, 9));
        assertEquals(LocalDate.of(2026, 11, 10), sla.prazo(SEXTA, Chamado.Prioridade.ALTA));
        assertFalse(sla.diaUtil(LocalDate.of(2026, 11, 9)));
        assertEquals(LocalDate.of(2026, 11, 9),
                PoliticaSla.padrao().proximoDiaUtil(LocalDate.of(2026, 11, 8)));
    }

    @Test
    void remocaoDeFeriadoNaoMudaPoliticaOriginal() {
        var data = LocalDate.of(2026, 11, 9);
        var original = PoliticaSla.padrao().comFeriado(data);
        var alterada = original.semFeriado(data);
        assertTrue(original.feriados().contains(data));
        assertFalse(alterada.feriados().contains(data));
    }

    @Test
    void configuraSlaSemModificarOriginal() {
        var original = PoliticaSla.padrao();
        var novo = original.comPrazo(Chamado.Prioridade.ALTA, 4);
        assertEquals(1, original.diasPara(Chamado.Prioridade.ALTA));
        assertEquals(4, novo.diasPara(Chamado.Prioridade.ALTA));
        assertEquals(4, novo.configuracao().get(Chamado.Prioridade.ALTA));
    }

    @Test
    void contaDiasUteisEmAmbasDirecoes() {
        var sla = PoliticaSla.padrao();
        var segunda = LocalDate.of(2026, 11, 9);
        assertEquals(1, sla.diasUteisEntre(SEXTA, segunda));
        assertEquals(-1, sla.diasUteisEntre(segunda, SEXTA));
        assertEquals(0, sla.diasUteisEntre(SEXTA, SEXTA));
    }

    @Test
    void verificaExpiracaoSemMarcarNoProprioPrazo() {
        var sla = PoliticaSla.padrao();
        assertFalse(sla.expirado(SEXTA, Chamado.Prioridade.ALTA, LocalDate.of(2026, 11, 9)));
        assertTrue(sla.expirado(SEXTA, Chamado.Prioridade.ALTA, LocalDate.of(2026, 11, 10)));
    }

    @Test
    void rejeitaPoliticaIncompletaENegativa() {
        assertThrows(IllegalArgumentException.class, () ->
                new PoliticaSla(Map.of(Chamado.Prioridade.ALTA, 1), Set.of()));
        assertThrows(IllegalArgumentException.class, () ->
                PoliticaSla.padrao().comPrazo(Chamado.Prioridade.ALTA, -1));
        assertThrows(IllegalArgumentException.class, () ->
                PoliticaSla.padrao().comPrazo(Chamado.Prioridade.ALTA, 366));
    }

    @Test
    void rejeitaArgumentosNulos() {
        assertThrows(NullPointerException.class, () -> PoliticaSla.padrao().diaUtil(null));
        assertThrows(NullPointerException.class, () -> PoliticaSla.padrao().prazo(null, Chamado.Prioridade.ALTA));
        assertThrows(NullPointerException.class, () -> PoliticaSla.padrao().diasPara(null));
    }

    @Test
    void configuracaoExpostaEImutavel() {
        var sla = PoliticaSla.padrao();
        assertThrows(UnsupportedOperationException.class, () ->
                sla.configuracao().put(Chamado.Prioridade.ALTA, 90));
        assertThrows(UnsupportedOperationException.class, () ->
                sla.feriados().add(SEXTA));
    }
}
