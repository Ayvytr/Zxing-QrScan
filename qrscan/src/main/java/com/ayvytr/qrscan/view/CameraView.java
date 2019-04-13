package com.ayvytr.qrscan.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ayvytr.qrscan.OnScanListener;
import com.ayvytr.qrscan.core.CameraConfigurationManager;
import com.ayvytr.qrscan.core.PlanarYUVLuminanceSource;
import com.ayvytr.qrscan.core.PreviewHandler;

import java.io.IOException;

/**
 * 继承SurfaceView的CameraView.
 *
 * @author wangdunwei
 * @since 1.0.0
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private PreviewHandler handler;
    private SurfaceHolder surfaceHolder;
    private Camera camera;

    private int frameWidth = -1;
    private int frameMarginTop = -1;

    private Rect framingRect;
    private Rect framingRectInPreview;

    private boolean initialized;
    private boolean previewing;
    private boolean useOneShotPreviewCallback = true;

    private CameraConfigurationManager configManager;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        this.configManager = new CameraConfigurationManager(getContext());

        if(handler == null) {
            handler = new PreviewHandler(this);
        }
    }

    public CameraConfigurationManager getConfigManager() {
        return configManager;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            openDriver(surfaceHolder);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera(holder);
        startPreview();
        handler.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        handler.stop();
        stopPreview();
        closeDriver();
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    public void setOnScanListener(OnScanListener onScanListener) {
        if(handler != null) {
            handler.setOnScanListener(onScanListener);
        }
    }

    /**
     * Opens the layout_camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the layout_camera will draw preview frames into.
     * @throws IOException Indicates the layout_camera driver failed to open.
     */
    public void openDriver(SurfaceHolder holder) throws IOException {
        if(camera == null) {
            camera = Camera.open();
            if(camera == null) {
                throw new IOException();
            }
            camera.setPreviewDisplay(holder);

            if(!initialized) {
                initialized = true;
                configManager.initFromCameraParameters(camera);
            }
            configManager.setDesiredCameraParameters(camera);
        }
    }

    /**
     * Closes the layout_camera driver if still in use.
     */
    public void closeDriver() {
        if(camera != null) {
            camera.release();
            camera = null;
        }

    }

    /**
     * Asks the layout_camera hardware to begin drawing preview frames to the screen.
     */
    public void startPreview() {
        if(camera != null && !previewing) {
            try {
                camera.startPreview();
            } catch(Exception e) {
                e.printStackTrace();
            }

            previewing = true;
        }
    }

    public void autoFocus(Camera.AutoFocusCallback callback) {
        if(camera != null && previewing) {
            camera.autoFocus(callback);
        }
    }

    /**
     * Tells the layout_camera to stop drawing preview frames.
     */
    public void stopPreview() {
        if(camera != null && previewing) {
            camera.cancelAutoFocus();

            if(!useOneShotPreviewCallback) {
                camera.setPreviewCallback(null);
            }
            try {
                camera.stopPreview();
            } catch(Exception e) {
                e.printStackTrace();
            }
            previewing = false;
        }
    }


    public void setPreviewCallback(Camera.PreviewCallback callback) {
        if(camera != null && previewing) {
            if(useOneShotPreviewCallback) {
                camera.setOneShotPreviewCallback(callback);
            } else {
                camera.setPreviewCallback(callback);
            }
        }
    }


    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    public Rect getFramingRect() {
        try {
            Point screenResolution = configManager.getScreenResolution();
            // if (framingRect == null) {
            if(camera == null) {
                return null;
            }

            int leftOffset = (screenResolution.x - frameWidth) / 2;

            int topOffset;
            if(frameMarginTop != -1) {
                topOffset = frameMarginTop;
            } else {
                topOffset = (screenResolution.y - frameWidth) / 2;
            }
            framingRect = new Rect(leftOffset, topOffset, leftOffset + frameWidth, topOffset + frameWidth);
            // }
            return framingRect;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     */
    public Rect getFramingRectInPreview() {
        if(framingRectInPreview == null) {
            Rect rect = new Rect(getFramingRect());
            Point cameraResolution = configManager.getCameraResolution();
            Point screenResolution = configManager.getScreenResolution();
            //modify here
            rect.left = rect.left * cameraResolution.y / screenResolution.x;
            rect.right = rect.right * cameraResolution.y / screenResolution.x;
            rect.top = rect.top * cameraResolution.x / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }


    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        int previewFormat = configManager.getPreviewFormat();
        String previewFormatString = configManager.getPreviewFormatString();
        switch(previewFormat) {
            // This is the standard Android format which all devices are REQUIRED to support.
            // In theory, it's the only one we should ever care about.
            case PixelFormat.YCbCr_420_SP:
                // This format has never been seen in the wild, but is compatible as we only care
                // about the Y channel, so allow it.
            case PixelFormat.YCbCr_422_SP:
                return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                        rect.width(), rect.height());
            default:
                // The Samsung Moment incorrectly uses this variant instead of the 'sp' version.
                // Fortunately, it too has all the Y data up front, so we can read it.
                if("yuv420p".equals(previewFormatString)) {
                    return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                            rect.width(), rect.height());
                }
        }
        throw new IllegalArgumentException("Unsupported picture format: " +
                previewFormat + '/' + previewFormatString);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }
}
