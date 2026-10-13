package br.com.conde.justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** As pecas, uma a uma, antes de qualquer medida. */
class JusticaTest {

    /**
     * Dois fluxos sempre cheios, com chegadas intercaladas.
     *
     * <p>Cada um recebe de uma vez 120% do que o enlace inteiro entrega na duracao toda.
     * Com menos que isso um deles esvazia no meio e a medida passa a ser sobre quem
     * ofereceu mais, que e outra pergunta.
     */
    private static Cenario doisFluxos(long tamanhoA, long tamanhoB, double pesoA, double pesoB,
            double duracao) {
        double capacidade = 1_000;
        double bytesPorFluxo = capacidade * duracao * 1.2;

        long quantosA = (long) Math.ceil(bytesPorFluxo / tamanhoA);
        long quantosB = (long) Math.ceil(bytesPorFluxo / tamanhoB);

        List<Pacote> pacotes = new ArrayList<>();

        for (long i = 0; i < Math.max(quantosA, quantosB); i++) {
            if (i < quantosA) pacotes.add(new Pacote(0, tamanhoA, 0));
            if (i < quantosB) pacotes.add(new Pacote(1, tamanhoB, 0));
        }

        return new Cenario("mao", new double[] {pesoA, pesoB}, pacotes, capacidade, duracao);
    }

    // ───────────────────── as pecas ─────────────────────

    @Nested
    @DisplayName("o pacote e o cenario")
    class AsPecas {

