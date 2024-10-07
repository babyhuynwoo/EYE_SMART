package com.example.eye_smart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;

public class FilePicker {
    private final Context context;
    private final ActivityResultLauncher<Intent> filePickerLauncher;

    public FilePicker(Context context, ActivityResultLauncher<Intent> filePickerLauncher) {
        this.context = context;
        this.filePickerLauncher = filePickerLauncher;
    }

    // 파일 선택 시작 메서드
    public void pickTextFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    // 선택된 파일을 실제 File 객체로 변환하는 메서드
    @Nullable
    public File getFileFromUri(Uri uri) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null) {
                return null;
            }

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File tempFile = new File(context.getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // URI로부터 파일 이름을 가져오는 메서드
    @SuppressLint("Range")
    @Nullable
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = Objects.requireNonNull(result).lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
