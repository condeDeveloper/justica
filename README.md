# justica

Enfileiramento justo do zero em Java 21: **rodízio é justo em pacotes e não em bytes.**

O juiz é o **compartilhamento ideal**, em que a saída serve todos os fluxos ao mesmo tempo, em
fio de byte, dando a cada um a capacidade vezes o peso dele sobre a soma dos pesos dos fluxos que
têm o que mandar. Ele é o juiz certo e não só o conveniente pelo mesmo motivo que o ótimo de
Belady é o juiz de um cache: **é impossível de implementar**, porque supõe a saída infinitamente
divisível, e nenhum enlace manda meio pacote.

São **dois juízes**, e eles se conferem. Nos cenários em que todos os fluxos estão sempre cheios,
o ideal é conta fechada: peso sobre a soma dos pesos, e pronto. Nos cenários em que os fluxos
ligam e desligam, ele é uma simulação em fluido que pula de evento em evento. Um teste exige que
os dois deem o mesmo número onde os dois valem, e é isso que autoriza usar a simulação onde a
conta fechada não vale.

E ele exige **validade, não justiça**: que toda disciplina sirva todo pacote exatamente uma vez,
que nenhuma deixe a saída parada com pacote em fila, e que todas sirvam o mesmo total de bytes a
menos de um pacote. A prioridade estrita deixa fluxos com zero byte e passa; o rodízio dá quinze
para um e passa. A distância até o ideal é o que as tabelas medem em vez de julgar.

A unidade é o **byte servido**, nunca o segundo. Escolher entre ele e o **pacote** servido é o
repositório inteiro.

## Quinze para um, com a disciplina funcionando perfeitamente

```
$ mvn exec:java -Dexec.args="saturado"

  cenario            disciplina                injustica   menor recebe   maior recebe razao entre eles
  grande x pequeno   primeiro que chega           0.8752         0.1248         1.8720            15.00
  grande x pequeno   prioridade estrita           1.0000         0.0000         1.9980         Infinity
  grande x pequeno   rodizio                      0.8752         0.1248         1.8720            15.00
  grande x pequeno   deficit 1500                 0.0024         0.9976         1.0020             1.00
  grande x pequeno   fila justa                   0.0040         0.9960         1.0016             1.01
  quatro tamanhos    rodizio                      1.7360         0.0730         2.7360            37.50
  quatro tamanhos    deficit 1500                 0.0080         0.9958         1.0080             1.01
  pesos cruzados     rodizio                      5.8400         0.0456         6.8400           150.00
  pesos cruzados     deficit 1500                 0.0200         0.9900         1.0200             1.03
  pesos 1234         fila justa                   0.0000         1.0000         1.0000             1.00
```

Dois fluxos de peso igual, um com pacotes de 1.500 bytes e outro de 100. O rodízio dá **exatamente
um pacote para cada**, sem erro nenhum, e o primeiro leva **15,00 vezes** a banda do segundo.

Não é aproximadamente quinze. É 1.500 sobre 100. Com os quatro tamanhos típicos de um enlace, a
razão é **37,50**, que é 1.500 sobre 40. E quando os pesos vão no sentido contrário dos tamanhos,
é **150,00**, que é 1.500 vezes 4 sobre 40.

O defeito do rodízio tem uma causa e só uma: **ele conta a coisa errada.** No cenário em que todos
os pacotes têm 500 bytes, ele bate com o ideal byte por byte, com atraso zero.

O conserto cabe numa ideia. Cada fluxo ganha um **quantum de bytes** por volta e manda enquanto o
próximo pacote couber no crédito; o que sobra fica guardado no déficit e vale na volta seguinte. A
injustiça cai de 0,8752 para **0,0024** nos mesmos pacotes.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `saturado`, `ideal`, `quantum` e `preco`.

## O que as medidas me corrigiram

**A regra de bolso "o quantum tem que ser pelo menos o maior pacote" não é sobre justiça. Em
justiça ela aponta para o lado contrário.**

```
$ mvn exec:java -Dexec.args="quantum"

     quantum      vezes o maior      injustica  passos por pacote razao entre eles
          40              0.027         0.0040               3.91            1.006
         150              0.100         0.0040               1.78            1.006
         500              0.333         0.0040               1.23            1.008
        1000              0.667         0.0040               1.12            1.004
        1500              1.000         0.0080               1.08            1.012
        3000              2.000         0.0160               1.04            1.024
       15000             10.000         0.0800               1.01            1.125
```

Eu escrevi no código que o quantum precisa cobrir o maior pacote **para a repartição sair certa**,
e que abaixo disso o número de passos explodiria. Medi as duas coisas e errei as duas.

Com quantum de **40 bytes**, um trigésimo do maior pacote, a injustiça é **0,0040**. Com quantum
de **15.000**, dez vezes o maior pacote, ela é **0,0800**: **vinte vezes pior**. A coluna anda
monotonicamente na direção errada, linha por linha.

E os passos não explodem: vão de 1,08 para **3,91** por pacote, que é 3,6 vezes e não uma
catástrofe.

A regra existe, e é boa, mas o que ela compra é outra coisa: **com quantum maior que o maior
pacote, toda visita a um fluxo manda pelo menos um pacote**, e o trabalho por pacote fica
constante em vez de depender dos tamanhos. É uma garantia de **custo**, e as duas colunas da
tabela são exatamente esse troco.

A justiça anda para o outro lado porque o déficit já garante a parte certa no longo prazo, seja
qual for o quantum. O que o quantum grande faz é servir em rajadas maiores, e dentro de qualquer
janela finita de medida a rajada aparece como erro.

