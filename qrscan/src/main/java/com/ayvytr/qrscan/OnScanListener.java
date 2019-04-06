package com.ayvytr.qrscan;

/**
 * 二维码扫描回调监听器.
 *
 * @author wangdunwei
 * @since 1.0.0
 */
public interface OnScanListener {

    /**
     * 扫描成功
     *
     * @param result 扫描结果
     */
    void onSucceed(String result);

    /**
     * 扫描失败
     */
    void onFailed();
}
