package com.example.nfcreceiver.NetPack;

import org.json.JSONException;

public interface OnAsyncTaskListener {
    void callback(HttpTaskResponse httpTaskResponse) throws JSONException;
}
