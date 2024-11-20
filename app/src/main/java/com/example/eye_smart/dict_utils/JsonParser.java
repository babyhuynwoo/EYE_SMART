package com.example.eye_smart.dict_utils;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonParser {

    public String parseResponse(String responseData) {
        StringBuilder displayText = new StringBuilder();
        try {
            // JSON 응답 파싱
            JSONObject jsonResponse = new JSONObject(responseData);

            // InputText 표시
            displayText.append("■ 입력된 텍스트\n\n");
            displayText.append("InputText: ").append(jsonResponse.optString("InputText", "없음")).append("\n\n");

            // Komoran 분석 결과 표시 (ExtractedWords 사용)
            //displayText.append("■ Komoran 형태소 분석 결과\n\n");
            displayText.append("■ 인식된 단어 \n\n");
            JSONArray extractedWords = jsonResponse.getJSONArray("ExtractedWords");
            for (int i = 0; i < extractedWords.length(); i++) {
                displayText.append(extractedWords.getString(i)).append(" ");
            }
            displayText.append("\n\n");

            // 단어 의미 표시
            displayText.append("■ 단어 의미\n\n");
            JSONObject meanings = jsonResponse.getJSONObject("Meanings");
            for (int i = 0; i < extractedWords.length(); i++) {
                String word = extractedWords.getString(i);
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
