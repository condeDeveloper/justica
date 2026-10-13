package br.com.conde.justica;

/**
 * A SAIDA: o laco que faz o tempo andar, igual para toda disciplina.
 *
 * <p>Ele esta num lugar so de proposito. Se cada disciplina tivesse o proprio laco, uma
 * diferenca de uma linha no jeito de avancar o relogio apareceria na tabela como se fosse
 * uma propriedade da disciplina.
 *
 * <p>A regra do fim merece uma frase: um pacote so e servido se ele CABE INTEIRO antes do
 * fim do cenario. Pacote nao se parte, e contar meio pacote faria a soma passar do que a
 * saida entrega. A sobra e menor que um pacote e vira menos de um decimo de por cento nas
 * duracoes usadas aqui.
 */
public final class Saida {

    private Saida() {}

    public static Contador rodar(Cenario cenario, Disciplina disciplina) {
        Contador contador = new Contador(cenario.fluxos());

        disciplina.comecar(cenario);

        double agora = 0;
        int proximo = 0;

        while (agora < cenario.duracao()) {
            while (proximo < cenario.pacotes().size()
                    && cenario.pacotes().get(proximo).chegada() <= agora + 1e-12) {

                disciplina.chegou(cenario.pacotes().get(proximo++), agora);
            }

            Pacote pacote = disciplina.proximo(agora, contador);

            if (pacote == null) {
                /* Ninguem tem o que mandar: a saida espera a proxima chegada. */
                if (proximo >= cenario.pacotes().size()) break;

                double ate = Math.min(cenario.pacotes().get(proximo).chegada(),
                        cenario.duracao());

                contador.ficouParada(ate - agora);

                agora = ate;
                continue;
            }

            double duracaoDoPacote = pacote.bytes() / cenario.capacidade();

            if (agora + duracaoDoPacote > cenario.duracao()) break;

            agora += duracaoDoPacote;

            contador.serviu(pacote.fluxo(), pacote.bytes());
        }

        return contador;
    }
}
