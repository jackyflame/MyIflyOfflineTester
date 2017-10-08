package com.haozi.myiflyofflinetester;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    // 语音识别对象
    private SpeechRecognizer mAsr;

    private TextView textView;

    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/test";
    private  final String GRAMMAR_TYPE_BNF = "bnf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);

        init();

        findViewById(R.id.btn_recongnize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecong();
            }
        });
        findViewById(R.id.btn_recongnize_jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,AsrDemo.class));
            }
        });
    }

    private void init(){
        // 初始化识别对象
        mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);

        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        mAsr.setParameter(SpeechConstant.RESULT_TYPE, MSCUtil.MSC_RESULT_TYPE);
        mAsr.setParameter(SpeechConstant.LANGUAGE, MSCUtil.MSC_LANGUAGE);//中文
        mAsr.setParameter(SpeechConstant.ACCENT, MSCUtil.MSC_ACCENT);//普通话


        if(SpeechConstant.TYPE_CLOUD.equals(mAsr.getParameter(SpeechConstant.ENGINE_TYPE))){
            mAsr.setParameter(SpeechConstant.VAD_BOS, MSCUtil.MSC_VAD_BOS);
            mAsr.setParameter(SpeechConstant.VAD_EOS, MSCUtil.MSC_VAD_EOS);
            mAsr.setParameter(SpeechConstant.ASR_PTT, MSCUtil.MSC_ASR_PTT);
            mAsr.setParameter(SpeechConstant.SPEED, MSCUtil.MSC_SPEED);
            mAsr.setParameter(SpeechConstant.ASR_DWA, MSCUtil.MSC_ASR_DWA);
        }
        else{
            //mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
            //设置编码类型
            mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
            //参数设为语音听写
            mAsr.setParameter(SpeechConstant.DOMAIN, "iat");
        }

        textView.setText("初始化完成");
    }

    private void startRecong(){
        if(mAsr.isListening()){
            mAsr.cancel();
            textView.setText("点击开始识别");
        }else{
            mAsr.startListening(new RecognizerListener() {
                @Override
                public void onVolumeChanged(int i, byte[] bytes) {}

                @Override
                public void onBeginOfSpeech() {
                    textView.setText("识别中");
                }

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onResult(RecognizerResult recognizerResult, boolean b) {
                    try {
                       String rst = JsonParser.parseGrammarResult(recognizerResult.getResultString());
                        textView.setText(rst);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(SpeechError speechError) {
                    if (speechError != null && speechError.getErrorDescription() != null)
                        showTip(speechError.getErrorDescription());
                    if (speechError != null) {
                        if (speechError.getErrorCode() == 2001 || speechError.getErrorCode() == 2002 || speechError.getErrorCode() == 2003) {
                            showTip("网络错误");
                        } else if (speechError.getErrorCode() == 10118) {
                            showTip(speechError.getErrorDescription());
                        } else {
                            if (!mAsr.isListening()) {
                                mAsr.cancel();
                                return;
                            }
                            showTip(speechError.getErrorCode() + speechError.getErrorDescription());
                        }
                    }
                }

                @Override
                public void onEvent(int i, int i1, int i2, Bundle bundle) {}
            });
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码："+code);
            }
        }
    };

    /**
     * 构建语法监听器。
     */
    private GrammarListener grammarListener = new GrammarListener() {

        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if(error == null){
                if (SpeechConstant.TYPE_CLOUD.equals(mAsr.getParameter(SpeechConstant.ENGINE_TYPE))) {
//                    SharedPreferences.Editor editor = mSharedPreferences.edit();
//                    if(!TextUtils.isEmpty(grammarId))
//                        editor.putString(KEY_GRAMMAR_ABNF_ID, grammarId);
//                    editor.commit();
                }
                showTip("语法构建成功：" + grammarId);
            }else{
                showTip("语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,str,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAsr.isListening()) {
            mAsr.cancel();
        }
    }

    //获取识别资源路径
    private String getResourcePath(){
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        return tempBuffer.toString();
    }
}
