package cn.com.aratek.mycamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * A basic Camera preview class
 */
@SuppressLint("ViewConstructor")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private final int mPreviewWidth;
    private final int mPreviewHeight;
    private final int mCameraId;
    private final SurfaceHolder mHolder;
    private Camera mCamera;
    private PreviewCallback mPreviewCallback;

    /**
     * @param context       上下文
     * @param cameraId      相机id
     * @param previewWidth  预览宽
     * @param previewHeight 预览高
     * @param callback      预览帧回调
     */
    public CameraPreview(Context context, int cameraId, int previewWidth, int previewHeight, PreviewCallback callback) {
        super(context);
        this.mCameraId = cameraId;
        this.mPreviewWidth = previewWidth;
        this.mPreviewHeight = previewHeight;
        this.mPreviewCallback = callback;
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        Log.d(TAG, "surfaceCreated");
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        Log.d(TAG, "surfaceDestroyed");
        releaseCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        Log.d(TAG, "surfaceChanged");

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);//8200
        mCamera.setParameters(parameters);

        // start preview with new settings
        try {
            if (mPreviewCallback != null) {
                mCamera.addCallbackBuffer(new byte[mPreviewWidth * mPreviewHeight * 3 / 2]);// 针对NV21格式
                mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        mPreviewCallback.onPreviewFrame(data, camera);
                        camera.addCallbackBuffer(data);
                    }
                });
            }

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(mCameraId); // attempt to get a Camera instance

        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public interface PreviewCallback{
        void onPreviewFrame(byte[] data, Camera camera);
    }
}
