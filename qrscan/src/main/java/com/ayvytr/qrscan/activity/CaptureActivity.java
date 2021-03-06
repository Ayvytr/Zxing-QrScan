package com.ayvytr.qrscan.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ayvytr.qrscan.OnScanListener;
import com.ayvytr.qrscan.QrUtils;
import com.ayvytr.qrscan.R;


/**
 * 默认的二维码扫描Activity.
 *
 * @author wangdunwei
 * @since 1.0.0
 */
public class CaptureActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_camera);
        CaptureFragment captureFragment = new CaptureFragment();
        captureFragment.setOnScanListener(onScanListener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_zxing_container, captureFragment).commit();
    }

    /**
     * 二维码解析回调函数
     */
    OnScanListener onScanListener = new OnScanListener() {

        @Override
        public void onSucceed(String result) {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString(QrUtils.RESULT, result);
            resultIntent.putExtras(bundle);
            setResult(RESULT_OK, resultIntent);
            finish();
        }

        @Override
        public void onFailed() {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString(QrUtils.RESULT, "");
            resultIntent.putExtras(bundle);
            finish();
        }
    };
}