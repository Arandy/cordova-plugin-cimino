package it.cimino;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import com.ionicframework.ciminoapp963035.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
import android.view.MotionEvent;
//import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

public class CameraCaptureActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "Cimino::CameraCapture";

    private MyJavaCameraView mOpenCvCameraView;
//    private List<android.hardware.Camera.Size> mResolutionList;
//    private MenuItem[] mEffectMenuItems;
//    private SubMenu mColorEffectsMenu;
//    private MenuItem[] mResolutionMenuItems;
//    private SubMenu mResolutionMenu;
    private int rows = 0;
    private int cols = 0;
    private List<Point> corners = null;
    
    private Point computeIntersect(double[] a, double[] b)
    {
    	double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
    	double x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
        double d = ((float)(x1-x2) * (y3-y4)) - ((y1-y2) * (x3-x4));
        		
        if (d>0)
        {
            Point pt = new Point();
            pt.x = ((x1*y2 - y1*x2) * (x3-x4) - (x1-x2) * (x3*y4 - y3*x4)) / d;
            pt.y = ((x1*y2 - y1*x2) * (y3-y4) - (y1-y2) * (x3*y4 - y3*x4)) / d;
            return pt;
        }
        else
            return new Point(-1, -1);
    }
    
    /*private Point[] sortCorners(Point[] corners, Point center)
    {
        Point[] top = new Point[2];
        Point[] bot = new Point[2];
        Point[] result = new Point[4];

        int tcount = 0;
        int bcount = 0;
        
        for (int i = 0; i < corners.length; i++)
        {
        	Point pt = new Point(corners[i].x,corners[i].y);
            if (corners[i].y < center.y)
                top[tcount++] = pt;
            else
            	bot[bcount++] = pt;
        }

        Point tl = top[0].x > top[1].x ? top[1] : top[0];
        Point tr = top[0].x > top[1].x ? top[0] : top[1];
        Point bl = bot[0].x > bot[1].x ? bot[1] : bot[0];
        Point br = bot[0].x > bot[1].x ? bot[0] : bot[1];

        result[0] = tl;
        result[0] = tr;
        result[0] = br;
        result[0] = bl;
        
        return result;
    }*/
    
    @SuppressLint("ClickableViewAccessibility")
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(CameraCaptureActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraCaptureActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.camera_capture_surface_view);

        mOpenCvCameraView = (MyJavaCameraView) findViewById(R.id.camera_capture_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.disableFpsMeter();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
        {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
        	mOpenCvCameraView.FlashlightOFF();
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Mat mRgbaT = inputFrame.rgba();
    	
    	Size sizeRgba = mRgbaT.size();
    	rows = (int) sizeRgba.height;
        cols = (int) sizeRgba.width;

    	Mat lines = new Mat();
        int threshold = 70;
        int minLineSize = 30;
        int lineGap = 10;
        
        Mat gray = new Mat();
        Mat blur = new Mat();
        //Mat thresh = new Mat();
        Mat edged = new Mat();
        
        Imgproc.cvtColor(mRgbaT, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, blur, new Size(5, 5), 5);
//        Imgproc.threshold(blur, thresh, 0, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
//      Imgproc.medianBlur(thresh, thresh, 11);
//      Core.addWeighted(thresh, 1.5, thresh, -0.5, 0, thresh);
        Imgproc.Canny( blur, edged, (double)50, (double)200, 3, true);
        Imgproc.HoughLinesP( edged, lines, 1, Math.PI / 180, threshold, minLineSize, lineGap);

        /*
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(edged, contours, mHierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
        
        Log.d(TAG,"contours found: "+(contours.size()));
        Iterator<MatOfPoint> each = contours.iterator();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        while (each.hasNext()) {
        	MatOfPoint contour = each.next();
        	MatOfPoint2f mMOP2f1 = new MatOfPoint2f(); 
        	contour.convertTo(mMOP2f1, CvType.CV_32FC2);
            double peri  = Imgproc.arcLength( mMOP2f1, true);
            
            Imgproc.approxPolyDP(mMOP2f1, approxCurve, 0.02 * peri, true);
            
            // if our approximated contour has four points, then we
        	// can assume that we have found our screen
            Log.d(TAG,"approxCurve size: "+(approxCurve.cols()));
        	if (approxCurve.cols() == 4)
        	{
        		List<MatOfPoint> screenCnt = new ArrayList<MatOfPoint>();
        		screenCnt.add(new MatOfPoint(approxCurve.toArray()));
        		Imgproc.drawContours(mRgbaT, screenCnt, -1, new Scalar(0,255,0), 2);
        		
        		break;
        	}
        		
        }
   */
        
        //Log.d(TAG,"Lines: "+(lines.cols()));
        if (lines.cols()>0)
        {
        	
            for (int x = 0; x < lines.cols(); x++) {

                double[] vec = lines.get(0, x);
                
                double[] val = new double[4];
                val[0] = 0;
                val[1] = ((float) vec[1] - vec[3]) / (vec[0] - vec[2]) * -vec[0] + vec[1];
                val[2] = cols;
                val[3] = ((float) vec[1] - vec[3]) / (vec[0] - vec[2]) * (cols - vec[2]) + vec[3];
                lines.put(0, x, val);
                
                /*double x1 = val[0], 
                        y1 = val[1],
                        x2 = val[2],
                        y2 = val[3];
                Point start = new Point(x1, y1);
                Point end = new Point(x2, y2);
                
                Core.line(mRgbaT, start, end, new Scalar(255,255,0), 3);
                */

            }
            
            corners = new ArrayList<Point>();
            Point corner00 = new Point();
            Point cornerX0 = new Point();
            Point corner0Y = new Point();
            Point cornerXY = new Point();
            
            int count00 = 0;
            int countX0 = 0;
            int count0Y = 0;
            int countXY = 0;
            
            Core.circle(mRgbaT, new Point(0,0), 100, new Scalar(127,127,0));
            Core.circle(mRgbaT, new Point(cols,0), 100, new Scalar(127,127,0));
            Core.circle(mRgbaT, new Point(0,rows), 100, new Scalar(127,127,0));
            Core.circle(mRgbaT, new Point(cols,rows), 100, new Scalar(127,127,0));
            
            for (int i = 0; i < lines.cols(); i++)
            {
                for (int j = i+1; j < lines.cols(); j++)
                {
                	Point pt = computeIntersect(lines.get(0,i), lines.get(0, j));
                    if (pt.x >= 0 && pt.y >= 0)
                    {
                        double distance00 = Math.sqrt(Math.pow(pt.x, 2)+Math.pow(pt.y, 2));                        
                        double distanceX0 = Math.sqrt(Math.pow(pt.x-cols, 2)+Math.pow(pt.y, 2));
                        double distance0Y = Math.sqrt(Math.pow(pt.x, 2)+Math.pow(pt.y-rows, 2));
                        double distanceXY = Math.sqrt(Math.pow(pt.x-cols, 2)+Math.pow(pt.y-rows, 2));
                        
                    	if (distance00<100)
                    	{
                    		corner00.x += pt.x;
                    		corner00.y += pt.y;
                    		count00++;
                    	}
                    	if (distanceX0<100)
                    	{
                    		cornerX0.x += pt.x;
                    		cornerX0.y += pt.y;
                    		countX0++;
                    	}
                    	if (distance0Y<100)
                    	{
                    		corner0Y.x += pt.x;
                    		corner0Y.y += pt.y;
                    		count0Y++;
                    	}                    		
                    	if (distanceXY<100)
                    	{
                    		cornerXY.x += pt.x;
                    		cornerXY.y += pt.y;
                    		countXY++;
                    	}
                    }
                }
            }
            if (count00>0) 
        	{
            	corner00.x /= count00;  
            	corner00.y /= count00;  
                corners.add(corner00);
            	Core.circle(mRgbaT, corner00, 15, new Scalar(0,0,255));
        	}
            if (countX0>0)
            {
            	cornerX0.x /= countX0;
            	cornerX0.y /= countX0;
            	corners.add(cornerX0);
            	Core.circle(mRgbaT, cornerX0, 15, new Scalar(0,0,255));
            }
            if (countXY>0) 
            {
            	cornerXY.x /= countXY;
            	cornerXY.y /= countXY;
            	corners.add(cornerXY);
            	Core.circle(mRgbaT, cornerXY, 15, new Scalar(0,0,255));
            }
            if (count0Y>0) 
            {
            	corner0Y.x /= count0Y;  
            	corner0Y.y /= count0Y;  
            	corners.add(corner0Y);
            	Core.circle(mRgbaT, corner0Y, 15, new Scalar(0,0,255));
            }
            
            Point[] corners2f = (Point[]) corners.toArray(new Point[corners.size()]);

            Log.d(TAG,"corners found: "+(corners2f.length));
            if (corners2f.length==4)
            {
            	Core.line(mRgbaT, corners2f[0], corners2f[1], new Scalar(255,255,0), 3);
            	Core.line(mRgbaT, corners2f[1], corners2f[2], new Scalar(255,255,0), 3);
            	Core.line(mRgbaT, corners2f[2], corners2f[3], new Scalar(255,255,0), 3);
            	Core.line(mRgbaT, corners2f[3], corners2f[0], new Scalar(255,255,0), 3);
            	
//            	MatOfPoint2f curve = new MatOfPoint2f(corners2f);
//                MatOfPoint2f approxCurve = new MatOfPoint2f();
                
//                Imgproc.approxPolyDP(curve, approxCurve, Imgproc.arcLength(curve, true) * 0.02, true);
                
            	/*
                if (approxCurve.cols() != 4)
                {
                    //std::cout << "The object is not quadrilateral!" << std::endl;
                	Log.d(TAG, "The object is not quadrilateral!");
                
                }
                else
                {
                	Point center = new Point(0,0);
                	for (int i = 0; i < corners2f.length; i++)
                	{
                	    center.x += corners2f[i].x;
                	    center.y += corners2f[i].y;
                	}

                	center.x *= (1. / corners2f.length);
                	center.y *= (1. / corners2f.length);
                	
                	Point[] sorted_corners = sortCorners(corners2f,center);
                	Core.circle(mRgbaT, sorted_corners[0], 5, new Scalar(255,255,0));
                	Core.circle(mRgbaT, sorted_corners[1], 5, new Scalar(255,255,0));
                	Core.circle(mRgbaT, sorted_corners[2], 5, new Scalar(255,255,0));
                	Core.circle(mRgbaT, sorted_corners[3], 5, new Scalar(255,255,0));
*/
                }
            	
//            }
            
        }
    	return mRgbaT;
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        mEffectMenuItems = new MenuItem[effects.size()];

        int idx = 0;
        ListIterator<String> effectItr = effects.listIterator();
        while(effectItr.hasNext()) {
           String element = effectItr.next();
           mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
           idx++;
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<android.hardware.Camera.Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            android.hardware.Camera.Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
         }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            android.hardware.Camera.Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            //resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }
*/
    @SuppressLint({"ClickableViewAccessibility", "ShowToast", "SimpleDateFormat" })
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
        try
		{
			if (corners.size()==4)
			{
				// qui attivo una "clessidra" e aspetto che la foto raddrizzata sia pronta
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			    String currentDateandTime = sdf.format(new Date());
			    String fileName = Environment.getExternalStorageDirectory().getPath() +
			                           "/sample_picture_" + currentDateandTime + ".jpg";
			    mOpenCvCameraView.takePicture(fileName, corners);
				Thread.sleep(2000);
				this.finish();
			}
			else
			{
				Toast.makeText(this, "Non hai inquadrato bene il documento, riprova", Toast.LENGTH_SHORT).show();
			}
		}
		catch (Exception e)
		{
			Toast.makeText(this, "Non hai inquadrato bene il documento, riprova", Toast.LENGTH_SHORT).show();
		}
        return false;
    }
    
    
} 

