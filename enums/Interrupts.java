package enums;

public enum Interrupts { // possiveis interrupcoes que esta CPU gera
    intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP, roundRobin, ioFinished, pageFault, pageSaved,
    pageLoaded;
}
