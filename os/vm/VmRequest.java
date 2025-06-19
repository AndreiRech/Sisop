package os.vm;

public class VmRequest {
    private int processId;
    private int requestedPage;
    private int frameToLoad;

    public VmRequest() {}

    public VmRequest(int processId, int requestedPage, int frameToLoad) {
        this.processId = processId;
        this.requestedPage = requestedPage;
        this.frameToLoad = frameToLoad;
    }

    public int getProcessId() {
        return processId;
    }

    public int getRequestedPage() {
        return requestedPage;
    }

    public int getFrameToLoad() {
        return frameToLoad;
    }

    public void setId(int id) {
		this.processId = id;
	}

	public void setPage(int page) {
		this.requestedPage = page;
	}

	public void setFrame(int frame) {
		this.frameToLoad = frame;
	}
}
