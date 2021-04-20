package com.example.speech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.example.speech.R;

import org.snu.ids.kkma.ma.MExpression;
import org.snu.ids.kkma.ma.MorphemeAnalyzer;
import org.snu.ids.kkma.ma.Sentence;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    Intent intent;
    SpeechRecognizer mRecognizer;
    Button sttBtn, btnSelect;
    TextView textView;
    EditText dbText;
    final int PERMISSION = 1;

    myDBHelper myDBHelper;
    SQLiteDatabase sqlDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= 23) {
            // 퍼미션 체크
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }
        textView = (TextView) findViewById(R.id.sttResult);
        dbText = (EditText) findViewById(R.id.dbtext);
        sttBtn = (Button) findViewById(R.id.sttStart);
        btnSelect = (Button) findViewById(R.id.btnSelect);
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        myDBHelper = new myDBHelper(this);


        sttBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(intent);
            }
        });

        btnSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sqlDB = myDBHelper.getReadableDatabase();
                Cursor cursor;
                cursor = sqlDB.rawQuery("SELECT * FROM groupTBL;", null);

                String morp = "형태소" + "\r\n";

                while (cursor.moveToNext()) {
                    morp += cursor.getString(0) + "\r \n";
                }
                dbText.setText(morp);
                cursor.close();
                sqlDB.close();
            }
        });


    }

    public class myDBHelper extends SQLiteOpenHelper {
        public myDBHelper(Context context) {
            super(context, "DB", null, 1);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE groupTBL ( morp CHAR(20) PRIMARY KEY);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS groupTBL");
            onCreate(db);
        }
    }


    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            //sqlDB = myDBHelper.getWritableDatabase();
            //myDBHelper.onUpgrade(sqlDB,1,2);
            //sqlDB.close();
            Toast.makeText(getApplicationContext(), "음성인식을 시작합니다.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            String message; //에러 메세지
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }
            Toast.makeText(getApplicationContext(), "에러가 발생하였습니다. : " + message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줍니다.
            String string = "";
            //String text = "";
            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < matches.size(); i++) {
                textView.setText(matches.get(i));
                string = matches.get(i);
            }
            try {
                MorphemeAnalyzer ma = new MorphemeAnalyzer();
                ma.createLogger(null);
                List<MExpression> ret = ma.analyze(string);
                ret = ma.postProcess(ret);
                ret = ma.leaveJustBest(ret);
                //Toast.makeText(getApplicationContext(), "테스트 : " + ret , Toast.LENGTH_SHORT).show();
                List<Sentence> stl = ma.divideToSentences(ret);
                for (int i = 0; i < stl.size(); i++) {
                    Sentence st = stl.get(i);
                    //text = stl.toString();
                    //Toast.makeText(getApplicationContext(), "문장 : " + text , Toast.LENGTH_SHORT).show();

                    for (int j = 0; j < st.size(); j++) {
                        sqlDB = myDBHelper.getWritableDatabase();
                        sqlDB.execSQL("INSERT INTO groupTBL VALUES ('" + st.get(j).toString() + "');");
                        sqlDB.close();
                        //Toast.makeText(getApplicationContext(), "형태소 : " + st.get(j) , Toast.LENGTH_SHORT).show();
                    }
                }
                ma.closeLogger();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };

}