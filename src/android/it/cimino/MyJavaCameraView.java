package it.cimino;

import it.cimino.CameraCaptureActivity.AutoFocusCallBackImpl;
import it.ciminotrack.R;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
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
    	//mCamera.enableShutterSound(true);
    	
    	Log.d(TAG,"Antibanding: "+mCamera.getParameters().getAntibanding());
    	Log.d(TAG,"AutoExposureLock: "+mCamera.getParameters().getAutoExposureLock());
    	Log.d(TAG,"ExposureCompensation: "+mCamera.getParameters().getExposureCompensation());
    	Log.d(TAG,"FocalLength: "+mCamera.getParameters().getFocalLength());
    	if(mCamera.getParameters().isVideoStabilizationSupported())
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
    	
    	//mCamera.getParameters().setSceneMode(Camera.Parameters.SCENE_MODE_PARTY);
    	//Camera.Parameters.SCENE_MODE_PARTY
    }
    
    public Camera.Parameters getParameters()
    {
    	return mCamera.getParameters();
    }
    
    public void setParameters(Camera.Parameters params)
    {
    	mCamera.setParameters(params);
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

    public int getOrientation()
    {
    	return rotation;
    }
    
    public void initParent(CameraCaptureActivity parent)
    {
    	this.parent = parent;
    }
    
    public void takePicture(final String fileName ) {
        	
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        
        //mCamera.stopPreview();
        
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        //mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }
    
    public void FlashlightON()
    {
    	if(mCamera!=null)
    	{
        	Camera.Parameters params = mCamera.getParameters();
        	try{
	            params.setFlashMode(Parameters.FLASH_MODE_TORCH);
	            mCamera.setParameters(params);
        	}catch(Exception e){
        		Log.d(TAG, "FLASH_MODE_TORCH unsupported");
        	}
    	}
    }

    public void FlashlightOFF()
    {
    	if(mCamera!=null)
    	{
    		try{
		    	Camera.Parameters params = mCamera.getParameters();
		        params.setFlashMode(Parameters.FLASH_MODE_OFF);
		        mCamera.setParameters(params);
	    	}catch(Exception e){
	    		Log.d(TAG, "FLASH_MODE_OFF unsupported");
	    	}	        
    	}
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
            parent.bIsPictureTaking = false;            
        } catch (java.io.IOException e) {
            Log.e(TAG, "Exception in photoCallback", e);
        }
    }

	public void autoFocus(AutoFocusCallBackImpl autoFocusCallBack)
	{
    	mCamera.autoFocus(autoFocusCallBack);		
	}
    
}
