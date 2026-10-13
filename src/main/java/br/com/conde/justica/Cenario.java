package br.com.conde.justica;

import java.util.ArrayList;
import java.util.List;

/**
 * UM CENARIO: os pesos dos fluxos, os pacotes que chegam, a capacidade e por quanto tempo.
 *
 * <p>A CAPACIDADE e em bytes por tique, e um pacote de b bytes ocupa a saida por b sobre a
 * capacidade tiques. Essa e a unica fisica do repositorio: a saida serve um pacote de cada
 * vez e nao interrompe pacote no meio.
 *
 * <p>Essa ultima frase e a origem de toda injustica medida aqui. O ideal contra o qual tudo
 * e comparado pode servir todos os fluxos ao mesmo tempo, um fio de byte de cada; nenhuma
 * disciplina de verdade pode, porque um pacote comecado tem que acabar.
 */
public record Cenario(String nome, double[] pesos, List<Pacote> pacotes, double capacidade,
        double duracao) {

    public Cenario {
        if (pesos.length < 1) throw new IllegalArgumentException("pelo menos um fluxo");
        if (capacidade <= 0) throw new IllegalArgumentException("capacidade " + capacidade);
        if (duracao <= 0) throw new IllegalArgumentException("duracao " + duracao);

        for (double peso : pesos)
            if (peso <= 0) throw new IllegalArgumentException("peso " + peso);

        pesos = pesos.clone();

        List<Pacote> ordenados = new ArrayList<>(pacotes);

        ordenados.sort(java.util.Comparator.comparingDouble(Pacote::chegada));

        for (Pacote pacote : ordenados)
            if (pacote.fluxo() >= pesos.length)
                throw new IllegalArgumentException(
                        "o pacote veio do fluxo " + pacote.fluxo() + " e so ha " + pesos.length);

        pacotes = List.copyOf(ordenados);
    }

    public int fluxos() {
        return pesos.length;
    }

    public double peso(int fluxo) {
        return pesos[fluxo];
    }

    public double somaDosPesos() {
        double soma = 0;

        for (double peso : pesos) soma += peso;

        return soma;
    }

    /**
     * A parte da saida que cabe a cada fluxo quando todos estao sempre cheios.
     *
     * <p>E o juiz do cenario saturado, e ele e um juiz fechado: nao e simulacao, e a
     * definicao de repartir por peso. Com todos os fluxos sempre com o que mandar, o ideal
     * entrega a cada um a capacidade vezes o peso dele sobre a soma dos pesos, durante o
     * tempo todo.
     */
    public double bytesIdeaisSaturado(int fluxo) {
        return capacidade * duracao * pesos[fluxo] / somaDosPesos();
    }

    /** Quantos bytes a saida consegue entregar no total, se nunca ficar ociosa. */
    public double bytesDaSaida() {
        return capacidade * duracao;
    }
}
