package br.junior.helpdesk;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Human-readable operational report with bounded, escaped output. */
public final class RelatorioHelpdesk {
    private RelatorioHelpdesk() {}

    public static String markdown(CentralAtendimento central, LocalDate hoje) {
        Objects.requireNonNull(central);
        Objects.requireNonNull(hoje);
        var metricas = central.metricas(hoje);
        var texto = new StringBuilder();
        texto.append("# Relatório de atendimento\n\n");
        texto.append("Data de referência: ").append(hoje).append("\n\n");
        texto.append("## Indicadores\n\n");
        texto.append("| Indicador | Quantidade |\n|---|---:|\n");
        linha(texto, "Total", metricas.total());
        linha(texto, "Abertos", metricas.abertos());
        linha(texto, "Em atendimento", metricas.emAtendimento());
        linha(texto, "Resolvidos", metricas.resolvidos());
        linha(texto, "Vencidos", metricas.vencidos());
        texto.append("\n## Prioridades\n\n");
        for (var prioridade : Chamado.Prioridade.values()) {
            texto.append("- ").append(prioridade).append(": ")
                    .append(metricas.porPrioridade().getOrDefault(prioridade, 0L)).append("\n");
        }
        texto.append("\n## Categorias\n\n");
        if (metricas.porCategoria().isEmpty()) texto.append("Nenhuma categoria cadastrada.\n");
        else grupos(texto, metricas.porCategoria());
        texto.append("\n## Responsáveis\n\n");
        if (metricas.porResponsavel().isEmpty()) texto.append("Nenhum responsável atribuído.\n");
        else grupos(texto, metricas.porResponsavel());
        texto.append("\n## Próximos vencimentos (2 dias corridos)\n\n");
        var proximos = central.proximosVencimentos(hoje, 2);
        if (proximos.isEmpty()) {
            texto.append("Nenhum chamado com vencimento iminente.\n");
        } else {
            texto.append("| ID | Título | Prazo | Prioridade | Responsável |\n")
                    .append("|---:|---|---|---|---|\n");
            for (var detalhe : proximos.stream().limit(200).toList()) {
                var chamado = detalhe.chamado();
                texto.append("| ").append(chamado.id()).append(" | ")
                        .append(escaparMarkdown(chamado.titulo())).append(" | ")
                        .append(chamado.prazo()).append(" | ")
                        .append(chamado.prioridade()).append(" | ")
                        .append(escaparMarkdown(chamado.responsavel() == null
                                ? "Não atribuído" : chamado.responsavel())).append(" |\n");
            }
            if (proximos.size() > 200) {
                texto.append("\nExibindo os primeiros 200 de ")
                        .append(proximos.size()).append(" chamados próximos do vencimento.\n");
            }
        }
        texto.append("\n## Chamados vencidos\n\n");
        var vencidos = central.pesquisar(
                CentralAtendimento.Filtro.todos().comVencidos(hoje), 0, 200).itens();
        if (vencidos.isEmpty()) {
            texto.append("Nenhum chamado vencido.\n");
        } else {
            texto.append("| ID | Título | Prazo | Prioridade |\n|---:|---|---|---|\n");
            for (var detalhe : vencidos) {
                var chamado = detalhe.chamado();
                texto.append("| ").append(chamado.id()).append(" | ")
                        .append(escaparMarkdown(chamado.titulo())).append(" | ")
                        .append(chamado.prazo()).append(" | ")
                        .append(chamado.prioridade()).append(" |\n");
            }
            if (metricas.vencidos() > vencidos.size()) {
                texto.append("\nExibindo os primeiros ").append(vencidos.size())
                        .append(" de ").append(metricas.vencidos()).append(" chamados vencidos.\n");
            }
        }
        return texto.toString();
    }

    private static void linha(StringBuilder sb, String titulo, int valor) {
        sb.append("| ").append(titulo).append(" | ").append(valor).append(" |\n");
    }

    private static void grupos(StringBuilder sb, Map<String, Long> grupos) {
        grupos.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append("- ").append(escaparMarkdown(e.getKey()))
                        .append(": ").append(e.getValue()).append("\n"));
    }

    static String escaparMarkdown(String valor) {
        Objects.requireNonNull(valor);
        return valor.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
