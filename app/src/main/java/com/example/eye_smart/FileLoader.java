package com.example.eye_smart;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileLoader {
    private final Context context;
    private final Uri fileUri;

    public FileLoader(Context context, Uri fileUri) {
        this.context = context;
        this.fileUri = fileUri;
    }

    public BufferedReader getBufferedReader() throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
        if (inputStream == null) {
            throw new IOException("파일을 열 수 없습니다.");
        }
        return new BufferedReader(new InputStreamReader(inputStream));
    }
}
