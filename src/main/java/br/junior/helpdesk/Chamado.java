package br.junior.helpdesk;

import java.time.LocalDate;
import java.util.Objects;

public record Chamado(
        int id,
        String titulo,
        Prioridade prioridade,
        String responsavel,
        LocalDate prazo,
        Status status) {
    public Chamado {
        if (id <= 0) throw new IllegalArgumentException("Id deve ser positivo");
        if (titulo == null || titulo.isBlank()) throw new IllegalArgumentException("Título obrigatório");
        Objects.requireNonNull(prioridade, "Prioridade obrigatória");
        Objects.requireNonNull(prazo, "Prazo obrigatório");
        Objects.requireNonNull(status, "Status obrigatório");
        titulo = titulo.trim();
        if (responsavel != null && responsavel.isBlank()) responsavel = null;
    }

    public static Chamado abrir(int id, String titulo, Prioridade prioridade, LocalDate prazo) {
        return new Chamado(id, titulo, prioridade, null, prazo, Status.ABERTO);
    }

    public Chamado atribuir(String pessoa) {
        if (pessoa == null || pessoa.isBlank()) throw new IllegalArgumentException("Responsável obrigatório");
        if (status == Status.RESOLVIDO) throw new IllegalStateException("Chamado resolvido não pode ser atribuído");
        return new Chamado(id, titulo, prioridade, pessoa.trim(), prazo, Status.EM_ATENDIMENTO);
    }

    public Chamado resolver() {
        if (status == Status.RESOLVIDO) return this;
        if (responsavel == null) throw new IllegalStateException("Atribua um responsável antes de resolver");
        return new Chamado(id, titulo, prioridade, responsavel, prazo, Status.RESOLVIDO);
    }

    public boolean vencido(LocalDate hoje) {
        Objects.requireNonNull(hoje, "Data obrigatória");
        return status != Status.RESOLVIDO && prazo.isBefore(hoje);
    }

    public enum Prioridade { BAIXA, MEDIA, ALTA, CRITICA }
    public enum Status { ABERTO, EM_ATENDIMENTO, RESOLVIDO }
}
