package com.example.eye_smart.dict_utils;

import android.util.Log;

import com.example.eye_smart.BuildConfig;

import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ServerCommunicator {

    private static final String SERVER_URL = BuildConfig.SERVER_IP_ADDRESS;
    private static final String TAG = "ServerCommunicator";

    public interface ResponseCallback {
        void onResponse(String responseData);
        void onError(String errorMessage);
    }

    public void sendWord(String word, ResponseCallback callback) { // 메서드 이름 변경
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(100, TimeUnit.SECONDS)
                .readTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .build();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("word", word); // 키를 "word"로 변경
            Log.d(TAG, "JSON payload created: " + jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Error creating JSON payload.");
            return;
        }

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonObject.toString()
        );

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    Log.d(TAG, "Server response received: " + responseData);
                    callback.onResponse(responseData);
                } else {
                    callback.onError("서버 오류: " + response.code());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Request failed.", e);
                callback.onError("통신 오류: " + e.getMessage());
            }
        }).start();
    }
}
