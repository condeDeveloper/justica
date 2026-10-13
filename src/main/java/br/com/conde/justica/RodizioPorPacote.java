package br.com.conde.justica;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * RODIZIO POR PACOTE: um pacote de cada fluxo, em volta.
 *
 * <p>E a primeira coisa que qualquer um inventa para ser justo, e ela E justa: ela reparte
 * os PACOTES por igual, exatamente, sem erro nenhum.
 *
 * <p>So que o enlace nao reparte pacotes, reparte BYTES. Um fluxo que manda pacotes de 1500
 * bytes e outro que manda de 100 recebem um pacote cada por volta, e o primeiro leva quinze
 * vezes mais banda. A disciplina esta funcionando perfeitamente e o resultado e quinze para
 * um.
 *
 * <p>Essa e a medida principal do repositorio, e ela nao e um caso construido: tamanhos de
 * pacote diferentes sao o caso normal de qualquer enlace que carregue mais de um tipo de
 * trafego.
 *
 * <p>Ele tambem ignora os pesos, porque nao ha onde encaixa-los: um pacote por volta e um
 * pacote por volta.
 */
public final class RodizioPorPacote implements Disciplina {

    private List<Deque<Pacote>> filas = List.of();
    private int ponteiro;

    @Override
    public String nome() {
        return "rodizio";
    }

    @Override
    public void comecar(Cenario cenario) {
        filas = new ArrayList<>();

        for (int i = 0; i < cenario.fluxos(); i++) filas.add(new ArrayDeque<>());

        ponteiro = 0;
    }

    @Override
    public void chegou(Pacote pacote, double agora) {
        filas.get(pacote.fluxo()).addLast(pacote);
    }

    @Override
    public Pacote proximo(double agora, Contador contador) {
        for (int tentativa = 0; tentativa < filas.size(); tentativa++) {
            contador.passo();

            Deque<Pacote> fila = filas.get(ponteiro);

            ponteiro = (ponteiro + 1) % filas.size();

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
