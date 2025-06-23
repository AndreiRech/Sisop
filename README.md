# Integrantes

Andrei Rech, Carlos Moares, Eduardo Wolf, Erik Suris, Mariah Freire.

# Seção Implementação

### IO

- [x] Leitura
- [x] Escrita
- [x] Console opera concorrentemente

### MEMÓRIA VIRTUAL

- [x] Alocação do processo inteiro em disco
- [x] Alocação da primeira página em memória
- [x] PAGE FAULT quando a página está faltante
- [x] Tratamentos de interrupções para pageFault, pageSave
- [x] Caso não haja espaço em memória, utiliza FIFO como política de substituição
- [x] Possibilidade de rodar mais de um programa concorrentemente

# Seção Testes

Para cada caso, recomendamos re-compilar o código, para limpar a memória e o disco.

### Caso 1: Out

#### Setup
Nenhum setup será necessário.

#### Comandos
- 1 - (Listagem de processos)
- 2 - (Cria processo 2)
- 6, 0, 15 - (Da um dump na memória e contém apenas a primeira página)
- 7, 0, 30 - (Da um dump no disco e contém todo o programa)
- 4 - (Executa)
- 6, 0, 30 - (Da um dump na memória e contém todas as páginas e o resultado na posição `6`)

#### Resultados

Note que durante o fim da execução, você verá uma linha contendo: `OUT:   18`. O que mostra que foi feito a operação `OUT`.

Também, como dito anteriormente, o resultado será `6:  [ DATA, -1, -1, 120 ]`.

### Caso 2: In

#### Setup
Nenhum setup será necessário.

#### Comandos
- 1 - (Listagem de processos)
- 6 - (Cria processo 6)
- 6, 0, 15 - (Da um dump na memória e contém apenas a primeira página)
- 7, 0, 60 - (Da um dump no disco e contém todo o programa)
- 4 - (Executa)
- 6, 0, 60 - (Da um dump na memória e contém todas as páginas)

#### Resultados

Durante a execução, será possível visualizar as linhas:

```
IN: 
```

Nesse momento deveria haver uma entrada do usuário, entretanto, como estava dando muitos problemas, fixamos a entrada para o valor 9 e o valor esperado de saída deve ser 21.

Como esperado, a sequência inteira pode ser encontrada no disco, e o último valor está presente em `49:  [ DATA, -1, -1, 21  ]`.

### Caso 3: Processos Concorrentes

#### Setup
Nenhum setup será necessário.

#### Comandos
- 1 - (Listagem de processos)
- 1 - (Cria processo 1)
- 6, 0, 15 - (Da um dump na memória e contém apenas a primeira página)
- 7, 0, 15 - (Da um dump no disco e contém todo o programa)
- 1 - (Listagem de processos)
- 4 - (Cria processo 4)
- 6, 0, 15 - (Da um dump na memória e contém apenas a primeira página de cada processo)
- 7, 0, 50 - (Da um dump no disco e contém todos os programas)
- 4 - (Executa)
- 6, 0, 50 - (Da um dump na memória e contém todas as páginas)

#### Resultados

O resultado do primeiro preocesso pode ser encontrado em: 
```
30:  [ DATA, -1, -1, 5040  ]
```
Já, do segundo, como as páginas trazidas não são sequenciais, pode ser enontrado em:
```
8:  [ DATA, -1, -1, 0  ]
9:  [ DATA, -1, -1, 1  ]
10:  [ DATA, -1, -1, 1  ]
11:  [ DATA, -1, -1, 2  ]
...
32:  [ DATA, -1, -1, 3  ]
33:  [ DATA, -1, -1, 5  ]
34:  [ DATA, -1, -1, 8  ]
35:  [ DATA, -1, -1, 13  ]
36:  [ DATA, -1, -1, 21  ]
37:  [ DATA, -1, -1, 34  ]
38:  [ DATA, -1, -1, 55  ]
```

### Caso 4: Política de Substituição

#### Setup
Na classe `Sistema.java`, na linha `24`, altere o valor de `memorySize` de `512` para `8` - dessa forma, será possível visualizar a política de substituição.

#### Comandos
- 1 - (Listagem de processos)
- 1 - (Cria processo 1)
- 6, 0, 15 - (Da um dump na memória e contém apenas a primeira página)
- 7, 0, 15 - (Da um dump no disco e contém todo o programa)
- 4 - (Executa)
- 6, 0, 7 - (Da um dump na memória e contém todas as páginas e o resultado na posição `2`)

#### Resultados

Durante a execução, será possível visualizar as linhas:

```
PAGE FAULT / Processo 1 - Página Lógica 2
Sem frames disponíveis. Iniciando substituição de página...
Página 2 carregada do disco (pág. 2) para o frame da RAM 0
```

Também, como dito anteriormente, o resultado será `2:  [ DATA, -1, -1, 5040  ]`.