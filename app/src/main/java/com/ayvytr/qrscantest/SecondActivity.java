package com.ayvytr.qrscantest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import com.ayvytr.qrscan.OnScanListener;
import com.ayvytr.qrscan.QrUtils;
import com.ayvytr.qrscan.activity.CaptureFragment;



/**
 * 定制化显示扫描界面
 */
public class SecondActivity extends BaseActivity {

    private CaptureFragment captureFragment;

    /**
     * 为CaptureFragment设置layout参数
     *
     * @param captureFragment
     * @param layoutId
     */
    public void setFragmentArgs(CaptureFragment captureFragment, int layoutId) {
        if(captureFragment == null || layoutId == -1) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putInt(CaptureFragment.LAYOUT_ID, layoutId);
        captureFragment.setArguments(bundle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        captureFragment = new CaptureFragment();
        // 为二维码扫描界面设置定制化界面
        setFragmentArgs(captureFragment, R.layout.my_camera);
        captureFragment.setOnScanListener(onScanListener);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_my_container, captureFragment).commit();

        initView();
    }

    public static boolean isOpen = false;

    private void initView() {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linear1);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOpen) {
//                    QrUtils.isLightEnable(true);
                    isOpen = true;
                } else {
//                    QrUtils.isLightEnable(false);
                    isOpen = false;
                }

            }
        });
    }


    /**
     * 二维码解析回调函数
     */
    OnScanListener onScanListener = new OnScanListener() {
        @Override
        public void onSucceed(Bitmap mBitmap, String result) {
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
