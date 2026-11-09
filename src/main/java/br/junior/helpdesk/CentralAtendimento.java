package br.junior.helpdesk;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service built on top of the original ticket lifecycle.
 * All mutations are serialized; callers receive immutable snapshots.
 */
public final class CentralAtendimento {
    private final HelpdeskService service;
    private final PoliticaSla sla;
    private final Clock relogio;
    private final Map<Integer, Metadados> metadados = new LinkedHashMap<>();

    private static final class Metadados {
        private String descricao;
        private String categoria;
        private final LocalDate abertura;
        private final Set<String> etiquetas = new HashSet<>();
        private final List<RegistroEvento> historico = new ArrayList<>();

        private Metadados(String descricao, String categoria, LocalDate abertura) {
            this.descricao = descricao;
            this.categoria = categoria;
            this.abertura = abertura;
        }
    }

    public record Detalhe(
            Chamado chamado,
            String descricao,
            String categoria,
            LocalDate abertura,
            Set<String> etiquetas,
            List<RegistroEvento> historico) {
        public Detalhe {
            Objects.requireNonNull(chamado);
            Objects.requireNonNull(descricao);
            Objects.requireNonNull(categoria);
            Objects.requireNonNull(abertura);
            etiquetas = Set.copyOf(etiquetas);
            historico = List.copyOf(historico);
        }

        public boolean vencidoEm(LocalDate data) {
            return chamado.vencido(data);
        }

        public boolean contemEtiqueta(String etiqueta) {
            return etiquetas.contains(normalizarEtiqueta(etiqueta));
        }

        public int quantidadeEventos() {
            return historico.size();
        }

        public Instant ultimaAlteracao() {
            return historico.get(historico.size() - 1).instante();
        }
    }

    public record Filtro(
            Chamado.Status status,
            Chamado.Prioridade prioridade,
            String responsavel,
            String categoria,
            String etiqueta,
            String busca,
            Boolean vencido,
            LocalDate hoje) {
        public static Filtro todos() {
            return new Filtro(null, null, null, null, null, null, null, null);
        }

        public Filtro comStatus(Chamado.Status valor) {
            return new Filtro(valor, prioridade, responsavel, categoria, etiqueta, busca, vencido, hoje);
        }

        public Filtro comPrioridade(Chamado.Prioridade valor) {
            return new Filtro(status, valor, responsavel, categoria, etiqueta, busca, vencido, hoje);
        }

        public Filtro comResponsavel(String valor) {
            return new Filtro(status, prioridade, valor, categoria, etiqueta, busca, vencido, hoje);
        }

        public Filtro comCategoria(String valor) {
            return new Filtro(status, prioridade, responsavel, valor, etiqueta, busca, vencido, hoje);
        }

        public Filtro comEtiqueta(String valor) {
            return new Filtro(status, prioridade, responsavel, categoria, valor, busca, vencido, hoje);
        }

        public Filtro comBusca(String valor) {
            return new Filtro(status, prioridade, responsavel, categoria, etiqueta, valor, vencido, hoje);
        }

        public Filtro comVencidos(LocalDate data) {
            return new Filtro(status, prioridade, responsavel, categoria, etiqueta, busca, true, data);
        }
    }

    public record Pagina(List<Detalhe> itens, int pagina, int tamanho, long total) {
        public Pagina {
            itens = List.copyOf(itens);
            if (pagina < 0 || tamanho < 1 || total < 0) {
                throw new IllegalArgumentException("Paginação inválida");
            }
        }

        public long totalPaginas() {
            return (total + tamanho - 1) / tamanho;
        }

        public boolean temProxima() {
            return (long) (pagina + 1) * tamanho < total;
        }
    }

    public record Metricas(
            int total,
            int abertos,
            int emAtendimento,
            int resolvidos,
            int vencidos,
            Map<Chamado.Prioridade, Long> porPrioridade,
            Map<String, Long> porCategoria,
            Map<String, Long> porResponsavel) {
        public Metricas {
            porPrioridade = Map.copyOf(porPrioridade);
            porCategoria = Map.copyOf(porCategoria);
            porResponsavel = Map.copyOf(porResponsavel);
        }

        public double percentualResolvidos() {
            return total == 0 ? 0.0 : (100.0 * resolvidos) / total;
        }

        public int pendentes() {
            return abertos + emAtendimento;
        }
    }

    public CentralAtendimento(Clock relogio, PoliticaSla sla) {
        this(new HelpdeskService(), relogio, sla);
    }

    public CentralAtendimento(HelpdeskService service, Clock relogio, PoliticaSla sla) {
        this.service = Objects.requireNonNull(service, "Serviço obrigatório");
        this.relogio = Objects.requireNonNull(relogio, "Relógio obrigatório");
        this.sla = Objects.requireNonNull(sla, "Política de SLA obrigatória");
        if (!service.listar().isEmpty()) {
            throw new IllegalArgumentException("Use um serviço sem chamados pré-existentes");
        }
    }

