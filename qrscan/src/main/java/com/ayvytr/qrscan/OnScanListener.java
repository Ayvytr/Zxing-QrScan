package com.ayvytr.qrscan;

import android.graphics.Bitmap;

/**
 * 二维码扫描回调监听器.
 *
 * @author Ayvytr <a href="https://github.com/Ayvytr" target="_blank">'s GitHub</a>
 * @since 1.0.0
 */
public interface OnScanListener {

    /**
     * 扫描成功
     *
     * @param mBitmap 扫描的Bitmap图像
     * @param result  扫描结果
     */
    void onSucceed(Bitmap mBitmap, String result);

    /**
     * 扫描失败
     */
    void onFailed();
}
