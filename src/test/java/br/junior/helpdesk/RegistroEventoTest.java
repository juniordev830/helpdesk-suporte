package br.junior.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RegistroEventoTest {
    private static final Instant AGORA = Instant.parse("2026-11-08T12:00:00Z");

    @Test
    void validaIdentidadeEAutor() {
        var evento = new RegistroEvento(1, RegistroEvento.Tipo.NOTA, " Ana ", " Atualizado ", AGORA);
        assertEquals("Ana", evento.autor());
        assertEquals("Atualizado", evento.descricao());
        assertTrue(evento.realizadoPor("ana"));
        assertFalse(evento.realizadoPor("Bruno"));
    }

    @Test
    void filtraIntervaloInclusivo() {
        var evento = new RegistroEvento(2, RegistroEvento.Tipo.ABERTURA, "Ana", "Aberto", AGORA);
        assertTrue(evento.entre(AGORA, AGORA));
        assertFalse(evento.entre(AGORA.plusSeconds(1), AGORA.plusSeconds(2)));
        assertThrows(IllegalArgumentException.class,
                () -> evento.entre(AGORA.plusSeconds(1), AGORA));
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroEvento(0, RegistroEvento.Tipo.NOTA, "Ana", "ok", AGORA));
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroEvento(1, RegistroEvento.Tipo.NOTA, " ", "ok", AGORA));
        assertThrows(IllegalArgumentException.class, () ->
                new RegistroEvento(1, RegistroEvento.Tipo.NOTA, "Ana", "x".repeat(1001), AGORA));
        assertThrows(NullPointerException.class, () ->
                new RegistroEvento(1, RegistroEvento.Tipo.NOTA, "Ana", "ok", null));
    }
}
