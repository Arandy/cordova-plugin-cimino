package it.cimino;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

public class MyJavaCameraView extends PortraitCameraView implements PictureCallback {

    private static final String TAG = "Cimino::MyJavaCameraView";
    private String mPictureFileName;
    private List<Point> corners = null;

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);        
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

    public void takePicture(final String fileName, List<Point> corners) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        this.corners = corners;
        
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
        
    @SuppressLint("SimpleDateFormat")
	@Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);
            fos.write(data);
            fos.close();
            
            Bitmap inputBitmap = BitmapFactory.decodeByteArray(data,0,data.length);
            // 1) devo ruotare la bitmap di 90 gradi in senso orario
            // l'immagine è 3246x2448
            // deve diventare 2448x3246
            Matrix matrix = new Matrix();
            matrix.setRotate(90, 0, 0);
            Bitmap rotatedBitmap = Bitmap.createBitmap(inputBitmap , 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);

            // 1) devo scalare la posizione dei punti
            Iterator<Point> it = corners.iterator();
            while (it.hasNext())
            {
            	Point pt = it.next();
            	pt.x = pt.x*2448/480;
            	pt.y = pt.y*3246/800;
            }
            
            Mat inputMat = new Mat(rotatedBitmap.getHeight(), rotatedBitmap.getWidth(), CvType.CV_8UC3);
            Utils.bitmapToMat(rotatedBitmap, inputMat);

        	Mat quad = new Mat(rotatedBitmap.getHeight(), rotatedBitmap.getWidth(), CvType.CV_8UC3);    	
        	
        	Mat src_mat = Converters.vector_Point_to_Mat(corners,CvType.CV_32F);
        	// Corners of the destination image
        	List<Point> quad_pts = new ArrayList<Point>();
        	quad_pts.add(new Point(0, 0));
        	quad_pts.add(new Point(quad.cols(), 0));
        	quad_pts.add(new Point(quad.cols(), quad.rows()));
        	quad_pts.add(new Point(0, quad.rows()));
        	Mat dst_mat = Converters.vector_Point_to_Mat(quad_pts,CvType.CV_32F);

        	// Get transformation matrix               	
        	Mat transmtx = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        	// Apply perspective transformation
        	Imgproc.warpPerspective(inputMat, quad, transmtx, quad.size());
            // Write the image in a file (in jpeg format)
        	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String currentDateandTime = sdf.format(new Date());
        	
        	Highgui.imwrite(Environment.getExternalStorageDirectory().getPath()+"/resultImage_"+currentDateandTime+".jpg", quad);     
        } catch (java.io.IOException e) {
            Log.e(TAG, "Exception in photoCallback", e);
        }
    }
}
