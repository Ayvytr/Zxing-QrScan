package com.ayvytr.qrscan.decoding;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.ayvytr.qrscan.QrUtils;
import com.ayvytr.qrscan.R;
import com.ayvytr.qrscan.camera.PlanarYUVLuminanceSource;
import com.ayvytr.qrscan.view.CameraView;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

import java.util.Hashtable;

/**
 * @author wangdunwei
 */
public class DecodeHandler extends HandlerThread implements Handler.Callback {

    private final MultiFormatReader multiFormatReader;
    private Handler handler;
    private CameraView cameraView;

    private final Hashtable<DecodeHintType, Object> hints;

    public DecodeHandler(CameraView cameraView) {
        super("ZXing-Decode");
        this.cameraView = cameraView;

        hints = new Hashtable<DecodeHintType, Object>(3);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, QrUtils.SUPPORT_FORMATS);

        multiFormatReader = new MultiFormatReader();
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler(getLooper(), this);
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message.what == R.id.decode) {
            decode((byte[]) message.obj, message.arg1, message.arg2);
        } else if (message.what == R.id.quit) {
            quit();
        }
        return true;
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private void decode(byte[] data, int width, int height) {
        Result rawResult = null;

        //modify here
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
        }
        int tmp = width; // Here we are swapping, that's the difference to #11
        width = height;
        height = tmp;

        PlanarYUVLuminanceSource source = cameraView.buildLuminanceSource(rotatedData, width, height);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            rawResult = multiFormatReader.decodeWithState(bitmap);
        } catch (ReaderException re) {
            // continue
        } finally {
            multiFormatReader.reset();
        }

        if (rawResult != null) {
            Message message = Message.obtain(cameraView.getHandler(), R.id.decode_succeeded, rawResult);
            Bundle bundle = new Bundle();
            bundle.putParcelable(QrUtils.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
            message.setData(bundle);
            message.sendToTarget();
        } else {
            Message message = Message.obtain(cameraView.getHandler(), R.id.decode_failed);
            message.sendToTarget();
        }
    }

    public Handler getHandler() {
        return handler;
    }
}
