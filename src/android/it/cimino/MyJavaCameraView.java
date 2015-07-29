package it.cimino;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;

@SuppressWarnings("deprecation")
public class MyJavaCameraView extends PortraitCameraView implements PictureCallback {

    private static final String TAG = "Cimino::MyJavaCameraView";
    private String mPictureFileName;
    private byte[] takenImageData;
    private CameraCaptureActivity parent;

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);        
    }
    
    public void setDefaultParameters()
    {
    	mCamera.enableShutterSound(true);
    	
    	Log.d(TAG,"Antibanding: "+mCamera.getParameters().getAntibanding());
    	Log.d(TAG,"AutoExposureLock: "+mCamera.getParameters().getAutoExposureLock());
    	Log.d(TAG,"ExposureCompensation: "+mCamera.getParameters().getExposureCompensation());
    	Log.d(TAG,"FocalLength: "+mCamera.getParameters().getFocalLength());
    	/*if(mCamera.getParameters().isVideoStabilizationSupported())
    	{
    		mCamera.getParameters().setVideoStabilization(true);
    	}
    	if(mCamera.getParameters().isAutoExposureLockSupported())
    	{
    		mCamera.getParameters().setAutoExposureLock(true);
    	}
    	if(mCamera.getParameters().isAutoWhiteBalanceLockSupported())
    	{
    		mCamera.getParameters().setAutoWhiteBalanceLock(true);
    	}
    	*/
    	mCamera.getParameters().setSceneMode(Camera.Parameters.SCENE_MODE_BARCODE);
    }
    
    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPictureSize();
    }

    public void initParent(CameraCaptureActivity parent)
    {
    	this.parent = parent;
    }
    
    public void takePicture(final String fileName ) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }
    
    public void FlashlightON()
    {
    	if(mCamera!=null)
    	{
        	Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(Parameters.FLASH_MODE_ON);
            mCamera.setParameters(params);
    	}
    }

    public void FlashlightOFF()
    {
    	if(mCamera!=null)
    	{
	    	Camera.Parameters params = mCamera.getParameters();
	        params.setFlashMode(Parameters.FLASH_MODE_OFF);
	        mCamera.setParameters(params);
    	}
    }
    
    public void initAutofocusCallback()
    {
    	mCamera.autoFocus(parent.autofocusCallback);
    }
    
    public void setFocusArea(List<Camera.Area> focusAreas)
    {
    	if (mCamera.getParameters().getMaxNumFocusAreas()>0)
    	{
        	mCamera.getParameters().setFocusAreas(focusAreas);
    	}
    }
    
    public byte[] getData()
    {
    	return takenImageData;
    }
    
    public void submitFocusAreaRect(final Rect touchRect)
    {    	
        Camera.Parameters cameraParameters = mCamera.getParameters();

        if (cameraParameters.getMaxNumFocusAreas() == 0)
        {
            return;
        }

        // Convert from View's width and height to +/- 1000
 
        Rect focusArea = new Rect();

        focusArea.set(touchRect.left * 2000 / this.getWidth() - 1000, 
                          touchRect.top * 2000 / this.getHeight() - 1000,
                          touchRect.right * 2000 / this.getWidth() - 1000,
                          touchRect.bottom * 2000 / this.getHeight() - 1000);

        // Submit focus area to camera

        ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
        focusAreas.add(new Camera.Area(focusArea, 1000));

        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        cameraParameters.setFocusAreas(focusAreas);
        mCamera.setParameters(cameraParameters);

        // Start the autofocus operation

        initAutofocusCallback();
    }
    
    @SuppressLint("SimpleDateFormat")
	@Override
    public void onPictureTaken(byte[] data, Camera camera) {
        // The camera preview was automatically stopped. Start it again.
        Log.d(TAG, "----- init takenImageData");
        this.takenImageData = data;
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        // Write the image in a file (in jpeg format)
        try {
            Log.d(TAG, "----- Saving a bitmap to file");
            FileOutputStream fos = new FileOutputStream(mPictureFileName);
            fos.write(data);
            fos.close();            
            parent.doBackgroundTask();
            
        } catch (java.io.IOException e) {
            Log.e(TAG, "Exception in photoCallback", e);
        }
    }
    
}