### O atraso contra o ideal não é uma questão de grau

```
$ mvn exec:java -Dexec.args="ideal"

  disciplina                duracao     maior atraso       em pacotes
  primeiro que chega            125            500.0             0.33
  primeiro que chega            250          41666.7            27.78
  primeiro que chega            500          62500.0            41.67
  prioridade estrita            125          62500.0            41.67
  prioridade estrita            250         144833.3            96.56
  prioridade estrita            500         332833.3           221.89
  rodizio                       125          54700.0            36.47
  rodizio                       250         101333.3            67.56
  rodizio                       500         214333.3           142.89
  deficit 1500                  125            500.0             0.33
  deficit 1500                  250            833.3             0.56
  deficit 1500                  500           1333.3             0.89
  fila justa                    125           1000.0             0.67
  fila justa                    250            733.3             0.49
  fila justa                    500            233.3             0.16
```

Esta é a tabela que separa as disciplinas em **duas espécies**, e não em graus.

Dobrando a janela de medida, o atraso do rodízio vai de 67,56 para **142,89** pacotes: dobra
junto. O da prioridade estrita vai de 96,56 para 221,89. O erro deles é uma **fração da banda**, e
fração de banda acumula para sempre.

O do rodízio com déficit vai de 0,56 para **0,89** pacote. O da fila justa **cai** de 0,49 para
**0,16**. Eles não acumulam: o que os atrasa é o pacote que já estava sendo servido quando a conta
começou, e isso não cresce com o tempo.

É a conta do teorema de Parekh e Gallager, de 1993, aparecendo numa tabela: uma fila justa
ponderada fica atrás do ideal por no máximo um pacote máximo. Aqui, por 0,16 dele.

### O primeiro que chega não tem comportamento próprio

Nos cenários saturados, a coluna do primeiro que chega é **idêntica** à do rodízio: mesma
injustiça, mesmos passos, mesmos bytes. Isso não é coincidência nem bug. As chegadas desses
cenários são intercaladas, um pacote de cada fluxo por vez, e servir na ordem de chegada passa a
ser exatamente servir em rodízio.

Ele reproduz a ordem em que o tráfego chegou, e mais nada. Isso é fácil de dizer e difícil de
aceitar, porque significa que **a repartição que ele faz é a que o remetente escolher**:

```
$ mvn exec:java -Dexec.args="ideal"

  cenario          disciplina            fluxo          ideal     disciplina   atraso bytes     em pacotes
  um abusado       primeiro que chega        0       320000.0       384500.0        64500.0          43.00
  um abusado       primeiro que chega        1        60000.0        38500.0       -21500.0         -14.33
  um abusado       rodizio                   0       320000.0       320000.0           -0.0          -0.00
  um abusado       rodizio                   1        60000.0        60000.0            0.0           0.00
```

Quatro fluxos iguais em tudo, um deles oferecendo dez vezes mais. O ideal **já é generoso com o
abusado**: como os outros três não usam a parte deles, ele recebe 320.000 dos 500.000 bytes do
enlace. O primeiro que chega lhe dá **384.500**, tirando um quinto do que cabia a cada um dos
outros.

E com pacotes todos do mesmo tamanho, o rodízio acerta o ideal **byte por byte**.

## O preço de decidir

```
$ mvn exec:java -Dexec.args="preco"

  disciplina                     passos        pacotes  passos por pacote    injustica media
  primeiro que chega               3451           3448              1.001             2.4878
  prioridade estrita               2002           1999              1.002             5.4965
  rodizio                          3451           3448              1.001             2.4878
  deficit 1500                    15055          14118              1.066             0.0126
  fila justa                     203098          14211             14.292             0.0045
```

A fila justa é **2,8 vezes mais justa** que o rodízio com déficit e **13 vezes mais cara**: ela
precisa de um mínimo entre todos os pacotes em fila a cada escolha, e as outras não precisam de
nada além de um ponteiro.

É por isso que o rodízio com déficit é o que está dentro do equipamento de verdade. A pergunta não
é qual chega mais perto do ideal, é se a distância que sobra cabe no orçamento, e 0,89 pacote de
atraso cabe em quase tudo.

## O que não tem aqui

**Sorteio nenhum.** Não há um gerador de números aleatórios neste repositório. Todos os cenários
são construídos à mão e todos os números são exatos, o que é possível aqui porque a pergunta é
sobre a disciplina e não sobre o tráfego médio. É a única coisa deste repositório que eu não
precisaria defender num artigo e que vale dizer: quando dá para medir sem sorteio, medir sem
sorteio é melhor.

**Perda de pacote.** Toda fila aqui é infinita. Numa fila de verdade o que transborda é descartado,
e qual pacote descartar é uma segunda decisão, tão importante quanto qual servir, com as mesmas
armadilhas de contar pacotes em vez de bytes.

**Atraso de cada pacote.** As tabelas medem repartição de banda, não quanto cada pacote esperou.
Uma disciplina pode repartir perfeitamente e ainda assim fazer um fluxo de voz esperar demais,
porque a banda dele chega em rajadas.

**Hierarquia.** Num equipamento de verdade os fluxos se agrupam em classes e as classes em
classes, e o ideal vira recursivo. Aqui há um nível só.

**Velocidade.** Nada é cronometrado. A coluna de passos conta candidatos olhados, e um passo da
fila justa (comparar dois instantes virtuais) não custa o mesmo que um passo do rodízio (olhar se
uma fila está vazia).

## Licença

MIT.
