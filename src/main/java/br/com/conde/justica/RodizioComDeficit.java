package br.com.conde.justica;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * RODIZIO COM DEFICIT: o rodizio que conta BYTES em vez de pacotes.
 *
 * <p>O conserto e de uma ideia so e cabe numa frase: cada fluxo ganha um QUANTUM de bytes
 * por volta e manda enquanto o proximo pacote couber no que ele tem de credito. O que
 * sobra nao se perde, fica guardado no deficit e vale na volta seguinte.
 *
 * <p>E e essa memoria que faz tudo funcionar. Sem ela, um fluxo de pacotes de 1.400 bytes
 * com quantum de 1.000 nunca mandaria nada; com ela, ele passa uma volta acumulando e manda
 * na seguinte. No longo prazo cada fluxo recebe exatamente o quantum dele por volta,
 * qualquer que seja o tamanho dos pacotes.
 *
 * <p>Com pesos, o quantum de cada fluxo e o quantum base vezes o peso dele, e a reparticao
 * sai proporcional aos pesos pelo mesmo argumento.
 *
 * <h2>O quantum, e o que a regra de bolso sobre ele quer dizer de verdade</h2>
 *
 * <p>A regra que se repete e "o quantum tem que ser pelo menos o maior pacote". Eu escrevi
 * isso aqui achando que era sobre JUSTICA, e medi: e o contrario. Com quantum de 40 bytes
 * contra um pacote maximo de 1.500, a injustica e 0,0040; com quantum de 15.000, dez vezes
 * o maior pacote, ela e 0,0800. Vinte vezes pior.
 *
 * <p>A regra e sobre CUSTO, nao sobre justica. Com quantum maior que o maior pacote, toda
 * visita a um fluxo manda pelo menos um pacote, e o trabalho por pacote fica constante. Com
 * quantum menor, o ponteiro passa voltas sem mandar nada, so acumulando credito: a tabela
 * mostra 3,91 passos por pacote com quantum de 40 contra 1,08 com quantum de 1.500.
 *
 * <p>E a justica anda para o outro lado porque o deficit ja garante a parte certa no longo
 * prazo, qualquer que seja o quantum; o que o quantum grande faz e deixar o servico mais em
 * rajada dentro de qualquer janela finita, e a janela da medida e finita.
 *
 * <p>E o unico parametro do repositorio inteiro, e ele troca passos por rajada.
 */
public final class RodizioComDeficit implements Disciplina {

    /** Um fluxo nao pode ficar girando para sempre sem conseguir mandar. */
    public static final int MAXIMO_DE_VOLTAS = 1_000_000;

    private final double quantumBase;

    private List<Deque<Pacote>> filas = List.of();
    private double[] deficit = new double[0];
    private double[] quantum = new double[0];
    private boolean[] recebeuNestaVisita = new boolean[0];

    private final Deque<Integer> ativos = new ArrayDeque<>();

    public RodizioComDeficit(double quantumBase) {
        if (quantumBase <= 0) throw new IllegalArgumentException("quantum " + quantumBase);

        this.quantumBase = quantumBase;
    }

    public double quantumBase() {
        return quantumBase;
    }

    @Override
    public String nome() {
        return "deficit " + (long) quantumBase;
    }

    @Override
    public void comecar(Cenario cenario) {
        filas = new ArrayList<>();

        for (int i = 0; i < cenario.fluxos(); i++) filas.add(new ArrayDeque<>());

        deficit = new double[cenario.fluxos()];
        quantum = new double[cenario.fluxos()];
        recebeuNestaVisita = new boolean[cenario.fluxos()];

        for (int i = 0; i < cenario.fluxos(); i++) quantum[i] = quantumBase * cenario.peso(i);

        ativos.clear();
    }

    @Override
    public void chegou(Pacote pacote, double agora) {
        boolean estavaVazia = filas.get(pacote.fluxo()).isEmpty();

        filas.get(pacote.fluxo()).addLast(pacote);

        if (!estavaVazia) return;

        /* Fluxo que estava vazio entra no fim da volta, com o credito zerado. */
        deficit[pacote.fluxo()] = 0;
        recebeuNestaVisita[pacote.fluxo()] = false;

        ativos.addLast(pacote.fluxo());
    }

    @Override
    public Pacote proximo(double agora, Contador contador) {
        for (int volta = 0; volta < MAXIMO_DE_VOLTAS; volta++) {
            if (ativos.isEmpty()) return null;

            int fluxo = ativos.peekFirst();

            contador.passo();

            if (!recebeuNestaVisita[fluxo]) {
                deficit[fluxo] += quantum[fluxo];
                recebeuNestaVisita[fluxo] = true;
            }

            Pacote cabeca = filas.get(fluxo).peekFirst();

            if (cabeca != null && cabeca.bytes() <= deficit[fluxo] + 1e-9) {
                deficit[fluxo] -= cabeca.bytes();

                filas.get(fluxo).removeFirst();

                if (filas.get(fluxo).isEmpty()) {
                    ativos.removeFirst();

                    /* Fluxo que esvazia perde o credito: guardar seria dar vantagem a
                     * quem ficou quieto, que e o contrario do que a disciplina promete. */
                    deficit[fluxo] = 0;
                    recebeuNestaVisita[fluxo] = false;
                }

                return cabeca;
            }

            /* Nao coube: passa a vez guardando o credito, e ganha outro quantum na volta. */
            ativos.removeFirst();
            ativos.addLast(fluxo);

            recebeuNestaVisita[fluxo] = false;
        }

        throw new IllegalStateException(
                "o ponteiro deu " + MAXIMO_DE_VOLTAS + " voltas sem mandar nada: o quantum de "
                        + quantumBase + " e pequeno demais para estes pacotes");
    }

    @Override
    public boolean temAlgo() {
        return !ativos.isEmpty();
    }
}
