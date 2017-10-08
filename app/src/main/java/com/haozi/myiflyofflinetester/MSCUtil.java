package com.haozi.myiflyofflinetester;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

/**
 * Created by Android Studio.
 * User:  Lena.t.Yan
 * Date: 11/5/15
 * Time: 09:46
 */
public abstract class MSCUtil {

    public static final String MSC_RESULT_TYPE = "json";
    public static final String MSC_LANGUAGE = "zh_cn";
    public static final String MSC_ACCENT = "mandarin";
    public static final String MSC_VAD_BOS = "20000";
    public static final String MSC_VAD_EOS = "60000";
    public static final String MSC_ASR_PTT = "0";
    public static final String MSC_ASR_DWA = "0";
    public static final String MSC_SPEED = "50";

    public static void init(Context context) {
        StringBuffer param = new StringBuffer();
        param.append("appid="+"59d2ed51");
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE+"="+SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(context, param.toString());
        //SpeechUtility.createUtility(context, SpeechConstant.APPID + "=" + "59d2ed51");
    }

    public static boolean getRecordAuthEnable(Context context){
        PackageManager pm = context.getPackageManager();
        String packgeName = context.getPackageName();
        boolean flag = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("Android.permission.RECORD_AUDIO", packgeName));
        //boolean flag = PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)== PermissionChecker.PERMISSION_GRANTED;
        if (flag){
            //ToastUtil.showMessage("有权限");
            return true;
        }else {
            //ToastUtil.showMessage("无权限");
            return false;
        }
    }

    // 音频获取源
    public static int audioSource = MediaRecorder.AudioSource.MIC;
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    public static int sampleRateInHz = 44100;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    public static int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    public static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    public static int bufferSizeInBytes = 0;

    /**
     * 判断是是否有录音权限
     */
    public static boolean isRecordPermission(final Context context){
        bufferSizeInBytes = 0;
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        AudioRecord audioRecord =  new AudioRecord(audioSource, sampleRateInHz,channelConfig, audioFormat, bufferSizeInBytes);
        //开始录制音频
        try{
            // 防止某些手机崩溃，例如联想
            audioRecord.startRecording();
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
        /**
         * 根据开始录音判断是否有录音权限
         */
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING
                && audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
            //LogW.e("audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING : " + audioRecord.getRecordingState());
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            return false;
        }
        byte[] bytes = new byte[1024];
        int readSize = audioRecord.read(bytes, 0, 1024);
        if (readSize == AudioRecord.ERROR_INVALID_OPERATION || readSize <= 0) {
            //LogW.e("readSize illegal : " + readSize);
            return false;
        }
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
            //如果短时间内频繁检测，会造成audioRecord还未销毁完成，此时检测会返回RECORDSTATE_STOPPED状态，再去read，会读到0的size，所以此时默认权限通过
            //LogW.e("audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED ");
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            return true;
        }
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;

        return true;
    }
}
