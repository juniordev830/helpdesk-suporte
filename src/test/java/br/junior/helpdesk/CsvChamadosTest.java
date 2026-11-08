package br.junior.helpdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvChamadosTest {
    @Test
    void exportaCabecalhoMesmoSemChamados() {
        var csv = CsvChamados.exportar(List.of());
        assertTrue(csv.startsWith("id,titulo,prioridade"));
        assertEquals(1, CsvChamados.lerRelatorio(csv).size());
    }

    @Test
    void trataAspasVirgulasEQuebrasDeLinha() {
        var dados = List.of("Ana, Maria", "Ele disse \"oi\"", "linha 1\nlinha 2", "");
        var csv = CsvChamados.linha(dados);
        assertEquals(List.of(dados), CsvChamados.ler(csv));
    }

    @Test
    void neutralizaFormulasPerigosas() {
        assertEquals("'=SUM(1)", CsvChamados.escapar("=SUM(1)"));
        assertEquals("'+123", CsvChamados.escapar("+123"));
        assertEquals("'-2", CsvChamados.escapar("-2"));
        assertEquals("'@x", CsvChamados.escapar("@x"));
        assertEquals("'  =x", CsvChamados.escapar("  =x"));
        assertEquals("normal", CsvChamados.escapar("normal"));
    }

    @Test
    void interpretaQuebraDeLinhaWindowsEUnix() {
        var csv = "a,b\r\nc,d\ne,f\r";
        assertEquals(List.of(List.of("a", "b"), List.of("c", "d"), List.of("e", "f")),
                CsvChamados.ler(csv));
    }

    @Test
    void preservaCampoVazioNoFinal() {
        assertEquals(List.of(List.of("a", "")), CsvChamados.ler("a,"));
        assertEquals(List.of(List.of("", "b")), CsvChamados.ler(",b"));
        assertEquals(List.of(List.of("", "")), CsvChamados.ler(","));
    }

    @Test
    void rejeitaAspasMalformadas() {
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.ler("\"ab"));
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.ler("ab\"cd"));
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.ler("\"ab\"cd"));
    }

    @Test
    void rejeitaCabecalhoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.lerRelatorio("a,b\r\n1,2"));
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.lerRelatorio(""));
        assertThrows(IllegalArgumentException.class, () ->
                CsvChamados.lerRelatorio(String.join(",", CsvChamados.CABECALHO) + "\r\n1,2\r\n"));
    }

    @Test
    void rejeitaCsvMuitoGrande() {
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.ler("x".repeat(2_000_001)));
    }

    @Test
    void rejeitaMaisDeCemColunas() {
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.ler("a,".repeat(101)));
    }

    @Test
    void rejeitaCampoExcessivamenteLongo() {
        assertThrows(IllegalArgumentException.class, () -> CsvChamados.ler("x".repeat(100_001)));
    }

    @Test
    void rejeitaCamposNulos() {
        assertThrows(NullPointerException.class, () -> CsvChamados.escapar(null));
        assertThrows(NullPointerException.class, () -> CsvChamados.ler(null));
        assertThrows(NullPointerException.class, () -> CsvChamados.exportar(null));
    }
}
