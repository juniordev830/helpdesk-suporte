package br.junior.helpdesk;

import java.time.Instant;
import java.util.Objects;

/** Immutable audit record for a ticket transition or annotation. */
public record RegistroEvento(
        int chamadoId,
        Tipo tipo,
        String autor,
        String descricao,
        Instant instante) {
    public RegistroEvento {
        if (chamadoId <= 0) throw new IllegalArgumentException("Chamado inválido");
        Objects.requireNonNull(tipo, "Tipo obrigatório");
        Objects.requireNonNull(instante, "Instante obrigatório");
        autor = texto(autor, "Autor", 100);
        descricao = texto(descricao, "Descrição", 1000);
    }

    private static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank() || valor.trim().length() > limite) {
            throw new IllegalArgumentException(campo + " inválido");
        }
        return valor.trim();
    }

    public enum Tipo {
        ABERTURA, ATRIBUICAO, RESOLUCAO, NOTA, ETIQUETA_ADICIONADA,
        ETIQUETA_REMOVIDA, CATEGORIA_ALTERADA
    }

    public boolean realizadoPor(String usuario) {
        return autor.equalsIgnoreCase(Objects.requireNonNull(usuario).trim());
    }

    public boolean entre(Instant inicio, Instant fim) {
        Objects.requireNonNull(inicio);
        Objects.requireNonNull(fim);
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("Intervalo inválido");
        return !instante.isBefore(inicio) && !instante.isAfter(fim);
    }
}
