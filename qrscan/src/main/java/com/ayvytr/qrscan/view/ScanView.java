package com.ayvytr.qrscan.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import com.ayvytr.qrscan.R;

/**
 * This view is overlaid on top of the layout_camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author Ayvytr <a href="https://github.com/Ayvytr" target="_blank">'s GitHub</a>
 * @since 1.0.0
 */
public class ScanView extends View {

    private static final long ANIMATION_DELAY = 16L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int POINT_SIZE = 6;

    private static final int CORNER_RECT_WIDTH = 8;  //扫描区边角的宽
    private static final int CORNER_RECT_HEIGHT = 40; //扫描区边角的高
    private static final int SCANNER_LINE_MOVE_DISTANCE = 6;  //扫描线移动距离
    private static final int SCANNER_LINE_HEIGHT = 10;  //扫描线宽度

    private final Paint paint;
    private final TextPaint textPaint;
    private Bitmap resultBitmap;
    private final int maskColor;
    //扫描区域边框颜色
    private final int frameColor;
    private int frameWidth;
    //扫描线颜色
    private final int laserColor;
    //四角颜色
    private final int cornerColor;
    private final float textPadding;
    private TextLocation textLocation;
    //扫描区域提示文本
    private String labelText;
    //扫描区域提示文本颜色
    private final int labelTextColor;
    private final float labelTextSize;
    private int scannerStart = 0;
    private int scannerEnd = 0;
    private Rect frame;

    public enum TextLocation {
        TOP(0), BOTTOM(1);

        private int mValue;

        TextLocation(int value) {
            mValue = value;
        }

        private static TextLocation getFromInt(int value) {

            for (TextLocation location : TextLocation.values()) {
                if (location.mValue == value) {
                    return location;
                }
            }

            return TextLocation.TOP;
        }
    }

