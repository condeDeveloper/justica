package br.com.conde.justica;

import java.util.PriorityQueue;

/**
 * FILA JUSTA PONDERADA: calcula quando o ideal terminaria cada pacote, e serve nessa ordem.
 *
 * <p>Ela e a unica disciplina deste repositorio que olha para o ideal. Para cada pacote
 * que chega, ela calcula em que instante do RELOGIO VIRTUAL o compartilhamento ideal
 * terminaria de entrega-lo, e depois serve os pacotes na ordem desses instantes.
 *
 * <h2>O relogio virtual</h2>
 *
 * <p>O relogio virtual anda mais rapido quando ha poucos fluxos com o que mandar e mais
 * devagar quando ha muitos: a taxa dele e a capacidade sobre a soma dos pesos dos fluxos
 * ocupados. E essa unica definicao que faz toda a conta funcionar, porque nela o ideal
 * entrega a todo fluxo ocupado exatamente o peso dele de bytes por unidade de tempo
 * virtual, independentemente de quem mais esta ocupado.
 *
 * <p>Entao o fim virtual de um pacote e o comeco virtual dele mais bytes sobre peso, e o
 * comeco e o maior entre o relogio de agora e o fim do pacote anterior do mesmo fluxo.
 *
 * <p>Esse "maior entre os dois" e a parte que faz justica: um fluxo que ficou quieto nao
 * acumula credito nenhum, porque o relogio virtual andou sem ele e ele recomeca do relogio
 * atual. Uma disciplina que deixasse o credito acumular daria ao fluxo quieto uma rajada
 * inteira de saida ao voltar, em cima de quem ficou trabalhando.
 *
 * <h2>O preco</h2>
 *
 * <p>Ela precisa de um minimo entre todos os pacotes em fila a cada escolha, e e a unica
 * daqui que nao e de passo constante. A tabela de preco mostra isso.
 */
public final class FilaJustaPonderada implements Disciplina {

    /** Um pacote com o instante virtual em que o ideal terminaria de entrega-lo. */
    private record Marcado(Pacote pacote, double fimVirtual, long ordem) {}

    private final PriorityQueue<Marcado> espera = new PriorityQueue<>(
            java.util.Comparator.comparingDouble(Marcado::fimVirtual)
                    .thenComparingLong(Marcado::ordem));

    private Cenario cenario;

    private double relogioVirtual;
    private double[] fimDoAnterior = new double[0];
    private int[] emFila = new int[0];
    private long ordem;

    @Override
    public String nome() {
        return "fila justa";
    }

    @Override
    public void comecar(Cenario qual) {
        cenario = qual;

        espera.clear();

        relogioVirtual = 0;
        fimDoAnterior = new double[qual.fluxos()];
        emFila = new int[qual.fluxos()];
        ordem = 0;
    }

    @Override
    public void chegou(Pacote pacote, double agora) {
        double comeco = Math.max(relogioVirtual, fimDoAnterior[pacote.fluxo()]);

        double fim = comeco + pacote.bytes() / cenario.peso(pacote.fluxo());

        fimDoAnterior[pacote.fluxo()] = fim;

        emFila[pacote.fluxo()]++;

        espera.add(new Marcado(pacote, fim, ordem++));
    }

    @Override
    public Pacote proximo(double agora, Contador contador) {
        if (espera.isEmpty()) return null;

        /* O monte faz log n comparacoes, e nenhuma outra disciplina daqui faz. */
        contador.passo(Math.max(1, (int) Math.ceil(
                Math.log(espera.size() + 1) / Math.log(2))));

        Marcado escolhido = espera.poll();

        emFila[escolhido.pacote().fluxo()]--;

        /* O relogio virtual anda o que o ideal andaria enquanto este pacote e servido. */
        double somaDosOcupados = 0;

        for (int i = 0; i < cenario.fluxos(); i++)
            if (emFila[i] > 0 || i == escolhido.pacote().fluxo())
                somaDosOcupados += cenario.peso(i);

        if (somaDosOcupados > 0)
            relogioVirtual += escolhido.pacote().bytes() / somaDosOcupados;

        return escolhido.pacote();
    }

    @Override
    public boolean temAlgo() {
        return !espera.isEmpty();
    }
}