        @Test
        @DisplayName("um pacote de zero byte nao ocupa a saida, entao nao e pacote")
        void pacoteVazio() {
            assertThatThrownBy(() -> new Pacote(0, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pelo menos um byte");
        }

        @Test
        @DisplayName("um pacote de um fluxo que nao existe e erro de quem montou o cenario")
        void fluxoQueNaoExiste() {
            assertThatThrownBy(() -> new Cenario("torto", new double[] {1},
                    List.of(new Pacote(3, 100, 0)), 1_000, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("so ha 1");
        }

        @Test
        @DisplayName("peso zero nao e peso, porque dividir por ele seria a conta toda")
        void pesoZero() {
            assertThatThrownBy(() -> new Cenario("torto", new double[] {1, 0},
                    List.of(new Pacote(0, 100, 0)), 1_000, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("peso");
        }

        @Test
        @DisplayName("os pacotes ficam em ordem de chegada, qualquer que seja a ordem da lista")
        void ordemDeChegada() {
            Cenario cenario = new Cenario("fora de ordem", new double[] {1},
                    List.of(new Pacote(0, 100, 5), new Pacote(0, 200, 1),
                            new Pacote(0, 300, 3)),
                    1_000, 10);

            assertThat(cenario.pacotes()).extracting(Pacote::chegada)
                    .containsExactly(1.0, 3.0, 5.0);
        }

        @Test
        @DisplayName("a parte devida de cada fluxo e o peso sobre a soma dos pesos")
        void aParteDevida() {
            Cenario cenario = new Cenario("pesos", new double[] {1, 3},
                    List.of(new Pacote(0, 100, 0)), 1_000, 10);

            assertThat(cenario.bytesIdeaisSaturado(0)).isEqualTo(1_000 * 10 * 0.25);
            assertThat(cenario.bytesIdeaisSaturado(1)).isEqualTo(1_000 * 10 * 0.75);

            assertThat(cenario.bytesIdeaisSaturado(0) + cenario.bytesIdeaisSaturado(1))
                    .as("e as partes somam o enlace inteiro")
                    .isEqualTo(cenario.bytesDaSaida());
        }
    }

    // ───────────────────── o ideal ─────────────────────

    @Nested
    @DisplayName("o compartilhamento ideal")
    class OIdeal {

        /**
         * OS DOIS JUIZES CONCORDAM ONDE OS DOIS VALEM.
         *
         * <p>Com todos os fluxos sempre cheios, o ideal e conta fechada. A simulacao fluida
         * nao sabe disso: ela pula de evento em evento sem olhar nenhuma formula. As duas
         * tem que dar o mesmo numero, e esse e o teste que autoriza usar a simulacao nos
         * cenarios em que a conta fechada nao vale.
         */
        @Test
        @DisplayName("com todos os fluxos cheios, a simulacao fluida bate com a conta fechada")
        void osDoisJuizesConcordam() {
            for (Cenario cenario : Cenarios.saturados()) {
                double[] simulado = Ideal.bytesServidos(cenario);

                for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++)
                    assertThat(simulado[fluxo])
                            .as(cenario.nome() + " fluxo " + fluxo)
                            .isCloseTo(cenario.bytesIdeaisSaturado(fluxo),
                                    within(cenario.bytesIdeaisSaturado(fluxo) * 1e-9));
            }
        }

        @Test
        @DisplayName("o ideal reparte exatamente o que a saida entrega, nem mais nem menos")
        void oIdealNaoInventaBanda() {
            for (Cenario cenario : Cenarios.saturados()) {
                double soma = 0;

                for (double bytes : Ideal.bytesServidos(cenario)) soma += bytes;

                assertThat(soma).as(cenario.nome())
                        .isCloseTo(cenario.bytesDaSaida(), within(cenario.bytesDaSaida() * 1e-9));
            }
        }

        @Test
        @DisplayName("o ideal da o dobro a quem tem o dobro do peso, byte por byte")
        void oIdealEhProporcional() {
            Cenario cenario = Cenarios.pesosDiferentes();

            double[] ideal = Ideal.bytesServidos(cenario);

            for (int fluxo = 1; fluxo < cenario.fluxos(); fluxo++)
                assertThat(ideal[fluxo] / ideal[0])
                        .as("fluxo " + fluxo)
                        .isCloseTo(cenario.peso(fluxo) / cenario.peso(0), within(1e-9));
        }

        /**
         * O IDEAL E CONSERVATIVO: o que um fluxo vazio nao usa vai para os cheios.
         *
         * <p>Com o fluxo 1 calado na segunda metade, ele recebe menos da metade do enlace e
         * os outros recebem mais do que a parte nominal deles. A soma continua sendo o
         * enlace inteiro.
         */
        @Test
        @DisplayName("o que um fluxo calado nao usa e repartido entre os que falam")
        void oIdealDevolveASobra() {
            List<Pacote> pacotes = new ArrayList<>();

            /* O fluxo 0 tem com que encher o enlace por dez tiques. */
            for (int i = 0; i < 1_000; i++) pacotes.add(new Pacote(0, 100, 0));

            /* O fluxo 1 tem 2.000 bytes e mais nada. */
            for (int i = 0; i < 20; i++) pacotes.add(new Pacote(1, 100, 0));

            Cenario cenario = new Cenario("um cala", new double[] {1, 1}, pacotes, 1_000, 10);

            double[] ideal = Ideal.bytesServidos(cenario);

            assertThat(ideal[0] + ideal[1])
                    .as("a saida nunca ficou parada")
                    .isCloseTo(cenario.bytesDaSaida(), within(1.0));

            /* Dividindo ao meio, o fluxo 1 esvazia nos 2.000 bytes dele, aos quatro
             * tiques. Dali em diante o enlace inteiro vai para o fluxo 0. */
            assertThat(ideal[1]).as("ele levou os dois mil que tinha").isCloseTo(2_000, within(1.0));

            assertThat(ideal[0])
                    .as("e os oito mil restantes foram todos para quem continuou falando")
                    .isCloseTo(8_000, within(1.0));
        }
    }

    // ───────────────────── as disciplinas ─────────────────────

    @Nested
    @DisplayName("as disciplinas")
    class AsDisciplinas {

        /**
         * A MEDIDA PRINCIPAL, no menor cenario possivel: a razao e o tamanho do pacote.
         *
         * <p>Dois fluxos de peso igual, um com pacote de 1.500 bytes e outro de 100. O
         * rodizio da exatamente quinze para um, que e 1.500 sobre 100.
         */
        @Test
        @DisplayName("o rodizio da a razao dos tamanhos de pacote, exatamente")
        void oRodizioDaARazaoDosTamanhos() {
            Cenario cenario = doisFluxos(1_500, 100, 1, 1, 500);

            Contador contador = Saida.rodar(cenario, new RodizioPorPacote());

            assertThat(contador.pacotesDe(0))
                    .as("em pacotes ele e perfeitamente justo, e e essa a armadilha")
                    .isEqualTo(contador.pacotesDe(1));

            assertThat(contador.bytesDe(0) / contador.bytesDe(1))
                    .as("e em bytes ele da quinze para um")
                    .isCloseTo(15.0, within(0.01));
        }

        @Test
        @DisplayName("o rodizio com deficit da um para um nos mesmos pacotes")
        void oDeficitConserta() {
            Cenario cenario = doisFluxos(1_500, 100, 1, 1, 500);

            Contador contador = Saida.rodar(cenario, new RodizioComDeficit(1_500));

            assertThat(contador.bytesDe(0) / contador.bytesDe(1))
                    .isCloseTo(1.0, within(0.01));

            assertThat(contador.pacotesDe(1))
                    .as("e para isso ele manda quinze pacotes pequenos por grande")
                    .isGreaterThan(contador.pacotesDe(0) * 14);
        }

        @Test
        @DisplayName("o deficit guarda o que sobra: um fluxo de pacote grande nao morre de fome")
        void oDeficitGuardaOTroco() {
            /* Quantum de 1.000 e pacote de 1.400: so manda em volta sim, volta nao. */
            Cenario cenario = doisFluxos(1_400, 1_400, 1, 1, 200);

            Contador contador = Saida.rodar(cenario, new RodizioComDeficit(1_000));

            assertThat(contador.bytesDe(0))
                    .as("sem o deficit guardado, nenhum dos dois mandaria nada")
                    .isPositive();

            assertThat(contador.bytesDe(0) / contador.bytesDe(1)).isCloseTo(1.0, within(0.02));
        }

        @Test
        @DisplayName("a prioridade estrita deixa o fluxo de baixo com zero byte")
        void aPrioridadeMataOsDeBaixo() {
            Cenario cenario = doisFluxos(500, 500, 1, 1, 500);

            Contador contador = Saida.rodar(cenario, new PrioridadeEstrita());

            assertThat(contador.bytesDe(1))
                    .as("zero, e nenhum peso muda isso porque a regra nao tem peso dentro")
                    .isZero();

            assertThat(contador.bytesDe(0)).isEqualTo(cenario.bytesDaSaida());
        }

        @Test
        @DisplayName("a fila justa honra os pesos: o dobro do peso leva o dobro dos bytes")
        void aFilaJustaHonraOsPesos() {
            Cenario cenario = doisFluxos(500, 500, 1, 3, 500);

            Contador contador = Saida.rodar(cenario, new FilaJustaPonderada());

            assertThat(contador.bytesDe(1) / contador.bytesDe(0)).isCloseTo(3.0, within(0.02));
        }

        @Test
        @DisplayName("a fila justa nao premia o silencio: quem calou nao acumula credito")
        void aFilaJustaNaoPremiaOSilencio() {
            List<Pacote> pacotes = new ArrayList<>();

            /* O fluxo 0 fala o tempo todo; o fluxo 1 so chega na metade, de uma vez. */
            for (int i = 0; i < 2_000; i++) pacotes.add(new Pacote(0, 500, i * 0.005));
            for (int i = 0; i < 1_000; i++) pacotes.add(new Pacote(1, 500, 5));

            Cenario cenario = new Cenario("um chega tarde", new double[] {1, 1}, pacotes,
                    1_000, 10);

            Contador contador = Saida.rodar(cenario, new FilaJustaPonderada());

            double[] ideal = Ideal.bytesServidos(cenario);

            for (int fluxo = 0; fluxo < 2; fluxo++)
                assertThat(contador.bytesDe(fluxo))
                        .as("fluxo " + fluxo)
                        .isCloseTo(ideal[fluxo], within(Cenarios.MAIOR_PACOTE * 1.0));
        }

        @Test
        @DisplayName("um quantum pequeno demais para o pacote faz o ponteiro girar a toa")
        void quantumPequenoDemais() {
            assertThatThrownBy(() -> new RodizioComDeficit(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantum");
        }

        @Test
        @DisplayName("toda disciplina serve o mesmo total de bytes, a menos de um pacote")
        void todasServemOMesmoTotal() {
            for (Cenario cenario : Cenarios.saturados()) {
                double referencia = Saida.rodar(cenario, new RodizioComDeficit(1_500))
                        .bytesNoTotal();

                for (Disciplina disciplina : Juiz.todas())
                    assertThat(Saida.rodar(cenario, disciplina).bytesNoTotal())
                            .as(cenario.nome() + " / " + disciplina.nome())
                            .isCloseTo(referencia, within((double) Cenarios.MAIOR_PACOTE));
            }
        }
    }

    // ───────────────────── o juiz ─────────────────────

    @Nested
    @DisplayName("o juiz")
    class OJuiz {

        private final Cenario cenario = Cenarios.quatroTamanhos();

        @Test
        @DisplayName("o juiz pega uma disciplina que deixa um fluxo na fila para sempre")
        void pegaAQueEsquece() {
            Disciplina esquecida = new Disciplina() {
                private final PrioridadeEstrita dentro = new PrioridadeEstrita();

                @Override
                public String nome() {
                    return "esquece o fluxo 3";
                }

                @Override
                public void comecar(Cenario qual) {
                    dentro.comecar(qual);
                }

                @Override
                public void chegou(Pacote pacote, double agora) {
                    /* O fluxo 3 e simplesmente jogado fora. */
                    if (pacote.fluxo() != 3) dentro.chegou(pacote, agora);
                }

                @Override
                public Pacote proximo(double agora, Contador contador) {
                    return dentro.proximo(agora, contador);
                }

                @Override
                public boolean temAlgo() {
                    return dentro.temAlgo();
                }
            };

            Juiz.Veredito veredito = Juiz.julgar(esquecida, cenario);

            assertThat(veredito.valido()).isFalse();
            assertThat(veredito.problema()).contains("o fluxo 3 ofereceu");
        }

        @Test
        @DisplayName("o juiz pega uma disciplina que serve o mesmo pacote duas vezes")
        void pegaAQueRepete() {
            Disciplina repetidora = new Disciplina() {
                private final java.util.ArrayDeque<Pacote> fila = new java.util.ArrayDeque<>();
                private Pacote ultimo;
                private boolean jaRepetiu;

                @Override
                public String nome() {
                    return "repete o primeiro";
                }

                @Override
                public void comecar(Cenario qual) {
                    fila.clear();
                    ultimo = null;
                    jaRepetiu = false;
                }

                @Override
                public void chegou(Pacote pacote, double agora) {
                    fila.addLast(pacote);
                }

                @Override
                public Pacote proximo(double agora, Contador contador) {
                    contador.passo();

                    if (ultimo != null && !jaRepetiu) {
                        jaRepetiu = true;
                        return ultimo;
                    }

                    ultimo = fila.pollFirst();

                    return ultimo;
                }

                @Override
                public boolean temAlgo() {
                    return !fila.isEmpty();
                }
            };

            Juiz.Veredito veredito = Juiz.julgar(repetidora, cenario);

            assertThat(veredito.valido()).isFalse();
            assertThat(veredito.problema()).contains("ofereceu");
        }

        @Test
        @DisplayName("o juiz nao olha justica: a prioridade estrita passa com fluxos zerados")
        void oJuizNaoOlhaJustica() {
            Juiz.Veredito veredito = Juiz.julgar(new PrioridadeEstrita(), cenario);

            assertThat(veredito.valido()).isTrue();

            Contador contador = Saida.rodar(cenario, new PrioridadeEstrita());

            assertThat(contador.bytesDe(3))
                    .as("e no cenario de medida ela deixa o fluxo 3 com zero")
                    .isZero();
        }

        @Test
        @DisplayName("as cinco disciplinas sao validas nos seis cenarios")
        void todasSaoValidas() {
            for (Cenario qual : Cenarios.todos())
                for (Disciplina disciplina : Juiz.todas()) {
                    Juiz.Veredito veredito = Juiz.julgar(disciplina, qual);

                    assertThat(veredito.valido())
                            .as(qual.nome() + " / " + disciplina.nome() + ": "
                                    + veredito.problema())
                            .isTrue();
                }
        }

        @Test
        @DisplayName("nenhuma disciplina deixa a saida parada tendo pacote em fila")
        void todasSaoConservativas() {
            for (Cenario qual : Cenarios.saturados())
                for (Disciplina disciplina : Juiz.todas()) {
                    Contador contador = Saida.rodar(qual, disciplina);

                    assertThat(contador.tempoOcioso())
                            .as(qual.nome() + " / " + disciplina.nome())
                            .isZero();

                    assertThat(contador.bytesNoTotal())
                            .as(qual.nome() + " / " + disciplina.nome())
                            .isGreaterThan(qual.bytesDaSaida() * 0.99);
                }
        }
    }
}
