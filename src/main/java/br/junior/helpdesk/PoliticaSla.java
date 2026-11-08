package br.junior.helpdesk;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Calculates support deadlines in business days, excluding Saturdays, Sundays
 * and explicitly configured holidays. A zero-day SLA expires on the opening day.
 */
public final class PoliticaSla {
    private final Map<Chamado.Prioridade, Integer> dias;
    private final java.util.Set<LocalDate> feriados;

    public PoliticaSla(Map<Chamado.Prioridade, Integer> dias,
                       java.util.Set<LocalDate> feriados) {
        Objects.requireNonNull(dias, "Dias de SLA obrigatórios");
        Objects.requireNonNull(feriados, "Feriados obrigatórios");
        var copia = new EnumMap<Chamado.Prioridade, Integer>(Chamado.Prioridade.class);
        for (var prioridade : Chamado.Prioridade.values()) {
            var quantidade = dias.get(prioridade);
            if (quantidade == null || quantidade < 0 || quantidade > 365) {
                throw new IllegalArgumentException("SLA inválido para " + prioridade);
            }
            copia.put(prioridade, quantidade);
        }
        if (feriados.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Feriado nulo");
        this.dias = Map.copyOf(copia);
        this.feriados = java.util.Set.copyOf(feriados);
    }

    public static PoliticaSla padrao() {
        return new PoliticaSla(Map.of(
                Chamado.Prioridade.CRITICA, 0,
                Chamado.Prioridade.ALTA, 1,
                Chamado.Prioridade.MEDIA, 3,
                Chamado.Prioridade.BAIXA, 5), java.util.Set.of());
    }

    public int diasPara(Chamado.Prioridade prioridade) {
        return dias.get(Objects.requireNonNull(prioridade, "Prioridade obrigatória"));
    }

    public boolean diaUtil(LocalDate data) {
        Objects.requireNonNull(data, "Data obrigatória");
        return data.getDayOfWeek() != DayOfWeek.SATURDAY
                && data.getDayOfWeek() != DayOfWeek.SUNDAY
                && !feriados.contains(data);
    }

    public LocalDate proximoDiaUtil(LocalDate data) {
        Objects.requireNonNull(data, "Data obrigatória");
        var atual = data;
        while (!diaUtil(atual)) atual = atual.plusDays(1);
        return atual;
    }

    public LocalDate prazo(LocalDate abertura, Chamado.Prioridade prioridade) {
        Objects.requireNonNull(abertura, "Abertura obrigatória");
        var inicio = proximoDiaUtil(abertura);
        int restantes = diasPara(prioridade);
        while (restantes > 0) {
            inicio = inicio.plusDays(1);
            if (diaUtil(inicio)) restantes--;
        }
        return inicio;
    }

    public long diasUteisEntre(LocalDate inicio, LocalDate fim) {
        Objects.requireNonNull(inicio, "Início obrigatório");
        Objects.requireNonNull(fim, "Fim obrigatório");
        if (fim.isBefore(inicio)) return -diasUteisEntre(fim, inicio);
        long total = 0;
        for (var dia = inicio.plusDays(1); !dia.isAfter(fim); dia = dia.plusDays(1)) {
            if (diaUtil(dia)) total++;
        }
        return total;
    }

    public boolean expirado(LocalDate abertura, Chamado.Prioridade prioridade, LocalDate hoje) {
        Objects.requireNonNull(hoje, "Data obrigatória");
        return prazo(abertura, prioridade).isBefore(hoje);
    }

    public Map<Chamado.Prioridade, Integer> configuracao() {
        return dias;
    }

    public java.util.Set<LocalDate> feriados() {
        return feriados;
    }

    public PoliticaSla comFeriado(LocalDate feriado) {
        Objects.requireNonNull(feriado, "Feriado obrigatório");
        var novos = new java.util.HashSet<>(feriados);
        novos.add(feriado);
        return new PoliticaSla(dias, novos);
    }

    public PoliticaSla semFeriado(LocalDate feriado) {
        Objects.requireNonNull(feriado, "Feriado obrigatório");
        var novos = new java.util.HashSet<>(feriados);
        novos.remove(feriado);
        return new PoliticaSla(dias, novos);
    }

    public PoliticaSla comPrazo(Chamado.Prioridade prioridade, int diasUteis) {
        var novos = new EnumMap<Chamado.Prioridade, Integer>(Chamado.Prioridade.class);
        novos.putAll(dias);
        novos.put(Objects.requireNonNull(prioridade), diasUteis);
        return new PoliticaSla(novos, feriados);
    }
}
