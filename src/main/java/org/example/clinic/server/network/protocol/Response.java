package org.example.clinic.server.network.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private boolean success;
    private Object data;
    private String error;
    private String errorCode;

    public Response() {
    }

    public static Response ok(String id, Object data) {
        Response r = new Response();
        r.id = id;
        r.success = true;
        r.data = data;
        return r;
    }

    public static Response error(String id, String code, String message) {
        Response r = new Response();
        r.id = id;
        r.success = false;
        r.errorCode = code;
        r.error = message;
        return r;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
