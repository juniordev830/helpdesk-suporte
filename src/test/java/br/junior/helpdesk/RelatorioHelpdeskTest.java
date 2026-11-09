package br.junior.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RelatorioHelpdeskTest {
    private CentralAtendimento novaCentral() {
        return new CentralAtendimento(
                Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC),
                PoliticaSla.padrao());
    }

    @Test
    void geraRelatorioVazioSemDivisaoPorZero() {
        var texto = RelatorioHelpdesk.markdown(novaCentral(), LocalDate.of(2026, 11, 6));
        assertTrue(texto.contains("# Relatório de atendimento"));
        assertTrue(texto.contains("| Total | 0 |"));
        assertTrue(texto.contains("Nenhum chamado vencido"));
        assertTrue(texto.contains("Nenhum responsável atribuído"));
    }

    @Test
    void listaVencidosPorPrioridadeEData() {
        var central = novaCentral();
        central.abrir("Servidor parado", Chamado.Prioridade.CRITICA,
                "Não responde", "Infraestrutura", "Ana");
        central.abrir("Nova conta", Chamado.Prioridade.BAIXA,
                "Acesso solicitado", "Identidade", "Bia");
        var texto = RelatorioHelpdesk.markdown(central, LocalDate.of(2026, 11, 7));
        assertTrue(texto.contains("| Total | 2 |"));
        assertTrue(texto.contains("| Vencidos | 1 |"));
        assertTrue(texto.contains("Servidor parado"));
        assertTrue(texto.contains("Infraestrutura: 1"));
        assertFalse(texto.contains("| 2 | Nova conta |"));
    }

    @Test
    void protegeMarkdownContraTabelaInjetada() {
        var central = novaCentral();
        central.abrir("Falha | <script>", Chamado.Prioridade.CRITICA,
                "Descrição", "Rede|VPN", "Ana");
        var texto = RelatorioHelpdesk.markdown(central, LocalDate.of(2026, 11, 7));
        assertTrue(texto.contains("Falha \\| &lt;script&gt;"));
        assertTrue(texto.contains("Rede\\|VPN"));
        assertFalse(texto.contains("<script>"));
    }

    @Test
    void preservaRelatorioComChamadoResolvido() {
        var central = novaCentral();
        var ticket = central.abrir("Servidor", Chamado.Prioridade.CRITICA,
                "Parado", "Rede", "Ana");
        central.atribuir(ticket.chamado().id(), "Bia", "Ana");
        central.resolver(ticket.chamado().id(), "Bia");
        var texto = RelatorioHelpdesk.markdown(central, LocalDate.of(2026, 11, 8));
        assertTrue(texto.contains("| Resolvidos | 1 |"));
        assertTrue(texto.contains("| Vencidos | 0 |"));
        assertTrue(texto.contains("Bia: 1"));
    }

    @Test
    void destacaChamadosComPrazoEmDoisDiasECamposEscapados() {
        var central = novaCentral();
        central.abrir("Vencido", Chamado.Prioridade.CRITICA,
                "Chamado urgente", "Rede", "João");
        central.abrir("VPN | <teste>", Chamado.Prioridade.ALTA,
                "Verificar acesso", "Rede", "João");
        central.abrir("Prazo distante", Chamado.Prioridade.BAIXA,
                "Retorno futuro", "Rede", "João");

        var texto = RelatorioHelpdesk.markdown(central, LocalDate.of(2026, 11, 7));
        var inicio = texto.indexOf("## Próximos vencimentos");
        var fim = texto.indexOf("## Chamados vencidos");
        assertTrue(inicio >= 0 && fim > inicio);
        var secao = texto.substring(inicio, fim);
        assertTrue(secao.contains("VPN \\| &lt;teste&gt;"));
        assertTrue(secao.contains("2026-11-09"));
        assertTrue(secao.contains("Não atribuído"));
        assertFalse(secao.contains("Prazo distante"));
        assertFalse(secao.contains("Vencido"));
        assertTrue(texto.substring(fim).contains("Vencido"));
    }

    @Test
    void naoMostraChamadosResolvidosNosProximosVencimentos() {
        var central = novaCentral();
        var chamado = central.abrir("Atendimento", Chamado.Prioridade.ALTA,
                "Análise", "Rede", "Ana");
        central.atribuir(chamado.chamado().id(), "Ana", "Gerente");
        central.resolver(chamado.chamado().id(), "Ana");
        var texto = RelatorioHelpdesk.markdown(central, LocalDate.of(2026, 11, 7));
        assertTrue(texto.contains("Nenhum chamado com vencimento iminente"));
    }

    @Test
    void rejeitaParametrosNulos() {
        assertThrows(NullPointerException.class, () ->
                RelatorioHelpdesk.markdown(null, LocalDate.now()));
        assertThrows(NullPointerException.class, () ->
                RelatorioHelpdesk.markdown(novaCentral(), null));
    }
}
