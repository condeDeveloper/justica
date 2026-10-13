package br.com.conde.justica;

/**
 * O CONTADOR: bytes servidos por fluxo. Nunca segundos.
 *
 * <p>O BYTE SERVIDO e a medida certa e nao so a conveniente, e escolher entre ele e o
 * PACOTE servido e o repositorio inteiro. Quem conta pacotes acha que o rodizio e justo;
 * quem conta bytes ve que ele nao e. As duas colunas estao aqui lado a lado de proposito.
 *
 * <p>E a medida tem teto conhecido, o que a torna legivel sem comparar com nada: a saida
 * entrega no maximo a capacidade vezes a duracao, e o ideal reparte exatamente isso na
 * proporcao dos pesos.
 *
 * <p>Ha tambem a conta de PASSOS DE DECISAO, que e quantos candidatos a disciplina olhou
 * para escolher quem manda. Ela existe porque nenhuma disciplina e de graca e porque a
 * mais justa deste repositorio e a que mais olha.
 *
 * <p>Segundos nao serviriam: a integracao continua roda em tres sistemas, e nada aqui e
 * uma rede de verdade. O tempo do cenario e em tiques e e exato.
 */
public final class Contador {

    private final double[] bytes;
    private final long[] pacotes;

    private long passosDeDecisao;
    private double tempoOcioso;

    public Contador(int fluxos) {
        this.bytes = new double[fluxos];
        this.pacotes = new long[fluxos];
    }

    public void serviu(int fluxo, long quantosBytes) {
        bytes[fluxo] += quantosBytes;
        pacotes[fluxo]++;
    }

    public void passo() {
        passosDeDecisao++;
    }

    public void passo(long quantos) {
        passosDeDecisao += quantos;
    }

    public void ficouParada(double quanto) {
        tempoOcioso += quanto;
    }

    public double bytesDe(int fluxo) {
        return bytes[fluxo];
    }

    public long pacotesDe(int fluxo) {
        return pacotes[fluxo];
    }

    public double bytesNoTotal() {
        double soma = 0;

        for (double quantos : bytes) soma += quantos;

        return soma;
    }

    public long pacotesNoTotal() {
        long soma = 0;

        for (long quantos : pacotes) soma += quantos;

        return soma;
    }

    public long passosDeDecisao() {
        return passosDeDecisao;
    }

    public double tempoOcioso() {
        return tempoOcioso;
    }

    public int fluxos() {
        return bytes.length;
    }
}
