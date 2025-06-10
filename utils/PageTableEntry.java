package utils;

public class PageTableEntry {
    private boolean isValid;
    private int frame;

    public PageTableEntry() {
        this.isValid = false;
        this.frame = -1;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public int getFrame() {
        return frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    @Override
    public String toString() {
        return "PageTableEntry [isValid=" + isValid + ", frame=" + frame + "]";
    }
}
