package com.ayvytr.qrscan;

import android.graphics.Bitmap;

/**
 * 解析二维码结果
 */
public interface OnScanListener {

    void onSucceed(Bitmap mBitmap, String result);

    void onFailed();
}
