package com.ayvytr.qrscan.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.ayvytr.qrscan.OnScanListener;
import com.ayvytr.qrscan.R;
import com.ayvytr.qrscan.view.CameraView;
import com.ayvytr.qrscan.view.ScanView;

/**
 * 自定义实现的扫描Fragment
 *
 * @author Ayvytr <a href="https://github.com/Ayvytr" target="_blank">'s GitHub</a>
 * @since 1.0.0
 */
public class CaptureFragment extends Fragment {
    public static final String LAYOUT_ID = "layout_id";

    private ScanView scanView;
    private CameraView cameraView;
    private OnScanListener onScanListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Bundle bundle = getArguments();
        View view = null;
        if (bundle != null) {
            int layoutId = bundle.getInt(LAYOUT_ID);
            if (layoutId != -1) {
                view = inflater.inflate(layoutId, null);
            }
        }

        if (view == null) {
            view = inflater.inflate(R.layout.fragment_capture, null);
        }

        scanView = (ScanView) view.findViewById(R.id.viewfinder_view);
        cameraView = (CameraView) view.findViewById(R.id.preview_view);
        cameraView.setOnScanListener(new OnScanListener() {
            @Override
            public void onSucceed(Bitmap mBitmap, String result) {
                onScanListener.onSucceed(mBitmap, result);
            }

            @Override
            public void onFailed() {
                onScanListener.onFailed();
            }
        });

        cameraView.setFrameWidth(scanView.getFrameWidth());
        return view;
    }

    public void setOnScanListener(OnScanListener onScanListener) {
        this.onScanListener = onScanListener;
    }
}
