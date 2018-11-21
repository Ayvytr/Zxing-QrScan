package com.ayvytr.qrscan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;
import com.ayvytr.qrscan.camera.BitmapLuminanceSource;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by aaron on 16/7/27.
 * 二维码扫描工具类
 */
public class QrUtils {
    public static final String RESULT = "result_string";

    public static final String BARCODE_BITMAP = "barcode_bitmap";

    private static final Vector<BarcodeFormat> PRODUCT_FORMATS;
    private static final Vector<BarcodeFormat> ONE_D_FORMATS;
    private static final Vector<BarcodeFormat> QR_CODE_FORMATS;
    private static final Vector<BarcodeFormat> DATA_MATRIX_FORMATS;

    public static final Vector<BarcodeFormat> SUPPORT_FORMATS;

    static {
        PRODUCT_FORMATS = new Vector<BarcodeFormat>(5);
        PRODUCT_FORMATS.add(BarcodeFormat.UPC_A);
        PRODUCT_FORMATS.add(BarcodeFormat.UPC_E);
        PRODUCT_FORMATS.add(BarcodeFormat.EAN_13);
        PRODUCT_FORMATS.add(BarcodeFormat.EAN_8);
        ONE_D_FORMATS = new Vector<BarcodeFormat>(PRODUCT_FORMATS.size() + 4);
        ONE_D_FORMATS.addAll(PRODUCT_FORMATS);
        ONE_D_FORMATS.add(BarcodeFormat.CODE_39);
        ONE_D_FORMATS.add(BarcodeFormat.CODE_93);
        ONE_D_FORMATS.add(BarcodeFormat.CODE_128);
        ONE_D_FORMATS.add(BarcodeFormat.ITF);
        QR_CODE_FORMATS = new Vector<BarcodeFormat>(1);
        QR_CODE_FORMATS.add(BarcodeFormat.QR_CODE);
        DATA_MATRIX_FORMATS = new Vector<BarcodeFormat>(1);
        DATA_MATRIX_FORMATS.add(BarcodeFormat.DATA_MATRIX);

        SUPPORT_FORMATS = new Vector<>(PRODUCT_FORMATS.size() + ONE_D_FORMATS.size() + QR_CODE_FORMATS.size() +
                DATA_MATRIX_FORMATS.size());
        SUPPORT_FORMATS.addAll(PRODUCT_FORMATS);
        SUPPORT_FORMATS.addAll(ONE_D_FORMATS);
        SUPPORT_FORMATS.add(BarcodeFormat.QR_CODE);
        SUPPORT_FORMATS.add(BarcodeFormat.DATA_MATRIX);
    }

    /**
     * 解析二维码图片工具类
     */
    public static void decodeBitmap(String path, OnScanListener onScanListener) {

        /**
         * 首先判断图片的大小,若图片过大,则执行图片的裁剪操作,防止OOM
         */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        options.inJustDecodeBounds = false; // 获取新的大小

        int sampleSize = (int) (options.outHeight / (float) 400);

        if(sampleSize <= 0) { sampleSize = 1; }
        options.inSampleSize = sampleSize;
        Bitmap mBitmap = BitmapFactory.decodeFile(path, options);

        MultiFormatReader multiFormatReader = new MultiFormatReader();

        // 解码的参数
        Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(2);
        // 可以解析的编码类型
        hints.put(DecodeHintType.POSSIBLE_FORMATS, SUPPORT_FORMATS);
        // 设置继续的字符编码格式为UTF8
        // hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
        // 设置解析配置参数
        multiFormatReader.setHints(hints);

        // 开始对图像资源解码
        Result rawResult = null;
        try {
            rawResult = multiFormatReader
                    .decodeWithState(new BinaryBitmap(new HybridBinarizer(new BitmapLuminanceSource(mBitmap))));
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(rawResult != null) {
            if(onScanListener != null) {
                onScanListener.onSucceed(mBitmap, rawResult.getText());
            }
        } else {
            if(onScanListener != null) {
                onScanListener.onFailed();
            }
        }
    }

    /**
     * 生成二维码图片
     *
     * @param text
     * @param w
     * @param h
     * @param logo
     * @return
     */
    public static Bitmap createBitmap(String text, int w, int h, Bitmap logo) {
        if(TextUtils.isEmpty(text)) {
            return null;
        }
        try {
            Bitmap scaleLogo = getScaleLogo(logo, w, h);

            int offsetX = w / 2;
            int offsetY = h / 2;

            int scaleWidth = 0;
            int scaleHeight = 0;
            if(scaleLogo != null) {
                scaleWidth = scaleLogo.getWidth();
                scaleHeight = scaleLogo.getHeight();
                offsetX = (w - scaleWidth) / 2;
                offsetY = (h - scaleHeight) / 2;
            }
            Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            //容错级别
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            //设置空白边距的宽度
            hints.put(EncodeHintType.MARGIN, 0);
            BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, w, h, hints);
            int[] pixels = new int[w * h];
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    if(x >= offsetX && x < offsetX + scaleWidth && y >= offsetY && y < offsetY + scaleHeight) {
                        int pixel = scaleLogo.getPixel(x - offsetX, y - offsetY);
                        if(pixel == 0) {
                            if(bitMatrix.get(x, y)) {
                                pixel = 0xff000000;
                            } else {
                                pixel = 0xffffffff;
                            }
                        }
                        pixels[y * w + x] = pixel;
                    } else {
                        if(bitMatrix.get(x, y)) {
                            pixels[y * w + x] = 0xff000000;
                        } else {
                            pixels[y * w + x] = 0xffffffff;
                        }
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(w, h,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
            return bitmap;
        } catch(WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Bitmap getScaleLogo(Bitmap logo, int w, int h) {
        if(logo == null) return null;
        Matrix matrix = new Matrix();
        float scaleFactor = Math.min(w * 1.0f / 5 / logo.getWidth(), h * 1.0f / 5 / logo.getHeight());
        matrix.postScale(scaleFactor, scaleFactor);
        Bitmap result = Bitmap.createBitmap(logo, 0, 0, logo.getWidth(), logo.getHeight(), matrix, true);
        return result;
    }


  //    public static void isLightEnable(boolean isEnable) {
//        if (isEnable) {
//            Camera camera = CameraManager.get().getCamera();
//            if (camera != null) {
//                Camera.Parameters parameter = camera.getParameters();
//                parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                camera.setParameters(parameter);
//            }
//        } else {
//            Camera camera = CameraManager.get().getCamera();
//            if (camera != null) {
//                Camera.Parameters parameter = camera.getParameters();
//                parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                camera.setParameters(parameter);
//            }
//        }
//    }
}
