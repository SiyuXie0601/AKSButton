package com.example.nfcreceiver;

import com.example.nfcreceiver.NetPack.HttpTaskRequest;
import com.example.nfcreceiver.NetPack.HttpTaskResponse;
import com.example.nfcreceiver.NetPack.OnAsyncTaskListener;
import com.example.nfcreceiver.NetPack.ThreadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class API {
    static String cookie = null;
    private static ArrayList<APITag> apis = new ArrayList<>();

    private static class CommonListener {
        OnAsyncTaskListener httpListener;
        CommonListener(final ResponseListener listener) {
            httpListener = new OnAsyncTaskListener() {
                @Override
                public void callback(HttpTaskResponse httpTaskResponse) throws JSONException {
                    if (httpTaskResponse.getCookie() != null)
                        cookie = httpTaskResponse.getCookie();
                    listener.callback(httpTaskResponse.getResponse());
                }
            };
        }
    }

    static void init() {
        String GET = "GET", POST = "POST", PUT = "PUT", DELETE = "DELETE";
        apis.clear();
        apis.add(new APITag(ADD_ORDER, POST, "/order"));
    }

    private static String host = "https://aks.6-79.cn";

    private static int ADD_ORDER = 19;  // 增加订单

    private static APITag find(int tag) {
        for (APITag item : apis) {
            if (item.tag == tag)
                return item;
        }
        return null;
    }

    static ThreadTask addOrder(String userId, int categoryId, ResponseListener listener) {
        APITag apiTag = find(ADD_ORDER);
        if (apiTag == null)
            return null;
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id", userId).put("category_id", categoryId);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        CommonListener commonListener = new CommonListener(listener);
        HttpTaskRequest httpTaskRequest = new HttpTaskRequest(host + apiTag.url, apiTag.method, jsonObject.toString(), cookie);
        return new ThreadTask(httpTaskRequest, commonListener.httpListener);
    }
}

class APITag {
    int tag;
    String url;
    String method;
    APITag(int tag, String method, String url) {
        this.tag = tag;
        this.method = method;
        this.url = url;
    }
}

interface ResponseListener {
    void callback(String response) throws JSONException;
}