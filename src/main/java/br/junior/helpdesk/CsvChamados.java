package br.junior.helpdesk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * CSV export for spreadsheets and a strict RFC4180-style reader for validation.
 * Spreadsheet formula prefixes are escaped to prevent CSV injection.
 */
public final class CsvChamados {
    private CsvChamados() {}

    public static final List<String> CABECALHO = List.of(
            "id", "titulo", "prioridade", "responsavel", "abertura", "prazo",
            "status", "categoria", "descricao", "etiquetas", "eventos");

    public static String exportar(List<CentralAtendimento.Detalhe> chamados) {
        Objects.requireNonNull(chamados, "Chamados obrigatórios");
        var csv = new StringBuilder();
        csv.append(String.join(",", CABECALHO)).append("\r\n");
        for (var detalhe : chamados) {
            Objects.requireNonNull(detalhe, "Chamado nulo");
            var chamado = detalhe.chamado();
            var tags = detalhe.etiquetas().stream().sorted().toList();
            csv.append(linha(List.of(
                    Integer.toString(chamado.id()),
                    chamado.titulo(),
                    chamado.prioridade().name(),
                    chamado.responsavel() == null ? "" : chamado.responsavel(),
                    detalhe.abertura().toString(),
                    chamado.prazo().toString(),
                    chamado.status().name(),
                    detalhe.categoria(),
                    detalhe.descricao(),
                    String.join(";", tags),
                    Integer.toString(detalhe.quantidadeEventos()))));
            csv.append("\r\n");
        }
        return csv.toString();
    }

    public static String linha(List<String> campos) {
        Objects.requireNonNull(campos);
        var joiner = new StringJoiner(",");
        for (var campo : campos) joiner.add(escapar(campo));
        return joiner.toString();
    }

    public static String escapar(String campo) {
        Objects.requireNonNull(campo, "Campo nulo");
        var seguro = campo;
        int i = 0;
        while (i < seguro.length() && Character.isWhitespace(seguro.charAt(i))) i++;
        if (i < seguro.length() && "=+-@".indexOf(seguro.charAt(i)) >= 0) {
            seguro = "'" + seguro;
        }
        if (seguro.contains(",") || seguro.contains("\"")
                || seguro.contains("\r") || seguro.contains("\n")) {
            return "\"" + seguro.replace("\"", "\"\"") + "\"";
        }
        return seguro;
    }

    /**
     * Parses quoted CSV, including embedded commas, quotes and newlines.
     * Rejects malformed quoting and excessively large inputs.
     */
    public static List<List<String>> ler(String csv) {
        Objects.requireNonNull(csv, "CSV obrigatório");
        if (csv.length() > 2_000_000) throw new IllegalArgumentException("CSV muito grande");
        var linhas = new ArrayList<List<String>>();
        var campos = new ArrayList<String>();
        var campo = new StringBuilder();
        boolean aspas = false;
        boolean fechado = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (aspas) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        aspas = false;
                        fechado = true;
                    }
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                if (campo.length() != 0 || fechado) {
                    throw new IllegalArgumentException("Aspas fora do início do campo");
                }
                aspas = true;
            } else if (c == ',') {
                adicionarCampo(campos, campo);
                fechado = false;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                adicionarCampo(campos, campo);
                adicionarLinha(linhas, campos);
                fechado = false;
            } else {
                if (fechado) throw new IllegalArgumentException("Texto após aspas finais");
                campo.append(c);
                if (campo.length() > 100_000) throw new IllegalArgumentException("Campo muito grande");
            }
        }
        if (aspas) throw new IllegalArgumentException("Aspas sem fechamento");
        if (!campos.isEmpty() || campo.length() > 0 || fechado) {
            adicionarCampo(campos, campo);
            adicionarLinha(linhas, campos);
        }
        return List.copyOf(linhas);
    }

    private static void adicionarCampo(List<String> campos, StringBuilder campo) {
        if (campos.size() >= 100) throw new IllegalArgumentException("Muitas colunas");
        campos.add(campo.toString());
        campo.setLength(0);
    }

    private static void adicionarLinha(List<List<String>> linhas, List<String> campos) {
        if (linhas.size() >= 10_000) throw new IllegalArgumentException("Muitas linhas");
        linhas.add(List.copyOf(campos));
        campos.clear();
    }

    public static List<List<String>> lerRelatorio(String csv) {
        var linhas = ler(csv);
        if (linhas.isEmpty() || !linhas.get(0).equals(CABECALHO)) {
            throw new IllegalArgumentException("Cabeçalho de chamados inválido");
        }
        for (var linha : linhas) {
            if (linha.size() != CABECALHO.size()) {
                throw new IllegalArgumentException("Número incorreto de colunas");
            }
        }
        return linhas;
    }
}
