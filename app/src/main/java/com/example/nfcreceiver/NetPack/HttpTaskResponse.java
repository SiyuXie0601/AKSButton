package com.example.nfcreceiver.NetPack;

public class HttpTaskResponse {
    private String cookie;
    private String response;
    HttpTaskResponse(String cookie, String response) {
        this.cookie = cookie;
        this.response = response;
    }
    public String getCookie() { return cookie; }
    public String getResponse() { return response; }
}
