package com.example.eye_smart;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.eye_smart.file_utils.FileLoader;
import com.example.eye_smart.file_utils.FilePicker;
import com.example.eye_smart.page_view_utils.PageDisplayer;
import com.example.eye_smart.dict_utils.ServerCommunicator;
import com.example.eye_smart.dict_utils.JsonParser;

public class MainActivity extends AppCompatActivity {

    private int currentPage = 0;

    private TextView textView;
    private FilePicker filePicker;
    private FileLoader fileLoader;
    private PageDisplayer pageDisplayer;
    private ServerCommunicator serverCommunicator;  // 서버 통신 객체 추가

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 초기화
        initUI();

        // 파일 선택기와 페이지 디스플레이어 초기화
        filePicker = new FilePicker(this, this::onFileSelected);
        pageDisplayer = new PageDisplayer(textView, 3f, 2f);
        serverCommunicator = new ServerCommunicator();  // 서버 통신 객체 초기화
    }

    private void initUI() {
        // 상단 툴바 설정
        Toolbar toolbarTop = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbarTop);

        // 뷰 초기화
        textView = findViewById(R.id.textView);
        Button buttonSelectFile = findViewById(R.id.button_select_file);
        Button buttonPrevPage = findViewById(R.id.button_prev_page);
        Button buttonNextPage = findViewById(R.id.button_next_page);
        // Button buttonSendToServer = findViewById(R.id.button_send_to_server);  // 서버 전송 이벤트 추가

        // 파일 선택 버튼 클릭 리스너 설정
        buttonSelectFile.setOnClickListener(v -> filePicker.pickTextFile());

        // 이전/다음 페이지 버튼 클릭 리스너 설정
        buttonPrevPage.setOnClickListener(v -> loadPreviousPage());
        buttonNextPage.setOnClickListener(v -> loadNextPage());

        // 서버 전송 이벤트 리스너 설정
        // buttonSendToServer.setOnClickListener(v -> sendTextToServer(textView.getText().toString()));
    }

    private void onFileSelected(Uri uri) {
        if (uri != null) {
            fileLoader = new FileLoader(this, uri);
            currentPage = 0;
            displayPage(currentPage);
        }
    }

    private void displayPage(int pageNumber) {
        // pageDisplayer가 null이 아닌지 확인 후 호출
        if (pageDisplayer != null && fileLoader != null) {
            pageDisplayer.displayPage(fileLoader, pageNumber, current -> currentPage = current);
        } else {
            textView.setText("PageDisplayer 또는 FileLoader가 초기화되지 않았습니다.");
        }
    }

    private void loadNextPage() {
        displayPage(++currentPage);
    }

    private void loadPreviousPage() {
        if (currentPage > 0) displayPage(--currentPage);
        else textView.setText("이전 페이지가 없습니다.");
    }

    // 서버로 텍스트 전송 메서드
    private void sendTextToServer(String text) {
        serverCommunicator.sendText(text, new ServerCommunicator.ResponseCallback() {
            @Override
            public void onResponse(String responseData) {
                JsonParser jsonParser = new JsonParser();
                String displayText = jsonParser.parseResponse(responseData);
                runOnUiThread(() -> textView.setText(displayText));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> textView.setText(errorMessage));
            }
        });
    }
}
