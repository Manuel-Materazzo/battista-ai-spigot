package org.itsmanu.battistaAiSpigot.dto;

public class BackendResponse {
    private String message = "";
    private Boolean flag = null;

    public BackendResponse(Boolean flag, String message) {
        this.flag = flag;
        this.message = message;
    }

    public BackendResponse(String message) {
        this.message = message;
    }

    public BackendResponse(boolean flag) {
        this.flag = flag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFlag() {
        return Boolean.TRUE.equals(this.flag);
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
