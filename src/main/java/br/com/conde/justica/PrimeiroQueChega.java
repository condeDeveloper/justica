package br.com.conde.justica;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * PRIMEIRO QUE CHEGA, PRIMEIRO SERVIDO: uma fila so, sem olhar de quem e o pacote.
 *
 * <p>E o que um enlace faz quando ninguem decidiu nada, e e o padrao de fato em quase todo
 * equipamento barato. Ela nao tem fluxo nenhum no codigo: nao ha pesos, nao ha classes, nao
 * ha o que configurar.
 *
 * <p>E e por isso que ela nao protege ninguem. Um fluxo que manda o dobro de pacotes leva o
 * dobro da saida; um que manda cem vezes leva cem vezes. A reparticao nao e por peso, e por
 * QUANTO CADA UM MANDOU, que e o oposto do que uma disciplina de fila existe para fazer.
 */
public final class PrimeiroQueChega implements Disciplina {

    private final Deque<Pacote> fila = new ArrayDeque<>();

    @Override
    public String nome() {
        return "primeiro que chega";
    }

    @Override
    public void comecar(Cenario cenario) {
        fila.clear();
    }

    @Override
    public void chegou(Pacote pacote, double agora) {
        fila.addLast(pacote);
    }

    @Override
    public Pacote proximo(double agora, Contador contador) {
        if (fila.isEmpty()) return null;

        /* Um passo: nao ha o que escolher, e e a vantagem inteira dela. */
        contador.passo();

        return fila.removeFirst();
    }

    @Override
    public boolean temAlgo() {
        return !fila.isEmpty();
    }
}
