package br.com.conde.justica;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * PRIORIDADE ESTRITA: o fluxo de menor numero manda enquanto tiver o que mandar.
 *
 * <p>Ela esta aqui porque e o que muita gente quer dizer quando pede "qualidade de
 * servico": uma classe que passa na frente. E ela faz exatamente isso, com a consequencia
 * que vem junto e que ninguem pede.
 *
 * <p>Se o fluxo de cima sozinho ja enche a saida, os de baixo recebem ZERO. Nao pouco:
 * zero. Nenhum peso, nenhuma configuracao e nenhum ajuste muda isso, porque a regra nao
 * tem peso nenhum dentro.
 *
 * <p>Repare que ela e conservativa: ela nunca deixa a saida parada. Ser conservativa nao
 * tem nada a ver com ser justa, e esta disciplina esta aqui para deixar isso claro.
 */
public final class PrioridadeEstrita implements Disciplina {

    private List<Deque<Pacote>> filas = List.of();

    @Override
    public String nome() {
        return "prioridade estrita";
    }

    @Override
    public void comecar(Cenario cenario) {
        filas = new java.util.ArrayList<>();

        for (int i = 0; i < cenario.fluxos(); i++) filas.add(new ArrayDeque<>());
    }

    @Override
    public void chegou(Pacote pacote, double agora) {
        filas.get(pacote.fluxo()).addLast(pacote);
    }

    @Override
    public Pacote proximo(double agora, Contador contador) {
        for (Deque<Pacote> fila : filas) {
            contador.passo();

            if (!fila.isEmpty()) return fila.removeFirst();
        }

        return null;
    }

    @Override
    public boolean temAlgo() {
        for (Deque<Pacote> fila : filas)
            if (!fila.isEmpty()) return true;

        return false;
    }
}
