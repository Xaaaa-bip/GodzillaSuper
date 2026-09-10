package core.ui.component.model;

public class FileOpertionInfo {
    private String srcFileName;
    private String destFileName;
    private Boolean opertionStatus;
    private boolean bigFile;

    public String getSrcFileName() {
        return this.srcFileName;
    }

    public String getDestFileName() {
        return this.destFileName;
    }

    public Boolean getOpertionStatus() {
        return this.opertionStatus;
    }

    public boolean isBigFile() {
        return this.bigFile;
    }

    public void setSrcFileName(String srcFileName) {
        this.srcFileName = srcFileName;
    }

    public void setDestFileName(String destFileName) {
        this.destFileName = destFileName;
    }

    public void setOpertionStatus(Boolean opertionStatus) {
        this.opertionStatus = opertionStatus;
    }

    public void setBigFile(boolean bigFile) {
        this.bigFile = bigFile;
    }
}
