package com.example.eye_smart;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

public class FilePicker {
    private final ActivityResultLauncher<Intent> filePickerLauncher;

    public FilePicker(ActivityResultLauncher<Intent> filePickerLauncher) {
        this.filePickerLauncher = filePickerLauncher;
    }

    // 파일 선택 시작 메서드
    public void pickTextFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

}
