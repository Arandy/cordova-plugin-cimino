package it.cimino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

@SuppressWarnings("deprecation")
public class PortraitCameraView extends CameraBridgeViewBase implements PreviewCallback {

private static final int MAGIC_TEXTURE_ID = 10;
private static final String TAG = "JavaCameraView";

private byte mBuffer[];
private Mat[] mFrameChain;
private int mChainIdx = 0;
private Thread mThread;
private boolean mStopThread;

protected Camera mCamera;
protected JavaCameraFrame[] mCameraFrame;
private SurfaceTexture mSurfaceTexture;
//private int mCameraId;
public Preview mPreview;
public Size frameSize;
protected int rotation = 0;

protected class Preview extends ViewGroup implements SurfaceHolder.Callback {

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;

    Preview(Context context) {
        super(context);

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		// Now that the size is known, set up the camera parameters and begin
	    // the preview.
	    Camera.Parameters parameters = mCamera.getParameters();
		List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
	    parameters.setPreviewFormat(ImageFormat.NV21);

	    Size frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), height, width); //use turn around values here to get the correct prev size for portrait mode
//        if (rotation>0)
//        {
//    	    frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), width, height); //use turn around values here to get the correct prev size for portrait mode
//        }
        Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) + "x" + Integer.valueOf((int)frameSize.height));
        parameters.setPreviewSize((int)frameSize.width, (int)frameSize.height);
        
	    requestLayout();
	    mCamera.setParameters(parameters);

	    // Important: Call startPreview() to start updating the preview surface.
	    // Preview must be started before you can take a picture.
	    mCamera.startPreview();
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		// TODO Auto-generated method stub
		
	}

}

public static class JavaCameraSizeAccessor implements ListItemAccessor {

    public int getWidth(Object obj) {
        Camera.Size size = (Camera.Size) obj;
        return size.width;
    }

    public int getHeight(Object obj) {
        Camera.Size size = (Camera.Size) obj;
        return size.height;
    }
}

public PortraitCameraView(Context context, int cameraId) {
    super(context, cameraId);
}

public PortraitCameraView(Context context, AttributeSet attrs) {
    super(context, attrs);
}

@SuppressWarnings("unused")
public android.hardware.Camera.Size getTopResolution(List<Camera.Size> mResolutionList, int width,int height, int rotation)
{
	android.hardware.Camera.Size optimalSize = null;
	
    Object[] mResArr = mResolutionList.toArray();
    int max_width = 0;
    
    for (int i=0; i<mResArr.length; i++)
    {
    	Camera.Size element = (Camera.Size) mResArr[i];
    	if (max_width<element.width)
    	{
    	optimalSize = element;
        	max_width = element.width;
    	}
//    	break;
    }
            
    return optimalSize;
}


public android.hardware.Camera.Size getUpperClosestResolution(List<Camera.Size> mResolutionList, int width,int height, int rotation)
{
	android.hardware.Camera.Size optimalSize = null;
	
    Object[] mResArr = mResolutionList.toArray();
    
    for (int i=0; i<mResArr.length; i++)
    {
    	Camera.Size element = (Camera.Size) mResArr[i];
    	int e_width = element.width;
    	int e_height = element.height;
        if(rotation==0)
        {
        	e_height = element.width;
        	e_width = element.height;
        }
        
    	if (e_width<width && e_height<height)
    	{
    		break;
    	}        	
    	optimalSize = element;
    }
            
    return optimalSize;
}

public android.hardware.Camera.Size getLowerClosestResolution(List<Camera.Size> mResolutionList, int width,int height, int rotation)
{
	android.hardware.Camera.Size optimalSize = null;

    ArrayList<Object> tempElements = new ArrayList<Object>();
    tempElements.addAll(mResolutionList);
	Collections.reverse(tempElements);

	Object[] mResArr = tempElements.toArray();
    
    for (int i=0; i<mResArr.length; i++)
    {
    	Camera.Size element = (Camera.Size) mResArr[i];
    	int e_width = element.width;
    	int e_height = element.height;
        if(rotation==0)
        {
        	e_height = element.width;
        	e_width = element.height;
        }
        
    	if (e_width>=width && e_height>=height)
    	{
    		break;
    	}        	
    	optimalSize = element;
    }
            
    return optimalSize;
}


protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize java camera "+width+"x"+height);
    boolean result = true;
    synchronized (this) {
        mCamera = null;
        mPreview = new Preview(this.getContext());
        
        boolean connected = false;
        int numberOfCameras = android.hardware.Camera.getNumberOfCameras();
        android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            android.hardware.Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    mCamera = Camera.open(i);
                    //mCameraId = i;
                    connected = true;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera #" + i + "failed to open: " + e.getMessage());
                }
                if (connected) break;
            }
        }

            if (mCamera == null)
                return false;

        /* Now set camera parameters */
        try {
            Camera.Parameters params = mCamera.getParameters();
            Log.d(TAG, "getSupportedPreviewSizes()");
                List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();

            if (sizes != null) {
                WindowManager wm = (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                //Point displaySize = new Point();
                //display.getSize(displaySize);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                display.getMetrics(displayMetrics);
                
                rotation = display.getRotation();
                Log.d(TAG,"Rotation: "+rotation);
                
                /* Select the size that fits surface considering maximum size allowed */
                if (rotation==0)
                {
                frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), height, width); //use turn around values here to get the correct prev size for portrait mode
                }
                else
                {
                    frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), width, height);
                }

                params.setPreviewFormat(ImageFormat.NV21);
                Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) + "x" + Integer.valueOf((int)frameSize.height));
                params.setPreviewSize((int)frameSize.width, (int)frameSize.height);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100"))
                    params.setRecordingHint(true);

                List<String> FocusModes = params.getSupportedFocusModes();
                if (FocusModes != null)
              	{
                	/*if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                	{
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        Log.d(TAG, "Set focus mode OK (FOCUS_MODE_CONTINUOUS_PICTURE)");
                	}                
                	else*/ if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
	                {
                        Log.d(TAG, "Set focus mode OK (FOCUS_MODE_CONTINUOUS_VIDEO)");
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
	                }
                	else if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                	{
                		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        Log.d(TAG, "Set focus mode OK (FOCUS_MODE_AUTO)");
                	}
                	else
                	{
                		params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                        Log.d(TAG, "Set focus mode OK (FOCUS_MODE_FIXED)");                		
                	}
                }

                mCamera.setParameters(params);
                params = mCamera.getParameters();
                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
                Camera.Size optimalSize = this.getTopResolution(params.getSupportedPictureSizes(), width, height, rotation);
                params.setPictureSize(optimalSize.width,optimalSize.height);
                Log.d(TAG, "Set picture size to " + optimalSize.width + "x" + optimalSize.height);                
                mCamera.setParameters(params);

                mFrameWidth = params.getPreviewSize().height; //the frame width and height of the super class are used to generate the cached bitmap and they need to be the size of the resulting frame
                mFrameHeight = params.getPreviewSize().width;

                int realWidth = mFrameHeight; //the real width and height are the width and height of the frame received in onPreviewFrame
                int realHeight = mFrameWidth;

                if (rotation>0){
                    mFrameWidth = params.getPreviewSize().width; 
                    mFrameHeight = params.getPreviewSize().height;
                    realWidth = mFrameWidth; 
                    realHeight = mFrameHeight;
                }

                if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT))
                    mScale = Math.min(((float)height)/mFrameHeight, ((float)width)/mFrameWidth);
                else
                    mScale = 0;

                if (mFpsMeter != null) {
                    mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                }

                int size = mFrameWidth * mFrameHeight;
                size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                mBuffer = new byte[size];

                mCamera.addCallbackBuffer(mBuffer);
                mCamera.setPreviewCallbackWithBuffer(this);

                mFrameChain = new Mat[2];
                mFrameChain[0] = new Mat(realHeight + (realHeight/2), realWidth, CvType.CV_8UC1); //the frame chane is still in landscape
                mFrameChain[1] = new Mat(realHeight + (realHeight/2), realWidth, CvType.CV_8UC1);

                AllocateCache();

                mCameraFrame = new JavaCameraFrame[2];
                mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight); //the camera frame is in portrait
                mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                    mCamera.setPreviewTexture(mSurfaceTexture);
                } 
                else
                {
                    if (rotation==0)
                {
                	requestLayout();
                	mCamera.setPreviewDisplay(mPreview.mHolder);
                }
                    else
                    {
                        mCamera.setPreviewDisplay(null);
                    }
                }
                /* Finally we are ready to start the preview */
                Log.d(TAG, "startPreview");
                mCamera.startPreview();
            }
            else
                result = false;
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        }
    }

    return result;
}

