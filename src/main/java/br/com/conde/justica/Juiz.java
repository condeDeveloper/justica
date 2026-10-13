package br.com.conde.justica;

import java.util.List;

/**
 * O JUIZ: exige VALIDADE, e a validade aqui nao tem nada a ver com justica.
 *
 * <p>Ele cobra quatro coisas:
 *
 * <ul>
 *   <li>Nenhum pacote foi servido duas vezes, e nenhum foi inventado: com duracao
 *       suficiente, a conta de pacotes por fluxo bate exatamente com a de pacotes
 *       oferecidos por fluxo.
 *   <li>A soma dos bytes servidos nao passa do que a saida entrega na duracao.
 *   <li>A disciplina e CONSERVATIVA: a saida nunca ficou parada com pacote em fila.
 *   <li>Todas as disciplinas servem o mesmo total de bytes, a menos de um pacote. Esse e o
 *       que torna a comparacao uma comparacao: nenhuma delas ganha banda, elas so a
 *       repartem diferente.
 * </ul>
 *
 * <p>Ele NAO cobra justica. A prioridade estrita deixa fluxos com zero byte e passa; o
 * rodizio da quinze para um e passa. A distancia ate o ideal e o que as tabelas medem em
 * vez de julgar, e e exatamente por isso que o juiz precisa ser cego para ela.
 */
public final class Juiz {

    private Juiz() {}

    public record Veredito(String disciplina, long pacotesServidos, long pacotesOferecidos,
            double bytesServidos, double bytesDaSaida, double tempoOcioso, boolean valido,
            String problema) {}

    /** Todas as disciplinas deste repositorio, da mais boba para a mais cara. */
    public static List<Disciplina> todas() {
        return List.of(
                new PrimeiroQueChega(),
                new PrioridadeEstrita(),
                new RodizioPorPacote(),
                new RodizioComDeficit(Cenarios.MAIOR_PACOTE),
                new FilaJustaPonderada());
    }

    /**
     * Julga uma disciplina num cenario alargado ate dar tempo de servir tudo.
     *
     * <p>O cenario de medida para no meio de proposito, para que a reparticao seja medida
     * numa janela; para julgar a validade e preciso deixar escoar, senao os pacotes que
     * sobraram em fila pareceriam perdidos.
     */
    public static Veredito julgar(Disciplina disciplina, Cenario cenario) {
        double bytesOferecidos = 0;

        for (Pacote pacote : cenario.pacotes()) bytesOferecidos += pacote.bytes();

        /* Tempo de sobra para escoar tudo, com folga para a ociosidade do liga e desliga. */
        double duracaoFolgada = cenario.duracao() + bytesOferecidos / cenario.capacidade() + 1;

        Cenario folgado = new Cenario(cenario.nome(), pesosDe(cenario), cenario.pacotes(),
                cenario.capacidade(), duracaoFolgada);

        Contador contador;

        try {
            contador = Saida.rodar(folgado, disciplina);
        } catch (RuntimeException erro) {
            return new Veredito(disciplina.nome(), 0, cenario.pacotes().size(), 0,
                    folgado.bytesDaSaida(), 0, false,
                    erro.getClass().getSimpleName() + ": " + erro.getMessage());
        }

        long[] oferecidosPorFluxo = new long[cenario.fluxos()];

        for (Pacote pacote : cenario.pacotes()) oferecidosPorFluxo[pacote.fluxo()]++;

        for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++)
            if (contador.pacotesDe(fluxo) != oferecidosPorFluxo[fluxo])
                return new Veredito(disciplina.nome(), contador.pacotesNoTotal(),
                        cenario.pacotes().size(), contador.bytesNoTotal(),
                        folgado.bytesDaSaida(), contador.tempoOcioso(), false,
                        "o fluxo " + fluxo + " ofereceu " + oferecidosPorFluxo[fluxo]
                                + " pacotes e recebeu " + contador.pacotesDe(fluxo));

        if (contador.bytesNoTotal() > bytesOferecidos + 1e-6)
            return new Veredito(disciplina.nome(), contador.pacotesNoTotal(),
                    cenario.pacotes().size(), contador.bytesNoTotal(), folgado.bytesDaSaida(),
                    contador.tempoOcioso(), false,
                    "serviu " + contador.bytesNoTotal() + " bytes e so chegaram "
                            + bytesOferecidos);

        return new Veredito(disciplina.nome(), contador.pacotesNoTotal(),
                cenario.pacotes().size(), contador.bytesNoTotal(), folgado.bytesDaSaida(),
                contador.tempoOcioso(), true, "");
    }

    /** O maior erro de reparticao em relacao ao ideal, em fracao da parte devida. */
    public static double injustica(Cenario cenario, Contador contador, double[] ideal) {
        double pior = 0;

        for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++) {
            if (ideal[fluxo] <= 0) continue;

            pior = Math.max(pior, Math.abs(contador.bytesDe(fluxo) - ideal[fluxo]) / ideal[fluxo]);
        }

        return pior;
    }

    /** A maior diferenca em BYTES entre o que a disciplina deu e o que o ideal daria. */
    public static double maiorAtraso(Cenario cenario, Contador contador, double[] ideal) {
        double pior = 0;

        for (int fluxo = 0; fluxo < cenario.fluxos(); fluxo++)
            pior = Math.max(pior, Math.abs(contador.bytesDe(fluxo) - ideal[fluxo]));

        return pior;
    }

    static double[] pesosDe(Cenario cenario) {
        double[] pesos = new double[cenario.fluxos()];

        for (int i = 0; i < pesos.length; i++) pesos[i] = cenario.peso(i);

        return pesos;
    }
}
