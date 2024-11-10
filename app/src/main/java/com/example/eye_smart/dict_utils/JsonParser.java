package com.example.eye_smart.dict_utils;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonParser {

    public String parseResponse(String responseData) {
        StringBuilder displayText = new StringBuilder();
        try {
            JSONObject jsonResponse = new JSONObject(responseData);

            // Komoran 분석 결과 표시
            displayText.append("■ Komoran 형태소 분석 결과\n\n");
            displayText.append("Komoran: ").append(jsonResponse.getString("Komoran")).append("\n\n");

            // Komoran 분석 결과에 해당하는 단어 의미 표시
            displayText.append("■ 단어 의미\n\n");
            JSONObject meanings = jsonResponse.getJSONObject("Meanings");
            JSONArray komoranWords = new JSONArray(jsonResponse.getString("Komoran").split(" "));

            for (int i = 0; i < komoranWords.length(); i++) {
                String word = komoranWords.getString(i);
                if (meanings.has(word)) {
                    JSONObject wordMeanings = meanings.getJSONObject(word);
                    JSONArray standardDict = wordMeanings.getJSONArray("StandardDict");

                    displayText.append("● ").append(word).append("\n");
                    for (int j = 0; j < standardDict.length(); j++) {
                        String meaning = standardDict.getString(j);
                        if (!meaning.startsWith("API") && !meaning.startsWith("HTTP")) {
                            displayText.append("  - ").append(meaning).append("\n");
                        }
                    }
                    displayText.append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "응답 파싱 오류: " + e.getMessage();
        }

        return displayText.toString();
    }
}
