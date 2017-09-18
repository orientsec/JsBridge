package com.xiaomao.jsbridge;

import org.json.JSONException;
import org.json.JSONObject;

public class Request {
    private String id;
    private String handlerName;
    private String data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("handlerName", handlerName);
            jsonObject.put("data", data);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Request toObject(String jsonStr) {
        try {
            Request request = new Request();
            JSONObject jsonObject = new JSONObject(jsonStr);
            request.setId(jsonObject.has("id") ? jsonObject.getString("id") : "");
            request.setHandlerName(jsonObject.has("handlerName") ? jsonObject.getString("handlerName") : "");
            request.setData(jsonObject.has("data") ? jsonObject.getString("data") : "");
            return request;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
