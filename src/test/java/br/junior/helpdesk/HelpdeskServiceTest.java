package br.junior.helpdesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class HelpdeskServiceTest {
    @Test
    void acompanhaResponsavelStatusEPrazo() {
        var service = new HelpdeskService();
        var chamado = service.abrir("VPN indisponível", Chamado.Prioridade.ALTA, LocalDate.of(2026, 11, 2));

        assertEquals(1, service.vencidos(LocalDate.of(2026, 11, 3)).size());
        service.atribuir(chamado.id(), "Ana");
        service.resolver(chamado.id());

        assertEquals(Chamado.Status.RESOLVIDO, service.listar().get(0).status());
        assertTrue(service.vencidos(LocalDate.of(2026, 11, 3)).isEmpty());
    }

    @Test
    void exigeResponsavelParaResolver() {
        var service = new HelpdeskService();
        var chamado = service.abrir("Acesso bloqueado", Chamado.Prioridade.MEDIA, LocalDate.of(2026, 11, 10));
        assertThrows(IllegalStateException.class, () -> service.resolver(chamado.id()));
    }
}