    public synchronized Detalhe abrir(
            String titulo,
            Chamado.Prioridade prioridade,
            String descricao,
            String categoria,
            String autor) {
        var textoDescricao = texto(descricao, "Descrição", 2000);
        var textoCategoria = texto(categoria, "Categoria", 80);
        var textoAutor = texto(autor, "Autor", 100);
        var data = LocalDate.now(relogio);
        var prazo = sla.prazo(data, prioridade);
        var chamado = service.abrir(titulo, prioridade, prazo);
        var meta = new Metadados(textoDescricao, textoCategoria, data);
        metadados.put(chamado.id(), meta);
        registrar(chamado.id(), RegistroEvento.Tipo.ABERTURA, textoAutor, "Chamado aberto");
        return detalhe(chamado);
    }

    public synchronized Detalhe atribuir(int id, String responsavel, String autor) {
        var pessoa = texto(responsavel, "Responsável", 100);
        var quem = texto(autor, "Autor", 100);
        var chamado = service.atribuir(id, pessoa);
        registrar(id, RegistroEvento.Tipo.ATRIBUICAO, quem, "Atribuído a " + pessoa);
        return detalhe(chamado);
    }

    public synchronized Detalhe resolver(int id, String autor) {
        var quem = texto(autor, "Autor", 100);
        var antes = localizar(id);
        var depois = service.resolver(id);
        if (antes.status() != Chamado.Status.RESOLVIDO) {
            registrar(id, RegistroEvento.Tipo.RESOLUCAO, quem, "Chamado resolvido");
        }
        return detalhe(depois);
    }

    public synchronized Detalhe anotar(int id, String autor, String nota) {
        var quem = texto(autor, "Autor", 100);
        var mensagem = texto(nota, "Nota", 1000);
        var chamado = localizar(id);
        registrar(id, RegistroEvento.Tipo.NOTA, quem, mensagem);
        return detalhe(chamado);
    }

    public synchronized Detalhe adicionarEtiqueta(int id, String etiqueta, String autor) {
        var quem = texto(autor, "Autor", 100);
        var tag = normalizarEtiqueta(etiqueta);
        var chamado = localizar(id);
        if (metadados.get(id).etiquetas.add(tag)) {
            registrar(id, RegistroEvento.Tipo.ETIQUETA_ADICIONADA, quem, tag);
        }
        return detalhe(chamado);
    }

    public synchronized Detalhe removerEtiqueta(int id, String etiqueta, String autor) {
        var quem = texto(autor, "Autor", 100);
        var tag = normalizarEtiqueta(etiqueta);
        var chamado = localizar(id);
        if (metadados.get(id).etiquetas.remove(tag)) {
            registrar(id, RegistroEvento.Tipo.ETIQUETA_REMOVIDA, quem, tag);
        }
        return detalhe(chamado);
    }

    public synchronized Detalhe alterarCategoria(int id, String categoria, String autor) {
        var quem = texto(autor, "Autor", 100);
        var nova = texto(categoria, "Categoria", 80);
        var chamado = localizar(id);
        var meta = metadados.get(id);
        if (!meta.categoria.equals(nova)) {
            meta.categoria = nova;
            registrar(id, RegistroEvento.Tipo.CATEGORIA_ALTERADA, quem, nova);
        }
        return detalhe(chamado);
    }

    public synchronized Detalhe consultar(int id) {
        return detalhe(localizar(id));
    }

    public synchronized List<Detalhe> listar() {
        return service.listar().stream().map(this::detalhe).toList();
    }

    public synchronized List<RegistroEvento> auditoria(int id) {
        localizar(id);
        return List.copyOf(metadados.get(id).historico);
    }

    public synchronized List<RegistroEvento> auditoriaGlobal(Instant inicio, Instant fim) {
        Objects.requireNonNull(inicio);
        Objects.requireNonNull(fim);
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("Intervalo inválido");
        return metadados.values().stream()
                .flatMap(meta -> meta.historico.stream())
                .filter(evento -> evento.entre(inicio, fim))
                .sorted(Comparator.comparing(RegistroEvento::instante)
                        .thenComparingInt(RegistroEvento::chamadoId))
                .toList();
    }

    public synchronized Pagina pesquisar(Filtro filtro, int pagina, int tamanho) {
        Objects.requireNonNull(filtro, "Filtro obrigatório");
        if (pagina < 0 || tamanho < 1 || tamanho > 200) {
            throw new IllegalArgumentException("Página ou tamanho inválido");
        }
        var encontrados = listar().stream()
                .filter(d -> corresponde(d, filtro))
                .sorted(Comparator
                        .comparingInt((Detalhe d) -> d.chamado().prioridade().ordinal()).reversed()
                        .thenComparing(d -> d.chamado().prazo())
                        .thenComparingInt(d -> d.chamado().id()))
                .toList();
        long inicio = (long) pagina * tamanho;
        if (inicio >= encontrados.size()) return new Pagina(List.of(), pagina, tamanho, encontrados.size());
        int fim = (int) Math.min(inicio + tamanho, encontrados.size());
        return new Pagina(encontrados.subList((int) inicio, fim), pagina, tamanho, encontrados.size());
    }