    // This constructor is used when the class is built from an XML resource.
    public ScanView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //初始化自定义属性信息
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ScanView);
        maskColor = array.getColor(R.styleable.ScanView_maskColor,
                ContextCompat.getColor(context, R.color.viewfinder_mask));
        frameColor = array.getColor(R.styleable.ScanView_frameColor,
                ContextCompat.getColor(context, R.color.viewfinder_frame));
        cornerColor = array.getColor(R.styleable.ScanView_cornerColor,
                ContextCompat.getColor(context, R.color.viewfinder_corner));
        laserColor = array.getColor(R.styleable.ScanView_laserColor,
                ContextCompat.getColor(context, R.color.viewfinder_laser));
        frameWidth = array.getDimensionPixelSize(R.styleable.ScanView_frameWidth, dp2px(getContext(), 200));

        labelText = array.getString(R.styleable.ScanView_text);
        labelTextColor = array.getColor(R.styleable.ScanView_textColor,
                ContextCompat.getColor(context, R.color.viewfinder_text_color));
        labelTextSize = array.getDimension(R.styleable.ScanView_textSize, TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, getResources().getDisplayMetrics()));
        textPadding = array.getDimension(R.styleable.ScanView_textPadding, TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        textLocation = TextLocation.getFromInt(array.getInt(R.styleable.ScanView_textLocation, 0));

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    public void onDraw(Canvas canvas) {
        frame = getFrame();
        if (frame == null) {
            return;
        }

        if (scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame.top;
            scannerEnd = frame.bottom - SCANNER_LINE_HEIGHT;
        }

        int width = getWidth();
        int height = getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        drawExterior(canvas, frame, width, height);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active
            // Draw a two pixel solid black border inside the framing rect
            drawFrame(canvas, frame);
            // 绘制边角
            drawCorner(canvas, frame);
            // Draw a red "laser scanner" line through the middle to show decoding is active
            drawLaserScanner(canvas, frame);
            //绘制提示信息
            drawTextInfo(canvas, frame);

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    private Rect getFrame() {
        if (frame == null) {
            frame = new Rect();
            frame.left = (getMeasuredWidth() - frameWidth) / 2;
            frame.right = frame.left + frameWidth;
            frame.top = (getMeasuredHeight() - frameWidth) / 2;
            frame.bottom = frame.top + frameWidth;
        }

        return frame;
    }

    //绘制文本
    private void drawTextInfo(Canvas canvas, Rect frame) {
        if (!TextUtils.isEmpty(labelText)) {
            textPaint.setColor(labelTextColor);
            textPaint.setTextSize(labelTextSize);
            textPaint.setTextAlign(Paint.Align.CENTER);
            StaticLayout staticLayout = new StaticLayout(labelText, textPaint, canvas.getWidth(),
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
            if (textLocation == TextLocation.BOTTOM) {
                canvas.translate(frame.left + frame.width() / 2, frame.bottom + textPadding);
                staticLayout.draw(canvas);
            } else {
                canvas.translate(frame.left + frame.width() / 2, frame.top - textPadding - staticLayout.getHeight());
                staticLayout.draw(canvas);
            }
        }

    }

    //绘制边角
    private void drawCorner(Canvas canvas, Rect frame) {
        paint.setColor(cornerColor);
        //左上
        canvas.drawRect(frame.left, frame.top, frame.left + CORNER_RECT_WIDTH, frame.top + CORNER_RECT_HEIGHT, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + CORNER_RECT_HEIGHT, frame.top + CORNER_RECT_WIDTH, paint);
        //右上
        canvas.drawRect(frame.right - CORNER_RECT_WIDTH, frame.top, frame.right, frame.top + CORNER_RECT_HEIGHT, paint);
        canvas.drawRect(frame.right - CORNER_RECT_HEIGHT, frame.top, frame.right, frame.top + CORNER_RECT_WIDTH, paint);
        //左下
        canvas.drawRect(frame.left, frame.bottom - CORNER_RECT_WIDTH, frame.left + CORNER_RECT_HEIGHT, frame.bottom,
                paint);
        canvas.drawRect(frame.left, frame.bottom - CORNER_RECT_HEIGHT, frame.left + CORNER_RECT_WIDTH, frame.bottom,
                paint);
        //右下
        canvas.drawRect(frame.right - CORNER_RECT_WIDTH, frame.bottom - CORNER_RECT_HEIGHT, frame.right, frame.bottom,
                paint);
        canvas.drawRect(frame.right - CORNER_RECT_HEIGHT, frame.bottom - CORNER_RECT_WIDTH, frame.right, frame.bottom,
                paint);
    }

    //绘制扫描线
    private void drawLaserScanner(Canvas canvas, Rect frame) {
        paint.setColor(laserColor);
        //线性渐变
        LinearGradient linearGradient = new LinearGradient(
                frame.left, scannerStart,
                frame.left, scannerStart + SCANNER_LINE_HEIGHT,
                shadeColor(laserColor),
                laserColor,
                Shader.TileMode.MIRROR);

        paint.setShader(linearGradient);
        if (scannerStart <= scannerEnd) {
            //椭圆
            RectF rectF = new RectF(frame.left + 2 * SCANNER_LINE_HEIGHT, scannerStart,
                    frame.right - 2 * SCANNER_LINE_HEIGHT, scannerStart + SCANNER_LINE_HEIGHT);
            canvas.drawOval(rectF, paint);
            scannerStart += SCANNER_LINE_MOVE_DISTANCE;
        } else {
            scannerStart = frame.top;
        }

        paint.setShader(null);
    }

    //处理颜色模糊
    public int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "20" + hax.substring(2);
        return Integer.valueOf(result, 16);
    }

    // 绘制扫描区边框 Draw a two pixel solid black border inside the framing rect
    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
        canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
        canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
        canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);
    }

    // 绘制模糊区域 Draw the exterior (i.e. outside the framing rect) darkened
    private void drawExterior(Canvas canvas, Rect frame, int width, int height) {
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public int getFrameWidth() {
        return frameWidth;
    }
}