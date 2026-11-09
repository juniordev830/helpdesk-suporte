package br.junior.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CentralAtendimentoTest {
    private static final Instant INSTANTE = Instant.parse("2026-11-06T12:00:00Z");
    private static final LocalDate SEXTA = LocalDate.of(2026, 11, 6);
    private CentralAtendimento central;

    @BeforeEach
    void preparar() {
        central = new CentralAtendimento(Clock.fixed(INSTANTE, ZoneOffset.UTC),
                PoliticaSla.padrao());
    }

    private CentralAtendimento.Detalhe abrir(String titulo, Chamado.Prioridade prioridade) {
        return central.abrir(titulo, prioridade, "Descrição do chamado", "Acesso", "Solicitante");
    }

    @Test
    void aberturaCalculaPrazoERegistraAuditoria() {
        var detalhe = abrir("VPN indisponível", Chamado.Prioridade.ALTA);
        assertEquals(1, detalhe.chamado().id());
        assertEquals(Chamado.Status.ABERTO, detalhe.chamado().status());
        assertEquals(LocalDate.of(2026, 11, 9), detalhe.chamado().prazo());
        assertEquals(SEXTA, detalhe.abertura());
        assertEquals(1, detalhe.quantidadeEventos());
        assertEquals(RegistroEvento.Tipo.ABERTURA, detalhe.historico().get(0).tipo());
        assertEquals(INSTANTE, detalhe.ultimaAlteracao());
    }

    @Test
    void idsSaoSequenciais() {
        assertEquals(1, abrir("Um", Chamado.Prioridade.BAIXA).chamado().id());
        assertEquals(2, abrir("Dois", Chamado.Prioridade.MEDIA).chamado().id());
        assertEquals(3, abrir("Três", Chamado.Prioridade.ALTA).chamado().id());
    }

    @Test
    void atribuirEResolverPreservamCicloOriginal() {
        var id = abrir("Conta bloqueada", Chamado.Prioridade.MEDIA).chamado().id();
        assertThrows(IllegalStateException.class, () -> central.resolver(id, "Gestor"));
        var atribuido = central.atribuir(id, "Ana", "Gestor");
        assertEquals("Ana", atribuido.chamado().responsavel());
        assertEquals(Chamado.Status.EM_ATENDIMENTO, atribuido.chamado().status());
        var resolvido = central.resolver(id, "Ana");
        assertEquals(Chamado.Status.RESOLVIDO, resolvido.chamado().status());
        assertEquals(3, resolvido.quantidadeEventos());
        assertThrows(IllegalStateException.class, () -> central.atribuir(id, "Bia", "Gestor"));
    }

    @Test
    void resolverNovamenteNaoDuplicaEvento() {
        var id = abrir("Erro", Chamado.Prioridade.ALTA).chamado().id();
        central.atribuir(id, "Ana", "Gestor");
        central.resolver(id, "Ana");
        central.resolver(id, "Ana");
        assertEquals(3, central.auditoria(id).size());
    }

    @Test
    void anotacoesSaoAuditadas() {
        var id = abrir("Acesso", Chamado.Prioridade.BAIXA).chamado().id();
        central.anotar(id, "Ana", "Contato realizado");
        central.anotar(id, "Bia", "Aguardando retorno");
        var eventos = central.auditoria(id);
        assertEquals(3, eventos.size());
        assertEquals("Contato realizado", eventos.get(1).descricao());
        assertEquals("Bia", eventos.get(2).autor());
    }

    @Test
    void etiquetasSaoNormalizadasEIdempotentes() {
        var id = abrir("Acesso", Chamado.Prioridade.BAIXA).chamado().id();
        central.adicionarEtiqueta(id, " URGENTE ", "Ana");
        central.adicionarEtiqueta(id, "urgente", "Ana");
        assertEquals(Set.of("urgente"), central.consultar(id).etiquetas());
        assertEquals(2, central.auditoria(id).size());
        central.removerEtiqueta(id, "Urgente", "Ana");
        central.removerEtiqueta(id, "urgente", "Ana");
        assertTrue(central.consultar(id).etiquetas().isEmpty());
        assertEquals(3, central.auditoria(id).size());
    }

    @Test
    void rejeitaEtiquetaInvalida() {
        var id = abrir("Acesso", Chamado.Prioridade.MEDIA).chamado().id();
        assertThrows(IllegalArgumentException.class,
                () -> central.adicionarEtiqueta(id, "com espaço", "Ana"));
        assertThrows(IllegalArgumentException.class,
                () -> central.adicionarEtiqueta(id, "áé", "Ana"));
        assertThrows(IllegalArgumentException.class,
                () -> central.adicionarEtiqueta(id, "", "Ana"));
    }

    @Test
    void alteracaoDeCategoriaEhAuditadaSomenteQuandoMuda() {
        var id = abrir("Acesso", Chamado.Prioridade.MEDIA).chamado().id();
        central.alterarCategoria(id, "Rede", "Ana");
        central.alterarCategoria(id, "Rede", "Ana");
        assertEquals("Rede", central.consultar(id).categoria());
        assertEquals(2, central.auditoria(id).size());
    }

    @Test
    void snapshotsNaoPodemSerAlteradosExternamente() {
        var id = abrir("Acesso", Chamado.Prioridade.MEDIA).chamado().id();
        var antes = central.consultar(id);
        assertThrows(UnsupportedOperationException.class, () -> antes.etiquetas().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> antes.historico().clear());
        assertThrows(UnsupportedOperationException.class, () -> central.listar().clear());
        central.adicionarEtiqueta(id, "rede", "Ana");
        assertTrue(antes.etiquetas().isEmpty());
        assertEquals(Set.of("rede"), central.consultar(id).etiquetas());
    }

    @Test
    void buscaPorTituloEDescricaoIgnoraMaiusculas() {
        abrir("VPN Indisponível", Chamado.Prioridade.ALTA);
        central.abrir("Outro", Chamado.Prioridade.BAIXA, "Falha de autenticação",
                "Acesso", "Ana");
        assertEquals(1, central.pesquisar(
                CentralAtendimento.Filtro.todos().comBusca("vpn"), 0, 10).total());
        assertEquals(1, central.pesquisar(
                CentralAtendimento.Filtro.todos().comBusca("AUTENTICAÇÃO"), 0, 10).total());
    }

    @Test
    void filtraStatusPrioridadeCategoriaEResponsavel() {
        var id = abrir("Falha", Chamado.Prioridade.CRITICA).chamado().id();
        abrir("Solicitação", Chamado.Prioridade.BAIXA);
        central.atribuir(id, "Ana", "Gestor");
        var filtro = CentralAtendimento.Filtro.todos()
                .comStatus(Chamado.Status.EM_ATENDIMENTO)
                .comPrioridade(Chamado.Prioridade.CRITICA)
                .comCategoria("acesso")
                .comResponsavel("ANA");
        assertEquals(1, central.pesquisar(filtro, 0, 20).total());
        assertEquals(0, central.pesquisar(filtro.comStatus(Chamado.Status.RESOLVIDO), 0, 20).total());
    }

    @Test
    void filtraEtiquetasEVencimentos() {
        var id = abrir("Falha", Chamado.Prioridade.CRITICA).chamado().id();
        abrir("Solicitação", Chamado.Prioridade.BAIXA);
        central.adicionarEtiqueta(id, "incidente", "Ana");
        assertEquals(1, central.pesquisar(
                CentralAtendimento.Filtro.todos().comEtiqueta("Incidente"), 0, 20).total());
        assertEquals(1, central.pesquisar(
                CentralAtendimento.Filtro.todos().comVencidos(LocalDate.of(2026, 11, 7)),
                0, 20).total());
    }

    @Test
    void naoContaChamadoResolvidoComoVencido() {
        var id = abrir("Falha", Chamado.Prioridade.CRITICA).chamado().id();
        central.atribuir(id, "Ana", "Gestor");
        central.resolver(id, "Ana");
        assertEquals(0, central.pesquisar(
                CentralAtendimento.Filtro.todos().comVencidos(LocalDate.of(2026, 11, 9)),
                0, 20).total());
    }

    @Test
    void ordenaFilaPorPrioridadeEData() {
        abrir("Baixa", Chamado.Prioridade.BAIXA);
        abrir("Alta", Chamado.Prioridade.ALTA);
        abrir("Crítica", Chamado.Prioridade.CRITICA);
        var itens = central.pesquisar(CentralAtendimento.Filtro.todos(), 0, 10).itens();
        assertEquals(List.of("Crítica", "Alta", "Baixa"),
                itens.stream().map(d -> d.chamado().titulo()).toList());
    }

    @Test
    void paginacaoRespeitaLimitesETotal() {
        for (int i = 0; i < 7; i++) abrir("Chamado " + i, Chamado.Prioridade.MEDIA);
        var primeira = central.pesquisar(CentralAtendimento.Filtro.todos(), 0, 3);
        assertEquals(7, primeira.total());
        assertEquals(3, primeira.itens().size());
        assertEquals(3, primeira.totalPaginas());
        assertTrue(primeira.temProxima());
        assertEquals(1, central.pesquisar(CentralAtendimento.Filtro.todos(), 2, 3).itens().size());
        assertTrue(central.pesquisar(CentralAtendimento.Filtro.todos(), 4, 3).itens().isEmpty());
    }

    @Test
    void rejeitaPaginacaoInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> central.pesquisar(CentralAtendimento.Filtro.todos(), -1, 10));
        assertThrows(IllegalArgumentException.class,
                () -> central.pesquisar(CentralAtendimento.Filtro.todos(), 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> central.pesquisar(CentralAtendimento.Filtro.todos(), 0, 201));
    }

    @Test
    void proximosVencimentosNaoIncluemPassadosNemResolvidos() {
        abrir("Crítica", Chamado.Prioridade.CRITICA);
        abrir("Alta", Chamado.Prioridade.ALTA);
        var id = abrir("Média", Chamado.Prioridade.MEDIA).chamado().id();
        central.atribuir(id, "Ana", "Gestor");
        central.resolver(id, "Ana");
        assertEquals(List.of("Alta"),
                central.proximosVencimentos(LocalDate.of(2026, 11, 9), 2).stream()
                        .map(d -> d.chamado().titulo()).toList());
    }

    @Test
    void metricasConsolidamStatusPrazosEGrupos() {
        var id = abrir("Crítica", Chamado.Prioridade.CRITICA).chamado().id();
        abrir("Baixa", Chamado.Prioridade.BAIXA);
        central.atribuir(id, "Ana", "Gestor");
        var metricas = central.metricas(LocalDate.of(2026, 11, 7));
        assertEquals(2, metricas.total());
        assertEquals(1, metricas.abertos());
        assertEquals(1, metricas.emAtendimento());
        assertEquals(0, metricas.resolvidos());
        assertEquals(1, metricas.vencidos());
        assertEquals(2, metricas.pendentes());
        assertEquals(1, metricas.porResponsavel().get("Ana"));
        assertEquals(2, metricas.porCategoria().get("Acesso"));
        assertEquals(1, metricas.porPrioridade().get(Chamado.Prioridade.CRITICA));
    }

    @Test
    void percentualResolvidosEhSeguroComZeroChamados() {
        assertEquals(0.0, central.metricas(SEXTA).percentualResolvidos());
        var id = abrir("Falha", Chamado.Prioridade.ALTA).chamado().id();
        central.atribuir(id, "Ana", "Gestor");
        central.resolver(id, "Ana");
        assertEquals(100.0, central.metricas(SEXTA).percentualResolvidos());
    }

    @Test
    void auditoriaGlobalRespeitaIntervalo() {
        abrir("A", Chamado.Prioridade.BAIXA);
        abrir("B", Chamado.Prioridade.ALTA);
        assertEquals(2, central.auditoriaGlobal(INSTANTE, INSTANTE).size());
        assertTrue(central.auditoriaGlobal(INSTANTE.plusSeconds(1),
                INSTANTE.plusSeconds(10)).isEmpty());
        assertThrows(IllegalArgumentException.class, () ->
                central.auditoriaGlobal(INSTANTE.plusSeconds(1), INSTANTE));
    }

    @Test
    void exportaCsvComCamposReaisEProtecaoDeFormula() {
        abrir("=HYPERLINK(1)", Chamado.Prioridade.ALTA);
        var linhas = CsvChamados.lerRelatorio(central.exportarCsv());
        assertEquals(2, linhas.size());
        assertEquals("'=HYPERLINK(1)", linhas.get(1).get(1));
        assertEquals("ALTA", linhas.get(1).get(2));
        assertEquals("Acesso", linhas.get(1).get(7));
    }

    @Test
    void rejeitaChamadoDesconhecido() {
        assertThrows(IllegalArgumentException.class, () -> central.consultar(99));
        assertThrows(IllegalArgumentException.class, () -> central.atribuir(99, "Ana", "Gestor"));
        assertThrows(IllegalArgumentException.class, () -> central.anotar(99, "Ana", "nota"));
        assertThrows(IllegalArgumentException.class, () -> central.resolver(99, "Ana"));
    }

    @Test
    void rejeitaCamposInvalidosSemCriarChamados() {
        assertThrows(IllegalArgumentException.class, () ->
                central.abrir("A", Chamado.Prioridade.ALTA, " ", "Rede", "Ana"));
        assertThrows(IllegalArgumentException.class, () ->
                central.abrir("A", Chamado.Prioridade.ALTA, "desc", " ", "Ana"));
        assertThrows(IllegalArgumentException.class, () ->
                central.abrir("A", Chamado.Prioridade.ALTA, "desc", "Rede", ""));
        assertEquals(0, central.listar().size());
    }

    @Test
    void rejeitaServicoPreenchidoParaEvitarMetadadosOrfaos() {
        var service = new HelpdeskService();
        service.abrir("Pré-existente", Chamado.Prioridade.ALTA, SEXTA);
        assertThrows(IllegalArgumentException.class, () ->
                new CentralAtendimento(service, Clock.systemUTC(), PoliticaSla.padrao()));
    }
}
