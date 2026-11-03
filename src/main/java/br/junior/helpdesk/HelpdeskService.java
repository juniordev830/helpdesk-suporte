package br.junior.helpdesk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HelpdeskService {
    private final List<Chamado> chamados = new ArrayList<>();
    private int nextId = 1;

    public Chamado abrir(String titulo, Chamado.Prioridade prioridade, LocalDate prazo) {
        var chamado = Chamado.abrir(nextId++, titulo, prioridade, prazo);
        chamados.add(chamado);
        return chamado;
    }

    public Chamado atribuir(int id, String pessoa) {
        var index = indexOf(id);
        var atualizado = chamados.get(index).atribuir(pessoa);
        chamados.set(index, atualizado);
        return atualizado;
    }

    public Chamado resolver(int id) {
        var index = indexOf(id);
        var atualizado = chamados.get(index).resolver();
        chamados.set(index, atualizado);
        return atualizado;
    }

    public List<Chamado> vencidos(LocalDate hoje) {
        return Collections.unmodifiableList(chamados.stream().filter(c -> c.vencido(hoje)).toList());
    }

    /** Immutable operational snapshot for the selected day. */
    public record Painel(int abertos, int emAtendimento, int resolvidos, int vencidos) {}

    public Painel painel(LocalDate hoje) {
        java.util.Objects.requireNonNull(hoje, "Data obrigatória");
        int abertos = 0, emAtendimento = 0, resolvidos = 0, vencidos = 0;
        for (Chamado chamado : chamados) {
            switch (chamado.status()) {
                case ABERTO -> abertos++;
                case EM_ATENDIMENTO -> emAtendimento++;
                case RESOLVIDO -> resolvidos++;
            }
            if (chamado.vencido(hoje)) vencidos++;
        }
        return new Painel(abertos, emAtendimento, resolvidos, vencidos);
    }

    /**
     * Open tickets are sorted by overdue status, priority, due date and ID.
     * Priority is CRITICA > ALTA > MEDIA > BAIXA; resolved tickets are excluded.
     * This is a queue view only: it does not assign or mutate tickets.
     */
    public List<Chamado> filaAtendimento(LocalDate hoje) {
        java.util.Objects.requireNonNull(hoje, "Data obrigatória");
        var ordenacao = java.util.Comparator
                .comparing((Chamado c) -> !c.vencido(hoje))
                .thenComparingInt(c -> -c.prioridade().ordinal())
                .thenComparing(Chamado::prazo)
                .thenComparingInt(Chamado::id);
        return chamados.stream()
                .filter(c -> c.status() != Chamado.Status.RESOLVIDO)
                .sorted(ordenacao)
                .toList();
    }

    public List<Chamado> listar() {
        return Collections.unmodifiableList(chamados);
    }

    private int indexOf(int id) {
        var index = java.util.stream.IntStream.range(0, chamados.size())
                .filter(i -> chamados.get(i).id() == id).findFirst().orElse(-1);
        if (index < 0) throw new IllegalArgumentException("Chamado não encontrado: " + id);
        return index;
    }
}
