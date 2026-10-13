package br.com.conde.justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    private static final List<Medidas.LinhaInjustica> INJUSTICA = Medidas.aInjustica();
    private static final List<Medidas.LinhaIdeal> IDEAL = Medidas.oIdeal();
    private static final List<Medidas.LinhaCrescimento> CRESCIMENTO = Medidas.oCrescimento();
    private static final List<Medidas.LinhaQuantum> QUANTUM = Medidas.oQuantum();
    private static final List<Medidas.LinhaPreco> PRECO = Medidas.oPreco();

    // ───────────────────── 1. a correcao ─────────────────────

    @Test
    @DisplayName("as 30 combinacoes servem todo pacote uma vez so, e nenhuma deixa a saida parada")
    void todasSaoValidas() {
        List<Medidas.LinhaCorrecao> linhas = Medidas.aCorrecao();

        assertThat(linhas).hasSize(30);

        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.valido()).as(l.cenario() + " / " + l.disciplina() + ": " + l.problema())
                    .isTrue();

            assertThat(l.pacotesServidos())
                    .as(l.cenario() + " / " + l.disciplina())
                    .isEqualTo(l.pacotesOferecidos());
        });
    }

    // ───────────────────── 2. o saturado ─────────────────────

    /**
     * A MEDIDA PRINCIPAL: a razao que o rodizio da e a razao dos tamanhos de pacote.
     *
     * <p>Nao aproximadamente: 15,00 para pacotes de 1.500 contra 100, e 37,50 para 1.500
     * contra 40. A disciplina esta funcionando perfeitamente, repartindo pacotes por igual,
     * e o resultado e esse.
     */
    @Test
    @DisplayName("o rodizio da exatamente a razao dos tamanhos: 15,00 e 37,50")
    void oRodizioDaARazaoDosTamanhos() {
        assertThat(injustica("grande x pequeno", "rodizio").razaoEntreOsDois())
                .as("1.500 sobre 100")
                .isCloseTo(15.00, within(0.01));

        assertThat(injustica("quatro tamanhos", "rodizio").razaoEntreOsDois())
                .as("1.500 sobre 40")
                .isCloseTo(37.50, within(0.01));

        assertThat(injustica("pesos cruzados", "rodizio").razaoEntreOsDois())
                .as("com os pesos contra os tamanhos, 1.500 vezes 4 sobre 40")
                .isCloseTo(150.00, within(0.01));
    }

    /**
     * E O CONSERTO VALE 0,0024 CONTRA 0,8752, nos mesmos pacotes.
     */
    @Test
    @DisplayName("o rodizio com deficit erra 0,2% onde o rodizio simples erra 88%")
    void oDeficitConserta() {
        assertThat(injustica("grande x pequeno", "rodizio").injustica())
                .isCloseTo(0.8752, within(0.0001));

        assertThat(injustica("grande x pequeno", "deficit 1500").injustica())
                .isCloseTo(0.0024, within(0.0001));

        assertThat(injustica("grande x pequeno", "deficit 1500").razaoEntreOsDois())
                .isCloseTo(1.00, within(0.01));

        assertThat(injustica("pesos 1234", "fila justa").injustica())
                .as("e com pacotes iguais e pesos diferentes a fila justa acerta na mosca")
                .isZero();
    }

    /**
     * O RODIZIO SO ERRA POR UM MOTIVO, e quando esse motivo some ele e exato.
     *
     * <p>No cenario do abusado todos os pacotes tem 500 bytes, e ali o rodizio bate com o
     * ideal byte por byte. O defeito dele nao e repartir mal: e contar a coisa errada.
     */
    @Test
    @DisplayName("com todos os pacotes do mesmo tamanho, o rodizio bate com o ideal byte a byte")
    void oRodizioAcertaComPacotesIguais() {
        for (int fluxo = 0; fluxo < 4; fluxo++)
            assertThat(ideal("um abusado", "rodizio", fluxo).atrasoEmBytes())
                    .as("fluxo " + fluxo)
                    .isCloseTo(0.0, within(0.5));
    }

    /**
     * E O PRIMEIRO QUE CHEGA REPARTE POR QUEM MANDOU MAIS, que nao e repartir.
     *
     * <p>No cenario do abusado, o fluxo 0 oferece dez vezes o que cada um dos outros
     * oferece. O ideal ja lhe da 320.000 bytes, porque o que os outros nao usam sobra para
     * ele; o primeiro que chega lhe da 384.500 e tira de todos os outros.
     */
    @Test
    @DisplayName("o primeiro que chega premia quem manda mais, mesmo alem do que o ideal ja da")
    void oPrimeiroQueChegaPremiaOAbusado() {
        assertThat(ideal("um abusado", "primeiro que chega", 0).bytesDoIdeal())
                .isCloseTo(320_000, within(1.0));

        assertThat(ideal("um abusado", "primeiro que chega", 0).bytesDaDisciplina())
                .isCloseTo(384_500, within(1.0));

        for (int fluxo = 1; fluxo < 4; fluxo++) {
            assertThat(ideal("um abusado", "primeiro que chega", fluxo).bytesDoIdeal())
                    .as("fluxo " + fluxo)
                    .isCloseTo(60_000, within(1.0));

            assertThat(ideal("um abusado", "primeiro que chega", fluxo).bytesDaDisciplina())
                    .as("fluxo " + fluxo)
                    .isCloseTo(38_500, within(1.0));
        }
    }

    private static Medidas.LinhaInjustica injustica(String cenario, String disciplina) {
        return INJUSTICA.stream()
                .filter(l -> l.cenario().equals(cenario) && l.disciplina().equals(disciplina))
                .findFirst()
                .orElseThrow();
    }

    private static Medidas.LinhaIdeal ideal(String cenario, String disciplina, int fluxo) {
        return IDEAL.stream()
                .filter(l -> l.cenario().equals(cenario) && l.disciplina().equals(disciplina)
                        && l.fluxo() == fluxo)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 3. o crescimento do atraso ─────────────────────

    /**
     * A TABELA QUE SEPARA AS DISCIPLINAS EM DUAS ESPECIES, e nao em graus.
     *
     * <p>Dobrando a janela de medida, o atraso do rodizio com deficit vai de 0,56 para 0,89
     * pacote e o da fila justa CAI de 0,49 para 0,16. O do rodizio simples vai de 67,56 para
     * 142,89: dobra junto com a janela.
     */
    @Test
    @DisplayName("o atraso das boas fica abaixo de um pacote e o das ruins dobra com a janela")
    void oAtrasoOuEhLimitadoOuCresce() {
        assertThat(crescimento("deficit 1500", 500).emPacotes()).isCloseTo(0.89, within(0.01));
        assertThat(crescimento("fila justa", 500).emPacotes()).isCloseTo(0.16, within(0.01));

        for (String disciplina : List.of("deficit 1500", "fila justa"))
            for (double duracao : new double[] {125, 250, 500})
                assertThat(crescimento(disciplina, duracao).emPacotes())
                        .as(disciplina + " em " + duracao)
                        .isLessThan(1.0);

        assertThat(crescimento("rodizio", 250).emPacotes()).isCloseTo(67.56, within(0.01));
        assertThat(crescimento("rodizio", 500).emPacotes()).isCloseTo(142.89, within(0.01));

        assertThat(crescimento("rodizio", 500).emPacotes())
                .as("dobrar a janela dobra o atraso dele")
                .isGreaterThan(crescimento("rodizio", 250).emPacotes() * 2);

        assertThat(crescimento("prioridade estrita", 500).emPacotes())
                .isCloseTo(221.89, within(0.01));
    }

    private static Medidas.LinhaCrescimento crescimento(String disciplina, double duracao) {
        return CRESCIMENTO.stream()
                .filter(l -> l.disciplina().equals(disciplina) && l.duracao() == duracao)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. o quantum ─────────────────────

    /**
     * A CORRECAO: a regra "o quantum tem que ser pelo menos o maior pacote" e sobre CUSTO,
     * e nao sobre justica. Em justica ela aponta para o lado contrario.
     *
     * <p>Quantum de 40 bytes, que e um trigesimo do maior pacote: injustica 0,0040.
     * Quantum de 15.000, que e dez vezes o maior pacote: injustica 0,0800. Vinte vezes pior.
     */
    @Test
    @DisplayName("o quantum pequeno e vinte vezes mais justo que o grande, e custa 3,9 passos")
    void oQuantumPequenoEhMaisJusto() {
        assertThat(quantum(40).injustica()).isCloseTo(0.0040, within(0.0001));
        assertThat(quantum(1_500).injustica()).isCloseTo(0.0080, within(0.0001));
        assertThat(quantum(15_000).injustica()).isCloseTo(0.0800, within(0.0001));

        assertThat(quantum(15_000).injustica() / quantum(40).injustica())
                .as("vinte vezes, e na direcao contraria a da regra de bolso")
                .isCloseTo(20.0, within(0.5));

        assertThat(quantum(40).passosPorPacote())
                .as("o preco do quantum pequeno e passo, e sao 3,9 por pacote")
                .isCloseTo(3.91, within(0.01));

        assertThat(quantum(1_500).passosPorPacote())
                .as("contra 1,08 quando o quantum cobre o maior pacote")
                .isCloseTo(1.08, within(0.01));

        assertThat(quantum(15_000).passosPorPacote()).isCloseTo(1.01, within(0.01));
    }

    @Test
    @DisplayName("a injustica cresce com o quantum e os passos caem, em toda a tabela")
    void osDoisEixosAndamAoContrario() {
        double injusticaAnterior = 0;
        double passosAnteriores = Double.MAX_VALUE;

        for (long valor : Medidas.QUANTA) {
            Medidas.LinhaQuantum linha = quantum(valor);

            assertThat(linha.injustica())
                    .as("quantum " + valor)
                    .isGreaterThanOrEqualTo(injusticaAnterior - 1e-9);

            assertThat(linha.passosPorPacote())
                    .as("quantum " + valor)
                    .isLessThanOrEqualTo(passosAnteriores + 1e-9);

            injusticaAnterior = linha.injustica();
            passosAnteriores = linha.passosPorPacote();
        }
    }

    private static Medidas.LinhaQuantum quantum(long valor) {
        return QUANTUM.stream()
                .filter(l -> l.quantum() == valor)
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. o preco ─────────────────────

    /**
     * A FILA JUSTA E TRES VEZES MAIS JUSTA E TREZE VEZES MAIS CARA.
     *
     * <p>E e por isso que o rodizio com deficit e o que esta dentro do equipamento: ele e
     * de passo praticamente constante, e a fila justa precisa de um minimo entre tudo o que
     * esta em fila.
     */
    @Test
    @DisplayName("a fila justa custa 14,3 passos por pacote e o deficit custa 1,07")
    void aFilaJustaEhCaraEOConsertoBarato() {
        assertThat(preco("fila justa").passosPorPacote()).isCloseTo(14.292, within(0.001));
        assertThat(preco("deficit 1500").passosPorPacote()).isCloseTo(1.066, within(0.001));

        assertThat(preco("fila justa").injusticaMedia()).isCloseTo(0.0045, within(0.0001));
        assertThat(preco("deficit 1500").injusticaMedia()).isCloseTo(0.0126, within(0.0001));

        assertThat(preco("fila justa").passosPorPacote() / preco("deficit 1500").passosPorPacote())
                .as("treze vezes mais cara")
                .isGreaterThan(13.0);

        assertThat(preco("deficit 1500").injusticaMedia() / preco("fila justa").injusticaMedia())
                .as("para ser tres vezes mais justa")
                .isCloseTo(2.8, within(0.1));
    }

    @Test
    @DisplayName("as tres disciplinas sem conceito de byte erram mais de duas vezes a parte devida")
    void asTresBobasErramMuito() {
        for (String disciplina : List.of("primeiro que chega", "rodizio", "prioridade estrita"))
            assertThat(preco(disciplina).injusticaMedia())
                    .as(disciplina)
                    .isGreaterThan(2.0);

        assertThat(preco("primeiro que chega").injusticaMedia())
                .as("com chegadas intercaladas o primeiro que chega VIRA o rodizio")
                .isCloseTo(preco("rodizio").injusticaMedia(), within(1e-9));

        assertThat(preco("prioridade estrita").injusticaMedia()).isCloseTo(5.4965, within(0.0001));
    }

    private static Medidas.LinhaPreco preco(String disciplina) {
        return PRECO.stream()
                .filter(l -> l.disciplina().equals(disciplina))
                .findFirst()
                .orElseThrow();
    }
}
