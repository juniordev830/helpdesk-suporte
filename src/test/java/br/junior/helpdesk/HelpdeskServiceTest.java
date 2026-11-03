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

    @Test
    void ordenaFilaPorAtrasoPrioridadePrazoEId() {
        var service = new HelpdeskService();
        LocalDate hoje = LocalDate.of(2026, 11, 3);
        var normal = service.abrir("Solicitação", Chamado.Prioridade.BAIXA, hoje.plusDays(1));
        var critica = service.abrir("Produção indisponível", Chamado.Prioridade.CRITICA, hoje.plusDays(2));
        var vencidaAlta = service.abrir("Integração", Chamado.Prioridade.ALTA, hoje.minusDays(1));
        var vencidaCritica = service.abrir("Serviço parado", Chamado.Prioridade.CRITICA, hoje.minusDays(2));
        var vencidaCritica2 = service.abrir("Rede parada", Chamado.Prioridade.CRITICA, hoje.minusDays(1));
        service.atribuir(normal.id(), "Maria");
        service.resolver(normal.id());

        var fila = service.filaAtendimento(hoje);
        assertEquals(java.util.List.of(vencidaCritica.id(), vencidaCritica2.id(),
                vencidaAlta.id(), critica.id()),
                fila.stream().map(Chamado::id).toList());
        assertThrows(UnsupportedOperationException.class, fila::clear);
        assertEquals(4, fila.size());
        assertEquals(1, service.painel(hoje).resolvidos());
    }

    @Test
    void painelContaStatusEVencidosSemMarcarResolvidos() {
        var service = new HelpdeskService();
        LocalDate hoje = LocalDate.of(2026, 11, 3);
        var atrasado = service.abrir("Chamado antigo", Chamado.Prioridade.BAIXA, hoje.minusDays(1));
        var emDia = service.abrir("Chamado atual", Chamado.Prioridade.MEDIA, hoje);
        var resolvido = service.abrir("Resolvido", Chamado.Prioridade.CRITICA, hoje.minusDays(5));
        service.atribuir(emDia.id(), "Ana");
        service.atribuir(resolvido.id(), "Bia");
        service.resolver(resolvido.id());

        assertEquals(new HelpdeskService.Painel(1, 1, 1, 1), service.painel(hoje));
        assertEquals(java.util.List.of(atrasado.id(), emDia.id()),
                service.filaAtendimento(hoje).stream().map(Chamado::id).toList());
        assertThrows(NullPointerException.class, () -> service.painel(null));
        assertThrows(NullPointerException.class, () -> service.filaAtendimento(null));
        assertEquals(3, service.listar().size());
    }

}