    public synchronized List<Detalhe> proximosVencimentos(LocalDate hoje, int dias) {
        Objects.requireNonNull(hoje);
        if (dias < 0 || dias > 365) throw new IllegalArgumentException("Janela inválida");
        var limite = hoje.plusDays(dias);
        return listar().stream()
                .filter(d -> d.chamado().status() != Chamado.Status.RESOLVIDO)
                .filter(d -> !d.chamado().prazo().isBefore(hoje))
                .filter(d -> !d.chamado().prazo().isAfter(limite))
                .sorted(Comparator.comparing(d -> d.chamado().prazo()))
                .toList();
    }

    public synchronized Metricas metricas(LocalDate hoje) {
        Objects.requireNonNull(hoje);
        var todos = listar();
        int abertos = (int) todos.stream().filter(d -> d.chamado().status() == Chamado.Status.ABERTO).count();
        int atendendo = (int) todos.stream().filter(d -> d.chamado().status() == Chamado.Status.EM_ATENDIMENTO).count();
        int resolvidos = (int) todos.stream().filter(d -> d.chamado().status() == Chamado.Status.RESOLVIDO).count();
        int vencidos = (int) todos.stream().filter(d -> d.vencidoEm(hoje)).count();
        var prioridades = new EnumMap<Chamado.Prioridade, Long>(Chamado.Prioridade.class);
        for (var prioridade : Chamado.Prioridade.values()) prioridades.put(prioridade, 0L);
        todos.forEach(d -> prioridades.merge(d.chamado().prioridade(), 1L, Long::sum));
        var categorias = todos.stream().collect(Collectors.groupingBy(
                Detalhe::categoria, Collectors.counting()));
        var responsaveis = todos.stream().filter(d -> d.chamado().responsavel() != null)
                .collect(Collectors.groupingBy(d -> d.chamado().responsavel(), Collectors.counting()));
        return new Metricas(todos.size(), abertos, atendendo, resolvidos, vencidos,
                prioridades, categorias, responsaveis);
    }

    public synchronized String exportarCsv() {
        return CsvChamados.exportar(listar());
    }

    public PoliticaSla politicaSla() {
        return sla;
    }

    private boolean corresponde(Detalhe d, Filtro f) {
        var c = d.chamado();
        if (f.status() != null && c.status() != f.status()) return false;
        if (f.prioridade() != null && c.prioridade() != f.prioridade()) return false;
        if (f.responsavel() != null && !igual(c.responsavel(), f.responsavel())) return false;
        if (f.categoria() != null && !igual(d.categoria(), f.categoria())) return false;
        if (f.etiqueta() != null && !d.contemEtiqueta(f.etiqueta())) return false;
        if (f.vencido() != null) {
            if (f.hoje() == null) throw new IllegalArgumentException("Data do filtro obrigatória");
            if (d.vencidoEm(f.hoje()) != f.vencido()) return false;
        }
        if (f.busca() != null && !f.busca().isBlank()) {
            var busca = f.busca().trim().toLowerCase(Locale.ROOT);
            if (!c.titulo().toLowerCase(Locale.ROOT).contains(busca)
                    && !d.descricao().toLowerCase(Locale.ROOT).contains(busca)) return false;
        }
        return true;
    }

    private static boolean igual(String primeiro, String segundo) {
        return primeiro != null && primeiro.equalsIgnoreCase(segundo.trim());
    }

    private Chamado localizar(int id) {
        return service.listar().stream().filter(c -> c.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + id));
    }

    private Detalhe detalhe(Chamado chamado) {
        var meta = metadados.get(chamado.id());
        if (meta == null) throw new IllegalStateException("Metadados ausentes");
        return new Detalhe(chamado, meta.descricao, meta.categoria, meta.abertura,
                meta.etiquetas, meta.historico);
    }

    private void registrar(int id, RegistroEvento.Tipo tipo, String autor, String descricao) {
        metadados.get(id).historico.add(
                new RegistroEvento(id, tipo, autor, descricao, Instant.now(relogio)));
    }

    static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank() || valor.trim().length() > limite) {
            throw new IllegalArgumentException(campo + " inválido");
        }
        return valor.trim();
    }

    static String normalizarEtiqueta(String etiqueta) {
        var valor = texto(etiqueta, "Etiqueta", 40).toLowerCase(Locale.ROOT);
        if (!valor.matches("[a-z0-9][a-z0-9_-]*")) {
            throw new IllegalArgumentException("Etiqueta deve conter letras, números, _ ou -");
        }
        return valor;
    }
}
