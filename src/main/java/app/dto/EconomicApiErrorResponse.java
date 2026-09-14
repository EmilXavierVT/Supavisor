package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EconomicApiErrorResponse {
    private String message;
    private String logId;

    public EconomicApiErrorResponse() {
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
}
