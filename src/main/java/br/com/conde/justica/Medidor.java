package br.com.conde.justica;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("saturado") || comando.equals("tudo")) saturado();
        if (comando.equals("ideal") || comando.equals("tudo")) ideal();
        if (comando.equals("quantum") || comando.equals("tudo")) quantum();
        if (comando.equals("preco") || comando.equals("tudo")) preco();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("TODA DISCIPLINA SERVE TODO PACOTE, UMA VEZ SO?");
        System.out.println();

        var linhas = Medidas.aCorrecao();
        long invalidas = linhas.stream().filter(l -> !l.valido()).count();

        System.out.printf("  %-18s %-20s %14s %14s %12s %10s %s%n",
                "cenario", "disciplina", "servidos", "oferecidos", "saida parada",
                "confere", "problema");

        for (var l : linhas)
            System.out.printf(Locale.ROOT, "  %-18s %-20s %14d %14d %12.2f %10s %s%n",
                    l.cenario(), l.disciplina(), l.pacotesServidos(), l.pacotesOferecidos(),
                    l.tempoOcioso(), l.valido() ? "sim" : "NAO", l.problema());

        System.out.println();
        System.out.printf("  %d combinacoes, %d invalidas.%n", linhas.size(), invalidas);
        System.out.println();
    }

    private static void saturado() {
        System.out.println();
        System.out.println("A REPARTICAO COM TODOS OS FLUXOS SEMPRE CHEIOS.");
        System.out.println("O ideal e conta fechada: peso sobre a soma dos pesos.");
        System.out.println();

        System.out.printf("  %-18s %-20s %6s %10s %8s %12s %12s %12s%n",
                "cenario", "disciplina", "fluxo", "pacote", "peso", "devido", "recebeu",
                "vezes o devido");

        for (var l : Medidas.oSaturado())
            System.out.printf(Locale.ROOT, "  %-18s %-20s %6d %10d %8.1f %12.4f %12.4f %12.3f%n",
                    l.cenario(), l.disciplina(), l.fluxo(), l.tamanho(), l.peso(),
                    l.parteDevida(), l.parteRecebida(), l.vezesODevido());

        System.out.println();
        System.out.println("  O RESUMO, por disciplina:");
        System.out.println();

        System.out.printf("  %-18s %-20s %14s %14s %14s %16s%n",
                "cenario", "disciplina", "injustica", "menor recebe", "maior recebe",
                "razao entre eles");

        for (var l : Medidas.aInjustica())
            System.out.printf(Locale.ROOT, "  %-18s %-20s %14.4f %14.4f %14.4f %16.2f%n",
                    l.cenario(), l.disciplina(), l.injustica(), l.piorFluxoRecebe(),
                    l.melhorFluxoRecebe(), l.razaoEntreOsDois());

        System.out.println();
    }

    private static void ideal() {
        System.out.println();
        System.out.println("O ATRASO CONTRA O COMPARTILHAMENTO IDEAL, com fluxos que ligam e desligam.");
        System.out.println("Aqui o ideal e simulado em fluido, porque o conjunto de ocupados muda.");
        System.out.println();

        System.out.printf("  %-16s %-20s %6s %14s %14s %14s %14s%n",
                "cenario", "disciplina", "fluxo", "ideal", "disciplina", "atraso bytes",
                "em pacotes");

        for (var l : Medidas.oIdeal())
            System.out.printf(Locale.ROOT, "  %-16s %-20s %6d %14.1f %14.1f %14.1f %14.2f%n",
                    l.cenario(), l.disciplina(), l.fluxo(), l.bytesDoIdeal(),
                    l.bytesDaDisciplina(), l.atrasoEmBytes(), l.atrasoEmPacotes());

        System.out.println();
        System.out.println("  E O MESMO ATRASO EM JANELAS CADA VEZ MAIORES:");
        System.out.println();

        System.out.printf("  %-20s %12s %16s %16s%n",
                "disciplina", "duracao", "maior atraso", "em pacotes");

        for (var l : Medidas.oCrescimento())
            System.out.printf(Locale.ROOT, "  %-20s %12.0f %16.1f %16.2f%n",
                    l.disciplina(), l.duracao(), l.maiorAtraso(), l.emPacotes());

        System.out.println();
    }

    private static void quantum() {
        System.out.println();
        System.out.println("O QUANTUM DO RODIZIO COM DEFICIT. O maior pacote do cenario tem "
                + Cenarios.MAIOR_PACOTE + " bytes.");
        System.out.println();

        System.out.printf("  %10s %18s %14s %18s %16s%n",
                "quantum", "vezes o maior", "injustica", "passos por pacote",
                "razao entre eles");

        for (var l : Medidas.oQuantum())
            System.out.printf(Locale.ROOT, "  %10d %18.3f %14.4f %18.2f %16.3f%n",
                    l.quantum(), l.vezesOMaiorPacote(), l.injustica(), l.passosPorPacote(),
                    l.razaoEntreOsDois());

        System.out.println();
    }

    private static void preco() {
        System.out.println();
        System.out.println("O PRECO DE DECIDIR, somando os quatro cenarios saturados.");
        System.out.println();

        System.out.printf("  %-20s %16s %14s %18s %18s%n",
                "disciplina", "passos", "pacotes", "passos por pacote", "injustica media");

        for (var l : Medidas.oPreco())
            System.out.printf(Locale.ROOT, "  %-20s %16d %14d %18.3f %18.4f%n",
                    l.disciplina(), l.passos(), l.pacotes(), l.passosPorPacote(),
                    l.injusticaMedia());

        System.out.println();
    }
}