protected void releaseCamera() {
    synchronized (this) {
        if (mCamera != null) {
            	Parameters params = mCamera.getParameters();
            	params.setFlashMode(Parameters.FLASH_MODE_OFF);
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);

            mCamera.release();
        }
        mCamera = null;
        if (mFrameChain != null) {
            mFrameChain[0].release();
            mFrameChain[1].release();
        }
        if (mCameraFrame != null) {
            mCameraFrame[0].release();
            mCameraFrame[1].release();
        }
    }
}

@Override
protected boolean connectCamera(int width, int height) {

    /* 1. We need to instantiate camera
     * 2. We need to start thread which will be getting frames
     */
    /* First step - initialize camera connection */
    Log.d(TAG, "Connecting to camera");
        
    if (!initializeCamera(width, height))
        return false;

    /* now we can start update thread */
    Log.d(TAG, "Starting processing thread");
    mStopThread = false;
    mThread = new Thread(new CameraWorker());
    mThread.start();

    return true;
}

    @Override
protected void disconnectCamera() {
    /* 1. We need to stop thread which updating the frames
     * 2. Stop camera and release it
     */
    Log.d(TAG, "Disconnecting from camera");
    try {
        mStopThread = true;
        Log.d(TAG, "Notify thread");
        synchronized (this) {
            this.notify();
        }
        Log.d(TAG, "Wating for thread");
        if (mThread != null)
            mThread.join();
    } catch (InterruptedException e) {
        e.printStackTrace();
    } finally {
        mThread =  null;
    }

    /* Now release camera */
    releaseCamera();
}

    @Override
public void onPreviewFrame(byte[] frame, Camera arg1) {
    //Log.d(TAG, "Preview Frame received. Frame size: " + frame.length);
    synchronized (this) {
        mFrameChain[1 - mChainIdx].put(0, 0, frame);
        this.notify();
    }
    if (mCamera != null)
        mCamera.addCallbackBuffer(mBuffer);
}

private class JavaCameraFrame implements CvCameraViewFrame {
    private Mat mYuvFrameData;
    private Mat mRgba;
    private int mWidth;
    private int mHeight;
    private Mat mRotated;

    @Override
    public Mat gray() {
    	if (rotation==0)
    	{
	        if (mRotated != null) mRotated.release();
	        mRotated = mYuvFrameData.submat(0, mWidth, 0, mHeight); //submat with reversed width and height because its done on the landscape frame
	        mRotated = mRotated.t();
	        Core.flip(mRotated, mRotated, 1);
	        return mRotated;
	    }
    	else
    	{
    		return mYuvFrameData.submat(0, mHeight, 0, mWidth);
    	}
    }

        @Override
    public Mat rgba() {
        if (rotation==0)
        {
	        Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
	        if (mRotated != null) mRotated.release();
	        mRotated = mRgba.t();
	        Core.flip(mRotated, mRotated, 1);
	        return mRotated;
	    }
        else
        {
        	Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
        	if (mRotated != null) mRotated.release();
            /*
            mRotated = mRgba.clone();
            Core.flip(mRotated, mRotated, -1);
            return mRotated;
            */
            return mRgba;
        }
    }

    public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
        super();
        mWidth = width;
        mHeight = height;
        mYuvFrameData = Yuv420sp;
        mRgba = new Mat();
    }

    public void release() {
        mRgba.release();
        if (mRotated != null) mRotated.release();
    }


};

private class CameraWorker implements Runnable {

        @Override
    public void run() {
        do {
            synchronized (PortraitCameraView.this) {
                try {
                    PortraitCameraView.this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "CameraWorker interrupted", e);
                }
            }

            if (!mStopThread) {
                if (!mFrameChain[mChainIdx].empty())
                    deliverAndDrawFrame(mCameraFrame[mChainIdx]);
                mChainIdx = 1 - mChainIdx;
            }
        } while (!mStopThread);
        Log.d(TAG, "Finish processing thread");
    }
}
}