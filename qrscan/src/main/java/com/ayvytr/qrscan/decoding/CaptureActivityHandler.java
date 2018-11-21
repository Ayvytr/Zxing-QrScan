package com.ayvytr.qrscan.decoding;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.ayvytr.qrscan.QrUtils;
import com.ayvytr.qrscan.R;
import com.ayvytr.qrscan.view.CameraView;
import com.google.zxing.Result;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author Ayvytr <a href="https://github.com/Ayvytr" target="_blank">'s GitHub</a>
 * @since 1.0.0
 */
public final class CaptureActivityHandler extends Handler {

    private final CameraView cameraView;
    private final DecodeHandler decodeThread;
    private State state;

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    public CaptureActivityHandler(CameraView cameraView) {
        this.cameraView = cameraView;
        decodeThread = new DecodeHandler(cameraView);
        decodeThread.start();

        state = State.SUCCESS;
        // Start ourselves capturing previews and decoding.
        cameraView.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == R.id.auto_focus) {
            //Log.d(TAG, "Got auto-focus message");
            // When one auto focus pass finishes, start another. This is the closest thing to
            // continuous AF. It does seem to hunt a bit, but I'm not sure what else to do.
            if (state == State.PREVIEW) {
                cameraView.requestAutoFocus(this, R.id.auto_focus);
            }
        } else if (message.what == R.id.restart_preview) {
            restartPreviewAndDecode();
        } else if (message.what == R.id.decode_succeeded) {
            state = State.SUCCESS;
            Bundle bundle = message.getData();

            /***********************************************************************/
            Bitmap barcode = bundle == null ? null :
                    (Bitmap) bundle.getParcelable(QrUtils.BARCODE_BITMAP);//���ñ����߳�

            cameraView.handleDecode((Result) message.obj, barcode);//���ؽ��
            /***********************************************************************/
        } else if (message.what == R.id.decode_failed) {
            // We're decoding as fast as possible, so when one decode fails, start another.
            state = State.PREVIEW;
            cameraView.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraView.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            decodeThread.join();
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraView.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
            cameraView.requestAutoFocus(this, R.id.auto_focus);
        }
    }

}
