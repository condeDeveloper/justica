package br.com.conde.justica;

/**
 * UMA DISCIPLINA DE FILA: quem manda o proximo pacote.
 *
 * <p>Esta e a unica decisao. Tudo mais ja esta decidido: a saida serve um pacote inteiro de
 * cada vez, na capacidade do enlace, e nao interrompe pacote no meio.
 *
 * <p>Toda disciplina daqui e CONSERVATIVA: ela nunca deixa a saida parada tendo pacote na
 * mao. Isso nao e uma gentileza, e o que torna a comparacao uma comparacao: duas
 * disciplinas conservativas servem exatamente o mesmo TOTAL de bytes, e a unica coisa que
 * muda entre elas e de quem sao esses bytes.
 */
public interface Disciplina {

    String nome();

    /** Zera o estado interno para este cenario. */
    void comecar(Cenario cenario);

    /** Um pacote chegou e entra na fila. */
    void chegou(Pacote pacote, double agora);

    /** O proximo a ser servido, ou null quando nao ha nenhum em fila. */
    Pacote proximo(double agora, Contador contador);

    boolean temAlgo();
}
