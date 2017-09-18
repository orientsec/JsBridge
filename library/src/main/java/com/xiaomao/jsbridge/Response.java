package com.xiaomao.jsbridge;

import org.json.JSONException;
import org.json.JSONObject;

public class Response {
    private String id;
    private String data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
            jsonObject.put("data", data);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Response toObject(String jsonStr) {
        try {
            Response response = new Response();
            JSONObject jsonObject = new JSONObject(jsonStr);
            response.setId(jsonObject.has("id") ? jsonObject.getString("id") : "");
            response.setData(jsonObject.has("data") ? jsonObject.getString("data") : "");
            return response;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
