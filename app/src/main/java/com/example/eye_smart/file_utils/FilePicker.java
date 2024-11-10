package com.example.eye_smart.file_utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.function.Consumer;

import androidx.activity.result.ActivityResultCaller;

public class FilePicker {

    private final ActivityResultLauncher<Intent> filePickerLauncher;

    public FilePicker(ActivityResultCaller caller, Consumer<Uri> onFileSelected) {
        filePickerLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            onFileSelected.accept(data.getData());
                        }
                    }
                }
        );
    }

    public void pickTextFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        filePickerLauncher.launch(intent);
    }
}
