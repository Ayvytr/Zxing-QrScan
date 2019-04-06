package com.ayvytr.qrscan.core;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ayvytr.qrscan.OnScanListener;
import com.ayvytr.qrscan.QrUtils;
import com.ayvytr.qrscan.view.CameraView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Hashtable;

/**
 * This class handles all the messaging which comprises the state machine for preview.
 *
 * @author wangdunwei
 * @since 1.0.0
 */
public final class PreviewHandler extends Handler {

    private static final int PREVIEW_DELAY_MILLIS = 1000;

    private static final int MSG_PREVIEW = 0;
    private static final int MSG_SUCCEED = 1;
    private static final int MSG_FAILED = -1;
    private static final int MSG_TIMING = 2;

    private final CameraView cameraView;

    /**
     * 当扫码成功一次时，是否继续识别扫码，默认停止识别
     *
     * 注意：
     * 直接停止有漏洞，如果页面回调直接关闭页面将会拿不到结果。这里设计为超过1分钟扫描失败，直接终止识别，一分钟内，扫描失败继续识别
     *
     * @code true 继续识别
     * @code false 停止识别
     */
    private boolean repeatOnSucceed;

    private final MultiFormatReader multiFormatReader;
    private final Hashtable<DecodeHintType, Object> hints;

    private OnScanListener onScanListener;


    public PreviewHandler(CameraView cameraView) {
        this(cameraView, false);
    }

    public PreviewHandler(CameraView cameraView, boolean repeatOnSucceed) {
        this.cameraView = cameraView;
        this.repeatOnSucceed = repeatOnSucceed;
        hints = new Hashtable<DecodeHintType, Object>(3);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, QrUtils.SUPPORT_FORMATS);

        multiFormatReader = new MultiFormatReader();
    }

    public void start() {
        preview();
        registerAbortMessage();
    }

    private void registerAbortMessage() {
        sendEmptyMessageDelayed(MSG_TIMING, 60000);
    }

    private void preview() {
        cameraView.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                cameraView.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        Log.e("CAH.onPreviewFrame", data + "");
                        decode(data);
                    }
                });
            }
        });
        sendEmptyMessageDelayed(MSG_PREVIEW, PREVIEW_DELAY_MILLIS);
    }

    @Override
    public void handleMessage(Message message) {
        switch(message.what) {
            case MSG_PREVIEW:
                preview();
                break;
            case MSG_SUCCEED:
                onSucceed(message);
                break;
            case MSG_FAILED:
                onFailed();
                break;
            case MSG_TIMING:
                performAbort();
            default:
                break;
        }
    }

    private void performAbort() {
        stop();
        failedNow();
    }

    private void failedNow() {
        if(onScanListener != null) {
            onScanListener.onFailed();
        }
    }

    public void setOnScanListener(OnScanListener onScanListener) {
        this.onScanListener = onScanListener;
    }

    private void onFailed() {
        Log.e("CAH.onFailed", "onFailed");
    }

    private void onSucceed(Message message) {
        Log.e("CAH.onSucceed", ((Result) message.obj).getText());
        if(onScanListener != null) {
            onScanListener.onSucceed(((Result) message.obj).getText());
        }

        if(!repeatOnSucceed) {
            stop();
        }
    }

    public void stop() {
        // Be absolutely sure we don't send any queued up messages
        removeMessages(MSG_SUCCEED);
        removeMessages(MSG_FAILED);
        removeMessages(MSG_PREVIEW);
        removeMessages(MSG_TIMING);
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data The YUV preview frame.
     */
    private void decode(byte[] data) {
        int width = cameraView.getConfigManager().getCameraResolution().x;
        int height = cameraView.getConfigManager().getCameraResolution().y;

        //modify here
        byte[] rotatedData = new byte[data.length];
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
        }
        int tmp = width; // Here we are swapping, that's the difference to #11
        width = height;
        height = tmp;

        Log.e("PreviewHandler.decode", width + " " + height);

        PlanarYUVLuminanceSource source = cameraView.buildLuminanceSource(rotatedData, width, height);

        Result rawResult = null;
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            rawResult = multiFormatReader.decodeWithState(bitmap);
        } catch(Exception e) {
            // continue
            return;
        } finally {
            multiFormatReader.reset();
        }

        if(rawResult != null) {
            Message message = Message.obtain(this, MSG_SUCCEED, rawResult);
            message.obj = rawResult;
            message.sendToTarget();
        } else {
            Message message = Message.obtain(this, MSG_FAILED);
            message.sendToTarget();
        }
    }
}
