package com.example.nfcreceiver.NetPack;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpTask {
    static HttpTaskResponse htmlTask(HttpTaskRequest httpTaskRequest) {
        try {
            URL url = new URL(httpTaskRequest.getUrl());
            HttpURLConnection mHttpURLConnection = (HttpURLConnection) url.openConnection();
            mHttpURLConnection.setConnectTimeout(15000);
            mHttpURLConnection.setReadTimeout(15000);
            mHttpURLConnection.setRequestMethod(httpTaskRequest.getMethod());
            mHttpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            if (httpTaskRequest.getSession() != null)
                mHttpURLConnection.setRequestProperty("Cookie", httpTaskRequest.getSession());
            mHttpURLConnection.setDoInput(true);
            mHttpURLConnection.setDoOutput(!httpTaskRequest.getMethod().equals("GET"));
            mHttpURLConnection.setUseCaches(false);
            mHttpURLConnection.connect();

            if (!httpTaskRequest.getMethod().equals("GET")) {
                DataOutputStream dos = new DataOutputStream(mHttpURLConnection.getOutputStream());
                dos.write(httpTaskRequest.getValue().getBytes());
                dos.flush();
                dos.close();
            }

            int respondCode = mHttpURLConnection.getResponseCode();
            String cookieValue = mHttpURLConnection.getHeaderField("Set-Cookie");
            try {
                if (cookieValue != null)
                    cookieValue = cookieValue.substring(0, cookieValue.indexOf(";"));
            } catch (Exception e) {
                Log.d("Error", "1");
                e.printStackTrace();
                return null;
            }
            if (respondCode != 200) {
                Log.d("Error", "2 "+respondCode);
                return null;
            }

            InputStream is = mHttpURLConnection.getInputStream();
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            int len;
            byte buffer[] = new byte[1024];
            while ((len = is.read(buffer)) != -1)
                response.write(buffer, 0, len);
            is.close();
            response.close();

            return new HttpTaskResponse(cookieValue, new String(response.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("Error", "3");
        return null;
    }
}
