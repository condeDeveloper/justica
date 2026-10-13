package br.com.conde.justica;

/**
 * O COMPARTILHAMENTO IDEAL: a saida serve TODOS os fluxos ao mesmo tempo, em fio de byte.
 *
 * <p>E o juiz deste repositorio, e e o juiz certo e nao so o conveniente, pelo mesmo motivo
 * que o otimo de Belady e o juiz de um cache: ele e impossivel de implementar e e por isso
 * que ele serve de regua.
 *
 * <p>Impossivel porque ele supoe que a saida e infinitamente divisivel: num instante
 * qualquer, cada fluxo com alguma coisa pendente recebe a capacidade vezes o peso dele
 * sobre a soma dos pesos dos fluxos pendentes, continuamente. Nenhum enlace faz isso. Um
 * enlace manda um pacote inteiro de cada vez.
 *
 * <p>E ele tem duas propriedades que fazem dele a definicao de justo e nao uma opiniao
 * sobre justo:
 *
 * <ul>
 *   <li>Proporcional: quem tem o dobro do peso recebe o dobro, sempre, em todo instante.
 *   <li>Conservativo: a saida nunca fica parada tendo alguem com o que mandar. O que um
 *       fluxo vazio nao usa e repartido entre os cheios, na proporcao dos pesos deles.
 * </ul>
 *
 * <h2>Como a conta anda</h2>
 *
 * <p>Entre dois eventos o conjunto de fluxos pendentes nao muda, entao a taxa de cada um e
 * constante e basta multiplicar. Os eventos sao dois: um pacote chega, ou um fluxo esvazia.
 * O laco pula de evento em evento e nunca avanca por passo de tempo fixo, que seria uma
 * aproximacao e traria erro de discretizacao para dentro do juiz.
 */
public final class Ideal {

    private Ideal() {}

    /** Quantos bytes de cada fluxo o ideal teria entregue ate o fim da duracao. */
    public static double[] bytesServidos(Cenario cenario) {
        int fluxos = cenario.fluxos();

        double[] pendente = new double[fluxos];
        double[] servido = new double[fluxos];

        double agora = 0;
        int proximo = 0;

        while (agora < cenario.duracao()) {
            /* Tudo que chegou ate agora entra na conta de pendente. */
            while (proximo < cenario.pacotes().size()
                    && cenario.pacotes().get(proximo).chegada() <= agora + 1e-12) {

                Pacote pacote = cenario.pacotes().get(proximo++);

                pendente[pacote.fluxo()] += pacote.bytes();
            }

            double soma = 0;

            for (int i = 0; i < fluxos; i++)
                if (pendente[i] > 0) soma += cenario.peso(i);

            double ateAChegada = proximo < cenario.pacotes().size()
                    ? cenario.pacotes().get(proximo).chegada() - agora
                    : Double.POSITIVE_INFINITY;

            double ateOFim = cenario.duracao() - agora;

            if (soma == 0) {
                /* Ninguem tem o que mandar: a saida fica parada ate a proxima chegada. */
                if (ateAChegada == Double.POSITIVE_INFINITY) break;

                agora += Math.min(ateAChegada, ateOFim);
                continue;
            }

            double ateEsvaziar = Double.POSITIVE_INFINITY;

            for (int i = 0; i < fluxos; i++) {
                if (pendente[i] <= 0) continue;

                double taxa = cenario.capacidade() * cenario.peso(i) / soma;

                ateEsvaziar = Math.min(ateEsvaziar, pendente[i] / taxa);
            }

            double salto = Math.min(ateOFim, Math.min(ateAChegada, ateEsvaziar));

            if (salto <= 0) break;

            for (int i = 0; i < fluxos; i++) {
                if (pendente[i] <= 0) continue;

                double entregue = cenario.capacidade() * cenario.peso(i) / soma * salto;

                servido[i] += entregue;
                pendente[i] -= entregue;

                /* Sem isto sobra um residuo de ponto flutuante e o fluxo nunca esvazia. */
                if (pendente[i] < 1e-9) pendente[i] = 0;
            }

            agora += salto;
        }

        return servido;
    }

    /**
     * Quantos bytes de cada fluxo o ideal teria entregue ate um instante qualquer.
     *
     * <p>Serve para medir o ATRASO de uma disciplina em relacao a ele, que e a conta do
     * teorema de Parekh e Gallager: uma disciplina boa fica atras do ideal por no maximo
     * um pacote, e nao por uma fracao da banda.
     */
    public static double[] bytesServidosAte(Cenario cenario, double instante) {
        return bytesServidos(new Cenario(cenario.nome(), copiaDosPesos(cenario),
                cenario.pacotes(), cenario.capacidade(), instante));
    }

    private static double[] copiaDosPesos(Cenario cenario) {
        double[] pesos = new double[cenario.fluxos()];

        for (int i = 0; i < pesos.length; i++) pesos[i] = cenario.peso(i);

        return pesos;
    }
}
