package br.com.conde.justica;

import java.util.ArrayList;
import java.util.List;

/**
 * OS CENARIOS, que sao a metade que importa.
 *
 * <p>Nao existe "o quanto uma disciplina e justa". Existe o quanto ela e justa naquele
 * trafego, e os cenarios daqui nao foram escolhidos para serem neutros: cada um e o melhor
 * caso de alguma disciplina e o pior de outra.
 *
 * <h2>Saturado quer dizer uma coisa exata</h2>
 *
 * <p>Nos cenarios saturados, cada fluxo recebe de uma vez, no instante zero, mais bytes do
 * que o enlace inteiro consegue entregar na duracao toda. Entao nenhum fluxo fica vazio em
 * instante nenhum, e o ideal vira uma conta fechada: capacidade vezes duracao vezes peso
 * sobre a soma dos pesos.
 *
 * <p>Isso importa porque e o que torna o juiz do cenario principal um juiz FECHADO, que
 * nao depende de nenhuma simulacao. A simulacao do ideal existe para os cenarios em que os
 * fluxos ligam e desligam, e ha um teste que exige que as duas concordem onde as duas
 * valem.
 */
public final class Cenarios {

    private Cenarios() {}

    public static final double CAPACIDADE = 1_000;
    public static final double DURACAO = 500;

    /** O maior pacote dos cenarios daqui, que e o piso do quantum. */
    public static final long MAIOR_PACOTE = 1_500;

    /**
     * Um cenario em que todos os fluxos estao sempre cheios.
     *
     * <p>Cada fluxo recebe, no instante zero, 120% do que o enlace inteiro entrega na
     * duracao toda. E de proposito que seja o enlace inteiro e nao a parte dele: assim o
     * fluxo continua cheio mesmo que a disciplina resolva dar tudo a ele.
     *
     * <p>As chegadas sao INTERCALADAS entre os fluxos, um pacote de cada por vez. Isso
     * existe por causa de uma disciplina so: o primeiro que chega nao tem comportamento
     * proprio, ele reproduz a ordem em que o trafego chegou. Despejando o fluxo zero
     * inteiro antes do fluxo um, ele viraria prioridade estrita e a tabela estaria medindo
     * a minha ordem de geracao, nao a disciplina.
     */
    public static Cenario saturado(String nome, double[] pesos, long[] tamanhos) {
        if (pesos.length != tamanhos.length)
            throw new IllegalArgumentException("um tamanho de pacote por fluxo");

        double bytesPorFluxo = CAPACIDADE * DURACAO * 1.2;

        long[] quantos = new long[pesos.length];
        long maior = 0;

        for (int fluxo = 0; fluxo < pesos.length; fluxo++) {
            quantos[fluxo] = (long) Math.ceil(bytesPorFluxo / tamanhos[fluxo]);
            maior = Math.max(maior, quantos[fluxo]);
        }

        List<Pacote> pacotes = new ArrayList<>();

        for (long i = 0; i < maior; i++)
            for (int fluxo = 0; fluxo < pesos.length; fluxo++)
                if (i < quantos[fluxo]) pacotes.add(new Pacote(fluxo, tamanhos[fluxo], 0));

        return new Cenario(nome, pesos, pacotes, CAPACIDADE, DURACAO);
    }

    /** Dois fluxos de peso igual, um com pacote grande e outro com pacote pequeno. */
    public static Cenario grandeContraPequeno() {
        return saturado("grande x pequeno", new double[] {1, 1}, new long[] {1_500, 100});
    }

    /** Quatro fluxos de peso igual, com os quatro tamanhos tipicos de um enlace. */
    public static Cenario quatroTamanhos() {
        return saturado("quatro tamanhos", new double[] {1, 1, 1, 1},
                new long[] {1_500, 500, 150, 40});
    }

    /** Quatro fluxos de pesos diferentes e pacotes do mesmo tamanho. */
    public static Cenario pesosDiferentes() {
        return saturado("pesos 1234", new double[] {1, 2, 3, 4},
                new long[] {500, 500, 500, 500});
    }

    /** Pesos e tamanhos cruzados: quem tem mais peso manda pacote menor. */
    public static Cenario pesosCruzados() {
        return saturado("pesos cruzados", new double[] {1, 2, 3, 4},
                new long[] {1_500, 500, 150, 40});
    }

    /**
     * Quatro fluxos iguais em tudo, mas um deles manda dez vezes mais pacotes.
     *
     * <p>E o cenario contra o primeiro que chega: ali o abusado leva dez vezes a banda dos
     * outros sem violar regra nenhuma, porque a disciplina nao tem o conceito de fluxo.
     * Nas outras quatro, ele leva exatamente o mesmo que os demais.
     */
    public static Cenario umAbusado() {
        List<Pacote> pacotes = new ArrayList<>();

        double bytesDoEnlace = CAPACIDADE * DURACAO;

        /* O abusado oferece 1,2 do enlace inteiro; cada um dos outros, um decimo disso. */
        for (int fluxo = 0; fluxo < 4; fluxo++) {
            double oferta = fluxo == 0 ? bytesDoEnlace * 1.2 : bytesDoEnlace * 0.12;

            long quantos = (long) Math.ceil(oferta / 500);

            double intervalo = DURACAO / quantos;

            for (long i = 0; i < quantos; i++)
                pacotes.add(new Pacote(fluxo, 500, Math.min(DURACAO - 1e-6, i * intervalo)));
        }

        return new Cenario("um abusado", new double[] {1, 1, 1, 1}, pacotes, CAPACIDADE, DURACAO);
    }

    /**
     * Quatro fluxos que ligam e desligam em janelas diferentes.
     *
     * <p>E o cenario em que o ideal deixa de ser uma conta fechada: o conjunto de fluxos
     * com o que mandar muda ao longo do tempo, e a parte de quem esta ligado cresce quando
     * os outros calam. Uma disciplina que nao devolve essa sobra esta desperdicando enlace;
     * uma que devolve demais a quem ficou quieto esta premiando o silencio.
     */
    public static Cenario ligaDesliga() {
        List<Pacote> pacotes = new ArrayList<>();

        double[][] janelas = {
                {0, DURACAO},
                {0, DURACAO / 2},
                {DURACAO / 4, DURACAO},
                {DURACAO / 2, DURACAO * 3 / 4}};

        long[] tamanhos = {1_500, 100, 500, 40};

        for (int fluxo = 0; fluxo < janelas.length; fluxo++) {
            double de = janelas[fluxo][0];
            double ate = janelas[fluxo][1];

            /* Dentro da janela ele oferece o enlace inteiro, entao fica sempre cheio. */
            double oferta = CAPACIDADE * (ate - de) * 1.2;

            long quantos = (long) Math.ceil(oferta / tamanhos[fluxo]);

            double intervalo = (ate - de) / quantos;

            for (long i = 0; i < quantos; i++)
                pacotes.add(new Pacote(fluxo, tamanhos[fluxo],
                        Math.min(DURACAO - 1e-6, de + i * intervalo)));
        }

        return new Cenario("liga e desliga", new double[] {1, 1, 1, 1}, pacotes, CAPACIDADE,
                DURACAO);
    }

    /** Os cenarios saturados, em que o ideal e conta fechada. */
    public static List<Cenario> saturados() {
        return List.of(grandeContraPequeno(), quatroTamanhos(), pesosDiferentes(),
                pesosCruzados());
    }

    /** Todos, inclusive os que nao sao saturados. */
    public static List<Cenario> todos() {
        List<Cenario> saida = new ArrayList<>(saturados());

        saida.add(umAbusado());
        saida.add(ligaDesliga());

        return saida;
    }
}
