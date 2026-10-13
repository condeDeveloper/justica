package br.com.conde.justica;

/**
 * UM PACOTE: de qual fluxo, quantos bytes, e quando chegou.
 *
 * <p>O TAMANHO e a razao de este repositorio existir. Se todo pacote tivesse o mesmo
 * tamanho, repartir a saida por pacote e reparti-la por byte seriam a mesma coisa, e o
 * rodizio simples seria justo. Nao sao, e ele nao e.
 *
 * <p>Num enlace de verdade os tamanhos variam de 40 bytes a 1.500, e quem manda pacote
 * grande leva mais banda do que quem manda pacote pequeno em qualquer disciplina que conte
 * pacotes.
 */
public record Pacote(int fluxo, long bytes, double chegada) {

    public Pacote {
        if (fluxo < 0) throw new IllegalArgumentException("fluxo " + fluxo);
        if (bytes < 1) throw new IllegalArgumentException("um pacote tem pelo menos um byte");
        if (chegada < 0) throw new IllegalArgumentException("chegada " + chegada);
    }
}
