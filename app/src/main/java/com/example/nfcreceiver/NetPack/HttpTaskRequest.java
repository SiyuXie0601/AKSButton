package com.example.nfcreceiver.NetPack;

public class HttpTaskRequest {
    private String url;
    private String method;
    private String value;
    private String session;
    public HttpTaskRequest(String url, String method, String value, String session) {
        this.url = url;
        this.method = method;
        this.value = value;
        this.session = session;
    }
    String getUrl() { return url; }
    String getMethod() { return method; }
    String getValue() { return value; }
    String getSession() { return session; }
}
