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
