package SimulatedOS.OS.VM;

public class VmRequisition {
    private int id; // id do processo que requisitou a pagina
    private int page; // pagina requisitada
    private int frame; // frame onde a pagina deve ser carregada

    public VmRequisition(int _id, int _page, int _frame) {
        id = _id;
        page = _page;
        frame = _frame;
    }

    public int getId() {
        return id;
    }

    public int getPage() {
        return page;
    }

    public int getFrame() {
        return frame;
    }
}
