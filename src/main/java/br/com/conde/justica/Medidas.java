package br.com.conde.justica;

import java.util.ArrayList;
import java.util.List;

/**
 * AS CINCO TABELAS que o medidor imprime e que os testes conferem numero por numero.
 *
 * <p>Tudo aqui e contado, nunca cronometrado: bytes servidos, pacotes servidos e passos de
 * decisao. O tempo dos cenarios e em tiques e e exato, entao os mesmos numeros saem em
 * qualquer sistema.
 */
public final class Medidas {

    private Medidas() {}

    private static final java.util.Map<String, Contador> RODADAS = new java.util.HashMap<>();

    /** Roda uma disciplina num cenario, guardando o resultado. */
    public static synchronized Contador rodar(Cenario cenario, Disciplina disciplina) {
        return RODADAS.computeIfAbsent(cenario.nome() + " | " + disciplina.nome(),
                chave -> Saida.rodar(cenario, disciplina));
    }

    private static final java.util.Map<String, double[]> IDEAIS = new java.util.HashMap<>();

    public static synchronized double[] ideal(Cenario cenario) {
        return IDEAIS.computeIfAbsent(cenario.nome(), chave -> Ideal.bytesServidos(cenario));
    }

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String cenario, String disciplina, long pacotesServidos,
            long pacotesOferecidos, double tempoOcioso, boolean valido, String problema) {}

    /** Toda disciplina serve todo pacote, uma vez so, e nunca deixa a saida parada a toa. */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> saida = new ArrayList<>();

        for (Cenario cenario : Cenarios.todos())
            for (Disciplina disciplina : Juiz.todas()) {
                Juiz.Veredito veredito = Juiz.julgar(disciplina, cenario);

                saida.add(new LinhaCorrecao(cenario.nome(), disciplina.nome(),
                        veredito.pacotesServidos(), veredito.pacotesOferecidos(),
                        veredito.tempoOcioso(), veredito.valido(), veredito.problema()));
            }

        return saida;
    }

    // ───────────────────── 2. o saturado ─────────────────────

    public record LinhaSaturado(String cenario, String disciplina, int fluxo, long tamanho,
            double peso, double parteDevida, double parteRecebida, double vezesODevido) {}

    /** A reparticao com todos os fluxos sempre cheios, fluxo por fluxo. */
    public static List<LinhaSaturado> oSaturado() {
        List<LinhaSaturado> saida = new ArrayList<>();

        for (Cenario cenario : Cenarios.saturados()) {
            long[] tamanhos = tamanhosDe(cenario);

            for (Disciplina disciplina : Juiz.todas()) {
                Contador contador = rodar(cenario, disciplina);

                double total = contador.bytesNoTotal();

                for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++) {
                    double devida = cenario.peso(fluxo) / cenario.somaDosPesos();
                    double recebida = total == 0 ? 0 : contador.bytesDe(fluxo) / total;

                    saida.add(new LinhaSaturado(cenario.nome(), disciplina.nome(), fluxo,
                            tamanhos[fluxo], cenario.peso(fluxo), devida, recebida,
                            devida == 0 ? 0 : recebida / devida));
                }
            }
        }

        return saida;
    }

    public record LinhaInjustica(String cenario, String disciplina, double injustica,
            double piorFluxoRecebe, double melhorFluxoRecebe, double razaoEntreOsDois) {}

    /** O resumo da tabela saturada: o maior erro de reparticao de cada disciplina. */
    public static List<LinhaInjustica> aInjustica() {
        List<LinhaInjustica> saida = new ArrayList<>();

        for (Cenario cenario : Cenarios.saturados())
            for (Disciplina disciplina : Juiz.todas()) {
                Contador contador = rodar(cenario, disciplina);

                double[] ideal = new double[cenario.fluxos()];

                for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++)
                    ideal[fluxo] = cenario.bytesIdeaisSaturado(fluxo);

                double menor = Double.POSITIVE_INFINITY;
                double maior = 0;

                for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++) {
                    double devida = cenario.peso(fluxo) / cenario.somaDosPesos();
                    double razao = contador.bytesDe(fluxo) / cenario.bytesDaSaida() / devida;

                    menor = Math.min(menor, razao);
                    maior = Math.max(maior, razao);
                }

                saida.add(new LinhaInjustica(cenario.nome(), disciplina.nome(),
                        Juiz.injustica(cenario, contador, ideal), menor, maior,
                        menor <= 0 ? Double.POSITIVE_INFINITY : maior / menor));
            }

        return saida;
    }

    // ───────────────────── 3. o ideal ─────────────────────

    public record LinhaIdeal(String cenario, String disciplina, int fluxo, double bytesDoIdeal,
            double bytesDaDisciplina, double atrasoEmBytes, double atrasoEmPacotes) {}

    /**
     * O atraso contra o ideal, no cenario em que os fluxos ligam e desligam.
     *
     * <p>Aqui o ideal nao e conta fechada: o conjunto de fluxos ocupados muda ao longo do
     * tempo, e so a simulacao fluida sabe quanto cabia a cada um em cada pedaco.
     */
    public static List<LinhaIdeal> oIdeal() {
        List<LinhaIdeal> saida = new ArrayList<>();

        for (Cenario cenario : List.of(Cenarios.umAbusado(), Cenarios.ligaDesliga())) {
            double[] ideal = ideal(cenario);

            for (Disciplina disciplina : Juiz.todas()) {
                Contador contador = rodar(cenario, disciplina);

                for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++) {
                    double atraso = contador.bytesDe(fluxo) - ideal[fluxo];

                    saida.add(new LinhaIdeal(cenario.nome(), disciplina.nome(), fluxo,
                            ideal[fluxo], contador.bytesDe(fluxo), atraso,
                            atraso / Cenarios.MAIOR_PACOTE));
                }
            }
        }

        return saida;
    }

    public record LinhaCrescimento(String disciplina, double duracao, double maiorAtraso,
            double emPacotes) {}

    /**
     * O MAIOR ATRASO CONTRA O IDEAL, medido em janelas cada vez mais longas.
     *
     * <p>Esta e a tabela que separa as disciplinas em duas especies, e nao em graus. Numa
     * disciplina boa o atraso e LIMITADO: ele para de crescer, por mais que a janela
     * cresca, porque o que a atrasa e o pacote que ja estava sendo servido e nada mais.
     * Numa ruim ele cresce com o tempo, porque ela entrega a fracao errada da saida e o
     * erro se acumula.
     *
     * <p>E a conta do teorema de Parekh e Gallager, de 1993: uma fila justa ponderada fica
     * atras do ideal por no maximo um pacote maximo.
     */
    public static List<LinhaCrescimento> oCrescimento() {
        List<LinhaCrescimento> saida = new ArrayList<>();

        Cenario cheio = Cenarios.ligaDesliga();

        for (Disciplina disciplina : Juiz.todas())
            for (double fracao : new double[] {0.25, 0.5, 1.0}) {
                double duracao = Cenarios.DURACAO * fracao;

                Cenario janela = new Cenario(cheio.nome() + " " + fracao, Juiz.pesosDe(cheio),
                        cheio.pacotes(), cheio.capacidade(), duracao);

                double[] ideal = Ideal.bytesServidos(janela);

                Contador contador = Saida.rodar(janela, disciplina);

                double maior = Juiz.maiorAtraso(janela, contador, ideal);

                saida.add(new LinhaCrescimento(disciplina.nome(), duracao, maior,
                        maior / Cenarios.MAIOR_PACOTE));
            }

        return saida;
    }

    // ───────────────────── 4. o quantum ─────────────────────

    /** Os quanta testados, do bem abaixo do maior pacote ao bem acima. */
    public static final long[] QUANTA = {40, 150, 500, 1_000, 1_500, 3_000, 15_000};

    public record LinhaQuantum(long quantum, double vezesOMaiorPacote, double injustica,
            double passosPorPacote, double razaoEntreOsDois) {}

    /** O unico parametro do repositorio, e o que acontece abaixo do piso dele. */
    public static List<LinhaQuantum> oQuantum() {
        List<LinhaQuantum> saida = new ArrayList<>();

        Cenario cenario = Cenarios.quatroTamanhos();

        double[] ideal = new double[cenario.fluxos()];

        for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++)
            ideal[fluxo] = cenario.bytesIdeaisSaturado(fluxo);

        for (long quantum : QUANTA) {
            RodizioComDeficit disciplina = new RodizioComDeficit(quantum);

            Contador contador = Saida.rodar(cenario, disciplina);

            double menor = Double.POSITIVE_INFINITY;
            double maior = 0;

            for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++) {
                double razao = contador.bytesDe(fluxo) / ideal[fluxo];

                menor = Math.min(menor, razao);
                maior = Math.max(maior, razao);
            }

            saida.add(new LinhaQuantum(quantum,
                    (double) quantum / Cenarios.MAIOR_PACOTE,
                    Juiz.injustica(cenario, contador, ideal),
                    (double) contador.passosDeDecisao() / contador.pacotesNoTotal(),
                    menor <= 0 ? Double.POSITIVE_INFINITY : maior / menor));
        }

        return saida;
    }

    // ───────────────────── 5. o preco ─────────────────────

    public record LinhaPreco(String disciplina, long passos, long pacotes,
            double passosPorPacote, double injusticaMedia) {}

    /** Quantos candidatos cada disciplina olha para escolher quem manda. */
    public static List<LinhaPreco> oPreco() {
        List<LinhaPreco> saida = new ArrayList<>();

        for (Disciplina disciplina : Juiz.todas()) {
            long passos = 0;
            long pacotes = 0;

            double soma = 0;
            int quantos = 0;

            for (Cenario cenario : Cenarios.saturados()) {
                Contador contador = rodar(cenario, disciplina);

                passos += contador.passosDeDecisao();
                pacotes += contador.pacotesNoTotal();

                double[] ideal = new double[cenario.fluxos()];

                for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++)
                    ideal[fluxo] = cenario.bytesIdeaisSaturado(fluxo);

                soma += Juiz.injustica(cenario, contador, ideal);
                quantos++;
            }

            saida.add(new LinhaPreco(disciplina.nome(), passos, pacotes,
                    (double) passos / pacotes, soma / quantos));
        }

        return saida;
    }

    static long[] tamanhosDe(Cenario cenario) {
        long[] tamanhos = new long[cenario.fluxos()];

        for (Pacote pacote : cenario.pacotes()) tamanhos[pacote.fluxo()] = pacote.bytes();

        return tamanhos;
    }
}
