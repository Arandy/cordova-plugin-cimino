package it.cimino;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.abbyy.ocrsdk.Client;
import com.abbyy.ocrsdk.Task;
import com.abbyy.ocrsdk.TextFieldSettings;
import com.googlecode.tesseract.android.TessBaseAPI;

import it.ciminotrack.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class ImageOcrActivity extends Activity{
    private static final String TAG = "Cimino::ImageOCR";
    private static final boolean DEBUG = false;

    //private ImageView selected_photo;
    private ProgressDialog progressDialog;
    @SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//    private String currentDateandTime = "";
    private String currentFileName  = "";
    private TessBaseAPI baseApi = null;
    private Client restClient;
    private String abbyLanguage = "Italian";
    private Vector<String> abbyyArgList;
    private String charWhiteList = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890'.,:/^-àèìòùé+=-"; // |
    private String charWhiteListLetters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
//    private static final float BYTES_PER_PX = 4.0f;
	private IAmABackgroundTask bktask = new IAmABackgroundTask();
	//private Mat collage;
    
    public ImageOcrActivity() {
        if (DEBUG) Log.d(TAG, "Instantiated new " + this.getClass());   
        ClientSettings.setupProxy();
		
		restClient = new Client();
		// replace with 'https://cloud.ocrsdk.com' to enable secure connection
		restClient.serverUrl = "http://cloud.ocrsdk.com";
		restClient.applicationId = ClientSettings.APPLICATION_ID;
		restClient.password = ClientSettings.PASSWORD;

		String[] abbyyArgs = {
				"--letterSet="+charWhiteList,
				"--textType=normal,matrix",
				"--oneTextLine=true"
		};
				
        abbyyArgList = new Vector<String>(Arrays.asList(abbyyArgs));

    }
    
	/**
	 * Check that user specified application id and password.
	 * 
	 * @return false if no application id or password
	 */
	private boolean checkAppId() {
		String appId = ClientSettings.APPLICATION_ID;
		String password = ClientSettings.PASSWORD;
		if (appId.isEmpty() || password.isEmpty()) {
			if (DEBUG) System.out.println("Error: No application id and password are specified.");
			if (DEBUG) System.out.println("Please specify them in ClientSettings.java.");
			return false;
		}
		return false;
	}
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    if (DEBUG) Log.d(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
   
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	if (DEBUG) Log.d(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        currentFileName = extras.getString("filepath");
        		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.ocr_view);

		if (DEBUG) Log.d(TAG,"###### new ProgressDialog");
		progressDialog = new ProgressDialog(ImageOcrActivity.this);
		progressDialog.setMessage("Attendere, elaborazione in corso...");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setProgress(0);  	    	
		progressDialog.setMax(100);        
		progressDialog.setCancelable(false);
		if (DEBUG) Log.d(TAG,"###### progressDialog.show()");
    	progressDialog.show();     
    	
        //selected_photo = (ImageView) findViewById(R.id.selected_photo);

/*        File imgFile = new File(currentFileName.replace("file:", ""));        

    	BitmapFactory.Options options = new BitmapFactory.Options();
    	options.inJustDecodeBounds = true;
    	BitmapFactory.decodeFile(imgFile.getAbsolutePath(),options);    	
    	
    	float imageHeight = options.outHeight;
    	float imageWidth = options.outWidth;
    	String imageMimeType = options.outMimeType;
    	
    	Log.d("ScaleBeforeLoad", "w, h, type: " + imageWidth + ", " + imageHeight + ", " + imageMimeType);
    	Log.d("ScaleBeforeLoad", "estimated memory required in MB: " + imageWidth * imageHeight * BYTES_PER_PX / MemUtils.BYTES_IN_MB);

        if( (imageWidth * imageHeight * BYTES_PER_PX / MemUtils.BYTES_IN_MB) > MemUtils.megabytesFree())
        {
        	options.inJustDecodeBounds = false;
        	options.inSampleSize = 2;	        	
        	inputBitmap =  BitmapFactory.decodeFile(imgFile.getAbsolutePath(),options);
        	
        }
        else
        {
            inputBitmap =  BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        
//        if(inputBitmap.getWidth()>inputBitmap.getHeight()) // devi controllare l'orientamento (width>height) 
        if(imageWidth>imageHeight) // devi controllare l'orientamento (width>height) 
        {
	        // 1) devo ruotare la bitmap di 90 gradi in senso orario
	        // l'immagine da 3246x2448, deve diventare 2448x3246        	
	        Matrix matrix = new Matrix();
	        matrix.setRotate(90, 0, 0);
            Bitmap rotateBitmap = Bitmap.createBitmap(inputBitmap, 0, 0,inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);
	        inputBitmap = rotateBitmap;
		}
		*/	        			
        //currentDateandTime = sdf.format(new Date());
    	//currentFileName = getTempDirectoryPath() + "/sample_picture_" + currentDateandTime + ".jpg";        
        
    	if (DEBUG) Log.d(TAG,"###### IAmABackgroundTask().execute() start");
    	//new IAmABackgroundTask().execute();
    	bktask.execute();
    	if (DEBUG) Log.d(TAG,"###### IAmABackgroundTask().execute() end");
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
    	if ( progressDialog!=null && progressDialog.isShowing() ){
            progressDialog.dismiss();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
        	if (DEBUG) Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
        	if (DEBUG) Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
    	if ( progressDialog!=null && progressDialog.isShowing() ){
            progressDialog.dismiss();
        }
    	bktask.cancel(true);
    	this.finish();
    }
    
    
    
    private String getTempDirectoryPath() 
    {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) 
        {
            cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Android/data/" + this.getPackageName() + "/cache/");
        }
        // Use internal storage
        else 
        {
            cache = this.getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }
    
    private String getFileDirectoryPath() 
    {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) 
        {
        	// se la cartella non esiste la devo creare?
            cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Android/data/" + this.getPackageName() + "/files/");
        }
        // Use internal storage
        else 
        {
            cache = this.getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }    

    
    class IAmABackgroundTask extends AsyncTask<String, Integer, Boolean> {
    	
    	public int safeLongToInt(long l) {
    	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
    	        throw new IllegalArgumentException
    	            (l + " cannot be cast to int without changing its value.");
    	    }
    	    return (int) l;
    	}

		@Override
		protected void onPreExecute() {
			if (DEBUG) Log.d(TAG,"###### IAmABackgroundTask.onPreExecute");
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if (DEBUG) Log.d(TAG,"###### IAmABackgroundTask.onPostExecute");

		    if (ImageOcrActivity.this != null) {
	        	if ( progressDialog!=null && progressDialog.isShowing() ){
	                progressDialog.dismiss();
	            }
                ImageOcrActivity.this.finish();
	    	}
		    
		}
		
		@SuppressWarnings("unused")
		@Override
		protected Boolean doInBackground(String... params) {			
			            
	        File imgFile = new File(currentFileName.replace("file:", "").replace("\n", ""));        
			
			boolean status = true;
	    	Intent resultIntent = new Intent();
	    	if (DEBUG) Log.d(TAG,"###### IAmABackgroundTask.doInBackground");
	        	        
	        Mat inputMat = new Mat();	    	
	        String imageFile = getTempDirectoryPath()+"/resultImage.jpg";
	        Mat src_inputMat = Imgcodecs.imread(imgFile.getAbsolutePath());
	        if (DEBUG) Imgcodecs.imwrite(imageFile, src_inputMat);
	        
	        if (src_inputMat.cols()>src_inputMat.rows())
	        {
		        Core.transpose(src_inputMat, inputMat);
		        Core.flip(inputMat, inputMat, 1);
	        }
	        else
	        {
		        inputMat = Imgcodecs.imread(imgFile.getAbsolutePath());
	        }
	        src_inputMat.release();
	        src_inputMat=null;	        	
	        
	        progressDialog.setProgress(1);
	        
	    	float realSizePixelRatio = 0; // px/mm
	        
            baseApi = new TessBaseAPI();
	        progressDialog.setProgress(10);
			baseApi.init(getFileDirectoryPath(), "ita"); // /tessdata/ita.traineddata
            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
            baseApi.setDebug(false);
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, charWhiteListLetters);
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "\\s");
	        progressDialog.setProgress(11);
			
	    	Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_BGR2GRAY);
	        progressDialog.setProgress(12);
			Mat OriginalBW = inputMat.clone();    	
	        progressDialog.setProgress(13);
			Mat OriginalBW_copy = inputMat.clone();
	        progressDialog.setProgress(14);
			Mat OriginalBW_copy2 = inputMat.clone(); // new Mat(inputMat,new Rect(0,0,inputMat.cols(),inputMat.rows()/2));
	        progressDialog.setProgress(15);
	    	Imgproc.GaussianBlur(inputMat, inputMat, new Size(5, 5), 5); //denoise
	        progressDialog.setProgress(16);
	    	Imgproc.medianBlur(inputMat, inputMat, 5); 
	        progressDialog.setProgress(17);
	    	Imgproc.adaptiveThreshold(inputMat, inputMat, 254, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,15,5);
	        progressDialog.setProgress(18);
	    	
			Mat Inv_OriginalBW = new Mat();
//    		Mat invertcolormatrix= new Mat(OriginalBW.rows(),OriginalBW.cols(), OriginalBW.type(), new Scalar(255,255,255));
//    		Core.subtract(invertcolormatrix, Ocropped, Inv_Ocropped);    		
//    		Core.invert(Ocropped, Inv_Ocropped);
			Core.bitwise_not(OriginalBW, Inv_OriginalBW);
	    	Imgproc.threshold(Inv_OriginalBW, Inv_OriginalBW, 160, 255, Imgproc.THRESH_BINARY);
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/INV_bitwise_not.jpg", Inv_OriginalBW);
	    	Imgproc.dilate(Inv_OriginalBW, Inv_OriginalBW, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
	    	Imgproc.erode(Inv_OriginalBW, Inv_OriginalBW, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/INV_erode_dilate.jpg", Inv_OriginalBW);
	    	Imgproc.GaussianBlur(Inv_OriginalBW, Inv_OriginalBW, new Size(5, 5), 5); //denoise
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/INV_GaussianBlur5x5.jpg", Inv_OriginalBW);
	    	Imgproc.threshold(Inv_OriginalBW, Inv_OriginalBW, 127, 255, Imgproc.THRESH_BINARY);
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/INV_threshold127.jpg", Inv_OriginalBW);

	    	String imageFile_ocr = getTempDirectoryPath()+"/0-imageOcr.jpg";
	    	if (DEBUG) Imgcodecs.imwrite(imageFile_ocr, inputMat);	    	
	        progressDialog.setProgress(19);

	    	Mat inputMat_morph = new Mat();
	    	Imgproc.GaussianBlur(inputMat, inputMat_morph, new Size(5, 5), 5); //denoise
	    	Imgproc.medianBlur(inputMat_morph, inputMat_morph, 5); 
	    	progressDialog.setProgress(20);
	    	if (DEBUG) Imgcodecs.imwrite( getTempDirectoryPath()+"/1-imageOcr_tresh_blur.jpg", inputMat);
//			double mean = Core.mean(inputMat).val[0];
//			if (DEBUG) Log.d(TAG,"mean: "+mean);
			Imgproc.threshold(inputMat_morph, inputMat_morph, 248, 255, Imgproc.THRESH_BINARY); // +Imgproc.THRESH_OTSU
			progressDialog.setProgress(21);
			if (DEBUG) Imgcodecs.imwrite( getTempDirectoryPath()+"/2-imageOcr_tresh_new.jpg", inputMat_morph);
			
//	    	Imgproc.morphologyEx(inputMat_morph, inputMat_morph, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,3)));
//	    	Imgproc.morphologyEx(inputMat_morph, inputMat_morph, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,1)));
//	    	Imgproc.morphologyEx(inputMat_morph, inputMat_morph, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
	    	Imgproc.dilate(inputMat_morph, inputMat_morph, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)), new Point(-1,-1),2);
			if (DEBUG) Imgcodecs.imwrite( getTempDirectoryPath()+"/3-imageOcr_tresh_new_dilate.jpg", inputMat_morph);
				        
	    	Rect prodottoTitleRect = null;
	    	Rect articoloTitleRect = null;
	    	Rect lottoTitleRect = null;
	    	Rect scadenzaTitleRect = null;
	    	
	    	//collage = new Mat(inputMat.rows(),inputMat.cols(),CvType.CV_8U);
			Mat sobel = new Mat();
//			Mat sobel_dilated = new Mat();
			Mat thresh = new Mat();

			Imgproc.Sobel(inputMat_morph, sobel, CvType.CV_8U, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT); 
	        progressDialog.setProgress(22);
			Imgproc.Sobel(sobel, sobel, CvType.CV_8U, 0, 1, 3, 1, 0, Core.BORDER_DEFAULT); 
	        progressDialog.setProgress(23);
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/11-sobel.jpg", sobel);	    
			Imgproc.erode(sobel, sobel, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));
	        progressDialog.setProgress(24);
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/22-sobel_erode_3x3.jpg", sobel);	 
			if (sobel.cols()>1500)
			{
				Imgproc.dilate(sobel, thresh, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,3)),new Point(-1,-1), 9);
			}
			else
			{
				Imgproc.dilate(sobel, thresh, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)),new Point(-1,-1), 9);
			}
//	        progressDialog.setProgress(23);
//			Imgproc.dilate(sobel, sobel, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,1)),new Point(-1,-1), 8);
	        progressDialog.setProgress(25);
//			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/33-image_sobelxy_dilated.jpg", sobel_dilated);	    
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/33-sobelxy_dilated.jpg", thresh);	    
			
	    	/*Imgproc.medianBlur(sobel_dilated, sobel_dilated, 3);
	        progressDialog.setProgress(25);	    	
	    	Imgproc.GaussianBlur(sobel_dilated, sobel_dilated, new Size(7, 1), 5); //denoise
			double mean = Core.mean(sobel_dilated).val[0]*2; // *3
			Log.d(TAG,"mean: "+mean);
	        progressDialog.setProgress(26);			
//			Imgproc.threshold(sobel, thresh, Core.mean(sobel).val[0]/2, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
			Imgproc.threshold(sobel_dilated, thresh, (mean>255 ? 255 : mean), 255, Imgproc.THRESH_BINARY); // +Imgproc.THRESH_OTSU
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/44-image_sobel_thresh.jpg", thresh);
	        progressDialog.setProgress(27);*/
			
			/*
			Mat intersect = new Mat();
			Mat _inv = new Mat();
			Core.bitwise_not(inputMat, _inv);
			Core.bitwise_and(thresh, _inv, intersect);
			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/3-imageOcr_intersect.jpg", intersect);
	        */
//			Imgproc.morphologyEx(thresh, thresh_dilate, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(41,1) ));

			if (DEBUG) Log.d(TAG,"detectTextBoxes(thresh,OriginalBW_copy2,1)");
	    	List<Rect> textBoxesOthers = detectTextBoxes(thresh,OriginalBW_copy2,1); // lo uso per trovare lotto e scadenza	  
	    	
	    	Iterator<Rect> lb = textBoxesOthers.iterator();
//	    	Iterator<Rect> _lb = textBoxes.iterator();
	    	//int boxes_count = textBoxesOthers.size();
			progressDialog.setProgress(30);	   
			double _step = (double)15/(double)textBoxesOthers.size();
			double current_progress=30;
	    	// ***************************** qui cerco la descrizione ***********************************************
			if (DEBUG) Log.d(TAG,"Inizio la ricerca delle parole descrizione lotto e scadenza.");
	    	//Log.d(TAG,"Inizio la ricerca della colonna descrizione.");
	    	boolean force_exit = false; 
	    	int search_count = 0;
	    	// primo ciclo: cerco le etichette delle colonne della tabella (codice articolo, nome prodotto, lotto e scadenza)
	    	Rect roi;
	    	Mat cropped = new Mat();
	    	Mat Ocropped = new Mat();
	    	Mat Inv_Ocropped = new Mat();
			Mat thresh_dilate_cut = new Mat();
	    	Bitmap bitmap = null;
	    	Bitmap Obitmap = null;
	    	Bitmap Inv_Obitmap = null;
	    	String recognizedText;
	    	int confidence;
	    	String OrecognizedText;
	    	int Oconfidence;
	    	String Inv_OrecognizedText;
	    	int Inv_Oconfidence;
	    	String stringToFind;
	    	String OstringToFind;
			String Inv_OstringToFind; 
			JSONObject finalData;
    		int index ;
    		Rect leftColumn;
    		int newX;
    		int widthIncrease;
	    	boolean stessaLinea_lotto = false;
	    	boolean stessaLinea_scadenza = false;
	    	
	    	while(lb.hasNext() && (prodottoTitleRect==null || scadenzaTitleRect==null || lottoTitleRect==null || articoloTitleRect==null) && !force_exit)
	    	{
	    		roi = lb.next();
	    		if (prodottoTitleRect!=null)
	    		{
		    		if (roi.y<(prodottoTitleRect.y-(prodottoTitleRect.height*2))) // limito la ricerca fino ad un certo punto sopra descrizione
		    		{
		    			force_exit = true;
		    		}
	    			
	    		}
	    		cropped = new Mat(inputMat,roi);        	    	
	    		if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/search_"+search_count+".jpg", cropped);
	    		Ocropped = new Mat(OriginalBW,roi);        	    	
	    		if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/Osearch_"+search_count+".jpg", Ocropped);
	    		Inv_Ocropped = new Mat(Inv_OriginalBW,roi);  
	    		if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/Inv_Osearch_"+search_count+".jpg", Inv_Ocropped);
	    		search_count++;
	    		
                bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped, bitmap);              
                
                Obitmap = Bitmap.createBitmap(Ocropped.cols(), Ocropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(Ocropped, Obitmap);

                Inv_Obitmap = Bitmap.createBitmap(Inv_Ocropped.cols(), Inv_Ocropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(Inv_Ocropped, Inv_Obitmap);
                
				baseApi.setImage(bitmap);
				recognizedText = baseApi.getUTF8Text().replace("\n", "");
				confidence = baseApi.meanConfidence();
				bitmap.recycle();
				bitmap=null;

				baseApi.setImage(Obitmap);
				OrecognizedText = baseApi.getUTF8Text().replace("\n", "");
				Oconfidence = baseApi.meanConfidence();
				Obitmap.recycle();
				Obitmap=null;

				baseApi.setImage(Inv_Obitmap);
				Inv_OrecognizedText = baseApi.getUTF8Text().replace("\n", "");
				Inv_Oconfidence = baseApi.meanConfidence();
				Inv_Obitmap.recycle();
				Inv_Obitmap=null;				
				
				if (DEBUG) Log.d(TAG,"roi ("+search_count+"): "+roi.x+","+roi.y+","+roi.width+","+roi.height);
				if (DEBUG) Log.d(TAG,"Only letters ROI confidence: "+confidence+" - OCR:"+recognizedText);
				if (DEBUG) Log.d(TAG,"Only letters ROI Oconfidence: "+Oconfidence+" - OOCR:"+OrecognizedText);
				if (DEBUG) Log.d(TAG,"Only letters ROI Inv_Oconfidence: "+Inv_Oconfidence+" - Inv_OOCR:"+Inv_OrecognizedText);

				stringToFind = Cimino.toSlug(recognizedText.toLowerCase(Locale.ITALIAN));
				OstringToFind = Cimino.toSlug(OrecognizedText.toLowerCase(Locale.ITALIAN));
				Inv_OstringToFind = Cimino.toSlug(Inv_OrecognizedText.toLowerCase(Locale.ITALIAN));
				
				if (
						stringToFind.indexOf("descrizione")>-1 || 
						StringSimilarity.similarity(stringToFind, "descrizione")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "cod descrizione")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "descrizione articolo")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "doscriziono")>=0.6
					)
				{
					prodottoTitleRect = roi;
					if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), new Scalar(100,100,100),3);					
					if (DEBUG) Log.d(TAG,"Colonna Descrizione trovata!");
					
				}
				else if (
						OstringToFind.indexOf("descrizione")>-1 || 
						StringSimilarity.similarity(OstringToFind, "descrizione")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "cod I descrizione")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "descrizione articolo")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "doscriziono")>=0.6
						
					)
				{
					prodottoTitleRect = roi;
					if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), new Scalar(100,100,100),3);
					if (DEBUG) Log.d(TAG,"Colonna Descrizione trovata!");
				}
				else if (
						Inv_OstringToFind.indexOf("descrizione")>-1 || 
						StringSimilarity.similarity(Inv_OstringToFind, "descrizione")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "cod I descrizione")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "descrizione articolo")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "doscriziono")>=0.6
						
					)
				{
					prodottoTitleRect = roi;
					if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), new Scalar(100,100,100),3);
					if (DEBUG) Log.d(TAG,"Colonna Descrizione trovata!");
				}				
				else if (
						stringToFind.indexOf("codice articolo")>-1 || 
						stringToFind.indexOf("n.")>-1 ||
						StringSimilarity.similarity(stringToFind, "codice articolo")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "codicearticolo")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "articolo")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "cod.")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "cod.art.")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "codice")>=0.6
					)
				{
					articoloTitleRect = roi;
					if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), new Scalar(100,100,100),3);
					if (DEBUG) Log.d(TAG,"Colonna Codice Articolo trovata!");
					
				}
				else if (
						OstringToFind.indexOf("codice articolo")>-1 ||
						OstringToFind.indexOf("n.")>-1 ||
						StringSimilarity.similarity(OstringToFind, "codice articolo")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "codicearticolo")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "articolo")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "cod.")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "cod.art.")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "codice")>=0.6
						
					)
				{
					articoloTitleRect = roi;
					if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), new Scalar(100,100,100),3);
					if (DEBUG) Log.d(TAG,"Colonna Codice Articolo trovata!");
				}		
				else if (
						Inv_OstringToFind.indexOf("codice articolo")>-1 ||
						Inv_OstringToFind.indexOf("n.")>-1 ||
						StringSimilarity.similarity(Inv_OstringToFind, "codice articolo")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "codicearticolo")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "articolo")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "cod.")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "cod.art.")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "codice")>=0.6
						
					)
				{
					articoloTitleRect = roi;
					if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), new Scalar(100,100,100),3);
					if (DEBUG) Log.d(TAG,"Colonna Codice Articolo trovata!");
				}				
				/*****/
				else if ( 
						stringToFind.indexOf("lotto")>-1 ||
						stringToFind.indexOf("lono")>-1 || 
						stringToFind.indexOf("loùo")>-1 || 						
						stringToFind.indexOf("loro")>-1 || 						
						StringSimilarity.similarity(stringToFind, "lotto")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "lono")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "loùo")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "loro")>=0.6
					)
				{
					if (DEBUG) Log.d(TAG,"scadenza StringSimilarity.similarity: "+StringSimilarity.similarity(stringToFind, "scadenza"));
					if (
							stringToFind.indexOf("scad")>-1 || 
							stringToFind.indexOf("seed")>-1 || 
							stringToFind.indexOf("sced")>-1 || 
							stringToFind.indexOf("sead")>-1 || 
							stringToFind.indexOf("scad.")>-1 || 
							stringToFind.indexOf("scadenza")>-1 ||
							StringSimilarity.similarity(stringToFind, "scad")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "seed")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "sced")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "sead")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "scad.")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "scadenza")>=0.6
						)
					{
//							lottoScadenzaTitleRect = roi;
						if (DEBUG) Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						lottoTitleRect = roi;
						if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), new Scalar(100,100,100),3);
						if (DEBUG) Log.d(TAG,"Campo Lotto trovato!");						
					}
				}
				else if ( 
						OstringToFind.indexOf("lotto")>-1 || 
						OstringToFind.indexOf("lono")>-1 || 
						OstringToFind.indexOf("loùo")>-1 || 						
						OstringToFind.indexOf("loro")>-1 || 						
						StringSimilarity.similarity(OstringToFind, "lotto")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "lono")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "loùo")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "loro")>=0.6
					)
				{
					if (DEBUG) Log.d(TAG,"scadenza StringSimilarity.similarity: "+StringSimilarity.similarity(OstringToFind, "scadenza"));
					if (
							OstringToFind.indexOf("scad")>-1 || 
							OstringToFind.indexOf("seed")>-1 || 
							OstringToFind.indexOf("sced")>-1 || 
							OstringToFind.indexOf("sead")>-1 || 
							OstringToFind.indexOf("scad.")>-1 || 
							OstringToFind.indexOf("scadenza")>-1 ||
							StringSimilarity.similarity(OstringToFind, "scad")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "seed")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "sced")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "sead")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "scad.")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "scadenza")>=0.6
						)
					{
//							lottoScadenzaTitleRect = roi;
						if (DEBUG) Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						lottoTitleRect = roi;
						if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), new Scalar(100,100,100),3);
						if (DEBUG) Log.d(TAG,"Campo Lotto trovato!");						
					}
				}	
				else if ( 
						Inv_OstringToFind.indexOf("lotto")>-1 || 
						Inv_OstringToFind.indexOf("lono")>-1 || 
						Inv_OstringToFind.indexOf("loùo")>-1 || 						
						Inv_OstringToFind.indexOf("loro")>-1 || 						
						StringSimilarity.similarity(Inv_OstringToFind, "lotto")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "lono")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "loùo")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "loro")>=0.6
					)
				{
					if (DEBUG) Log.d(TAG,"scadenza StringSimilarity.similarity: "+StringSimilarity.similarity(OstringToFind, "scadenza"));
					if (
							Inv_OstringToFind.indexOf("scad")>-1 || 
							Inv_OstringToFind.indexOf("seed")>-1 || 
							Inv_OstringToFind.indexOf("sced")>-1 || 
							Inv_OstringToFind.indexOf("sead")>-1 || 
							Inv_OstringToFind.indexOf("scad.")>-1 || 
							Inv_OstringToFind.indexOf("scadenza")>-1 ||
							StringSimilarity.similarity(Inv_OstringToFind, "scad")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "seed")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "sced")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "sead")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "scad.")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "scadenza")>=0.6
						)
					{
//							lottoScadenzaTitleRect = roi;
						if (DEBUG) Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						lottoTitleRect = roi;
						if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), new Scalar(100,100,100),3);
						if (DEBUG) Log.d(TAG,"Campo Lotto trovato!");						
					}
				}					
				else if ( 
						stringToFind.indexOf("scad")>-1 || 
						stringToFind.indexOf("seed")>-1 || 
						stringToFind.indexOf("sced")>-1 || 
						stringToFind.indexOf("sead")>-1 || 
						stringToFind.indexOf("scad.")>-1 || 
						stringToFind.indexOf("scadenza")>-1 ||
						StringSimilarity.similarity(stringToFind, "scad")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "seed")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "sced")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "sead")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "scad.")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "scadenza")>=0.6
						
					)
				{
					if (DEBUG) Log.d(TAG,"lotto StringSimilarity.similarity: "+StringSimilarity.similarity(stringToFind, "lotto"));
					if (
							stringToFind.indexOf("lotto")>-1 || 
							stringToFind.indexOf("lono")>-1 || 
							stringToFind.indexOf("loùo")>-1 || 						
							stringToFind.indexOf("loro")>-1 || 						
							StringSimilarity.similarity(stringToFind, "lotto")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "lono")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "loùo")>=0.6 ||
							StringSimilarity.similarity(stringToFind, "loro")>=0.6
						)
					{
//							lottoScadenzaTitleRect = roi;
						if (DEBUG) Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						scadenzaTitleRect = roi;
						if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), new Scalar(100,100,100),3);
						if (DEBUG) Log.d(TAG,"Campo Scadenza trovato!");						
					}
				}
				else if ( 
						OstringToFind.indexOf("scad")>-1 || 
						OstringToFind.indexOf("seed")>-1 || 
						OstringToFind.indexOf("sced")>-1 || 
						OstringToFind.indexOf("sead")>-1 || 
						OstringToFind.indexOf("scad.")>-1 || 
						OstringToFind.indexOf("scadenza")>-1 ||
						StringSimilarity.similarity(OstringToFind, "scad")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "seed")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "sced")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "sead")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "scad.")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "scadenza")>=0.6
						
					)
				{
					if (DEBUG) Log.d(TAG,"lotto StringSimilarity.similarity: "+StringSimilarity.similarity(OstringToFind, "lotto"));
					if (
							OstringToFind.indexOf("lotto")>-1 || 
							OstringToFind.indexOf("lono")>-1 || 
							OstringToFind.indexOf("loùo")>-1 || 						
							OstringToFind.indexOf("loro")>-1 || 						
							StringSimilarity.similarity(OstringToFind, "lotto")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "lono")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "loùo")>=0.6 ||
							StringSimilarity.similarity(OstringToFind, "loro")>=0.6
						)
					{
//							lottoScadenzaTitleRect = roi;
						if (DEBUG) Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						scadenzaTitleRect = roi;
						if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), new Scalar(100,100,100),3);
						if (DEBUG) Log.d(TAG,"Campo Scadenza trovato!");						
					}
				}
				else if ( 
						Inv_OstringToFind.indexOf("scad")>-1 || 
						Inv_OstringToFind.indexOf("seed")>-1 || 
						Inv_OstringToFind.indexOf("sced")>-1 || 
						Inv_OstringToFind.indexOf("sead")>-1 || 
						Inv_OstringToFind.indexOf("scad.")>-1 || 
						Inv_OstringToFind.indexOf("scadenza")>-1 ||
						StringSimilarity.similarity(Inv_OstringToFind, "scad")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "seed")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "sced")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "sead")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "scad.")>=0.6 ||
						StringSimilarity.similarity(Inv_OstringToFind, "scadenza")>=0.6
						
					)
				{
					if (DEBUG) Log.d(TAG,"lotto StringSimilarity.similarity: "+StringSimilarity.similarity(OstringToFind, "lotto"));
					if (
							Inv_OstringToFind.indexOf("lotto")>-1 || 
							Inv_OstringToFind.indexOf("lono")>-1 || 
							Inv_OstringToFind.indexOf("loùo")>-1 || 						
							Inv_OstringToFind.indexOf("loro")>-1 || 						
							StringSimilarity.similarity(Inv_OstringToFind, "lotto")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "lono")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "loùo")>=0.6 ||
							StringSimilarity.similarity(Inv_OstringToFind, "loro")>=0.6
						)
					{
//							lottoScadenzaTitleRect = roi;
						if (DEBUG) Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						scadenzaTitleRect = roi;
						if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), new Scalar(100,100,100),3);
						if (DEBUG) Log.d(TAG,"Campo Scadenza trovato!");						
					}
				}				

				cropped = null;
				bitmap = null;
	    	    	current_progress+=_step;
	    	    	progressDialog.setProgress((int)current_progress);	    			
	    	}
	    	progressDialog.setProgress(45);
            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, charWhiteList);
						
	    	progressDialog.setProgress(46);
	    	
	    	// fuori da questo ciclo devo trovare sulla stessa linea di desxcrizione lotto e scadenza
	    	// basta controllare se sono "sotto" cioe' y>ydescrizione e 
	    	// se non ci sono allora lotto e scadenza sono nell'area descrizione prodotto
	    	finalData = new JSONObject();
	    	if (prodottoTitleRect!=null)
	    	{
	    		List<Rect> codiciArticoloList = new ArrayList<Rect>();
	    		// cerco tutti i codici articolo - se sono riuscito a trovare la colonna
    			if (articoloTitleRect!=null)
    			{
    				// secondo ciclo: cerco tutte le aree realtiva a un codice prodotto
    	    		lb = textBoxesOthers.iterator();
    	    		Rect _roi;
    	    		while (lb.hasNext())
    	    		{
    	    			_roi = lb.next();
			    		if (		    				
			    				(_roi.y)>(articoloTitleRect.y+articoloTitleRect.height) 
			    				&& 
			    				(
			    						(_roi.x<=articoloTitleRect.x && _roi.x+_roi.width>=articoloTitleRect.x+articoloTitleRect.width) ||
			    						(_roi.x<=articoloTitleRect.x && _roi.x+_roi.width<=articoloTitleRect.x+articoloTitleRect.width && _roi.x+_roi.width>articoloTitleRect.x)
	    						)
			    			)
			    		{
			    			codiciArticoloList.add(_roi);
			    			if (DEBUG) Imgproc.rectangle(OriginalBW_copy, _roi.tl(), _roi.br(), new Scalar(100,100,100),3);
			    			if (DEBUG) Imgproc.rectangle(OriginalBW_copy2, _roi.tl(), _roi.br(), new Scalar(100,100,100),3);
			    		}
    	    		}
        			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/detectTextBoxes_1.jpg", OriginalBW_copy2);
    			}
		    	progressDialog.setProgress(47);

		    	if (lottoTitleRect!=null)
		    	{
			    	Rect searchAreaLotto = new Rect(lottoTitleRect.x,prodottoTitleRect.y,lottoTitleRect.width,prodottoTitleRect.height);
			    	if (intersect_Y(searchAreaLotto, lottoTitleRect))
			    	{
			    		stessaLinea_lotto = true;
			    		lottoTitleRect.x = (int) (lottoTitleRect.x-(lottoTitleRect.width*0.5))+5;
			    		lottoTitleRect.width = (int) (lottoTitleRect.width+(lottoTitleRect.width*0.5)+(lottoTitleRect.width*0.7));
			    		if (DEBUG) Log.d(TAG,"Il campo lotto sta sulla stessa linea di descrizione!");
			    	}
		    	}
		    	if (scadenzaTitleRect!=null)
		    	{
			    	Rect searchAreaScadenza = new Rect(scadenzaTitleRect.x,prodottoTitleRect.y,scadenzaTitleRect.width,prodottoTitleRect.height-10);
			    	if (intersect_Y(searchAreaScadenza, scadenzaTitleRect))
			    	{
			    		stessaLinea_scadenza = true;
			    		if (DEBUG) Log.d(TAG,"Il campo scadenza sta sulla stessa linea di descrizione!");
			    	}
		    	}
		    	JSONArray jrows = new JSONArray();
		    	
		    	progressDialog.setProgress(48);
		    	if (articoloTitleRect==null)
		    	{
			    	Iterator<Rect> broi = textBoxesOthers.iterator();
			    	
			    	List<Rect> tableHeader = new ArrayList<Rect>();
			    	tableHeader.add(prodottoTitleRect);

			    	// terzo ciclo se articoloTitleRect è null, cerco, se c'è, l'area realtiva all'intestazione di colonna a sinistra di "descrizione"
			    	Rect __troi;
			    	while(broi.hasNext())
			    	{
			    		__troi = broi.next();
			    		if (
			    				((__troi.y>=prodottoTitleRect.y && __troi.y<prodottoTitleRect.y+prodottoTitleRect.height/3) ||
			    				(__troi.y<prodottoTitleRect.y && __troi.y+__troi.height>prodottoTitleRect.y)) &&
			    				__troi.x<prodottoTitleRect.x
			    			)
			    		{
			    			tableHeader.add(__troi);
			    		}
			    			
			    	}
		    		Collections.sort(tableHeader, new Comparator<Rect>(){
		    		     public int compare(Rect o1, Rect o2){
		    		         if(o1.x == o2.x)
		    		             return 0;
		    		         return o1.x < o2.x ? -1 : 1;
		    		     }
		    		});
		    		
		    		index = tableHeader.indexOf(prodottoTitleRect);
		    		leftColumn = new Rect(0,prodottoTitleRect.y,1,prodottoTitleRect.height);
		    		//Rect rightColumn = new Rect(inputMat.cols()-1,productTitleRect.y,inputMat.cols(),productTitleRect.y+productTitleRect.height);
		    		if (index>0)
		    		{
		    			leftColumn = tableHeader.get(index-1);
		    		}
		    		
		    		newX = (int) (leftColumn.x+leftColumn.width*1.4); //+50%
		    		if (newX>prodottoTitleRect.x) 
		    		{
		    			newX = prodottoTitleRect.x;
		    		}
		    		widthIncrease = prodottoTitleRect.x-newX;  
		    		prodottoTitleRect.width += widthIncrease; //(int) ((rightColumn.x-productTitleRect.x)/1.2); //-20%
		    		prodottoTitleRect.x = newX;		    		
		    	}
		    	else
		    	{		    		
		    		newX = (int) ((articoloTitleRect.x+articoloTitleRect.width)*1.1); //+10%
		    		widthIncrease = prodottoTitleRect.x-newX;  
		    		prodottoTitleRect.width += widthIncrease; //(int) ((rightColumn.x-productTitleRect.x)/1.2); //-20%
		    		prodottoTitleRect.x = newX;		    		
		    	}

	    		//Imgproc.rectangle(OriginalBW, prodottoTitleRect.tl(), prodottoTitleRect.br(), new Scalar(50,50,50),1);
				Rect final_area = new Rect(prodottoTitleRect.x,prodottoTitleRect.y,sobel.cols()-prodottoTitleRect.x,sobel.rows()-prodottoTitleRect.y);
		    	Mat OriginalBW_copy_cut = new Mat(OriginalBW_copy,final_area);		    	
				if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/44-OriginalBW_copy_cut.jpg", OriginalBW_copy_cut);
		    	Mat sobel_cut = new Mat(sobel,final_area);
		    	Mat thresh_cut = new Mat(thresh,final_area);
				Imgproc.dilate(sobel_cut, thresh_dilate_cut, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(19,1)),new Point(-1,-1), 9);
				Core.bitwise_or(thresh_dilate_cut, thresh_cut, thresh_dilate_cut);
				Mat thresh_dilate = new Mat(OriginalBW_copy.rows(),OriginalBW_copy.cols(), thresh_dilate_cut.type());
				Mat dst_roi = thresh_dilate.submat(final_area);
				thresh_dilate_cut.copyTo(dst_roi);
				Imgproc.erode(thresh_dilate,thresh_dilate,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
				Imgproc.dilate(thresh_dilate,thresh_dilate,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
				if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/55-morphologyEx.jpg", thresh_dilate);
		        progressDialog.setProgress(49);
							
		        if (DEBUG) Log.d(TAG,"detectTextBoxes(thresh_dilate,OriginalBW_copy,2)");
		    	List<Rect> textBoxes = detectTextBoxes(thresh_dilate,OriginalBW_copy,2);
    			if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/detectTextBoxes_2.jpg", OriginalBW_copy);

    			thresh_dilate_cut.release();
    			thresh_dilate_cut=null;
    			thresh_cut.release();
    			thresh_cut=null;
    			sobel.release();
    			sobel=null;
    			OriginalBW_copy_cut.release();
    			OriginalBW_copy_cut=null;
    			
		    	//Iterator<OcrRect> broiBody = ocrTextBoxes.iterator();
		    	Iterator<Rect> broiBody = textBoxes.iterator();
		    	
		    	progressDialog.setProgress(50);
		    	double step = (double)49/(double)textBoxes.size();
		    	current_progress=50;		    	
		    	int count = 0;
		    	String lotto = "nessuno";
		    	String scadenza = "nessuna";
		    	// quarto ciclo: cerco i prodotti con lotto e scadenza
		    	Rect __roi = null;
		    	List<OcrRect> ocrTextList = null;
		    	OcrRect BW_OrecognizedText = null;
		    	OcrRect AD_OrecognizedText = null;
		    	OcrRect MO_OrecognizedText = null;
		    	JSONObject jobj = null;
		    	String recognizedText_lotto = "";
    			List<OcrRect> ocrTextList_lotto = null;
				OcrRect BW_recognizedText_lotto = null;
				OcrRect AD_recognizedText_lotto = null;
				boolean hasError=false;														
				String first="";
				String last ="";
				String recognizedText_scadenza = "";
				OcrRect BW_recognizedText_scadenza = null;
				OcrRect AD_recognizedText_scadenza = null;
				boolean lottoPresent = false;
				boolean scadenzaPresent = false;
				String[] parts = new String[2];
		    	while(broiBody.hasNext())
		    	{
		    		__roi = broiBody.next();					
		    		if(__roi.width>=0 && __roi.height>=0)
		    		{
			    		try{	                   
				    		if (		    				
//				    				((roi.y+roi.height)>=(prodottoTitleRect.y+prodottoTitleRect.height)) &&  
				    				((__roi.y)>=(prodottoTitleRect.y+prodottoTitleRect.height)) && // originariente: +prodottoTitleRect.height/3   
				    				(
				    						(__roi.x+__roi.width>=prodottoTitleRect.x && __roi.x+__roi.width<=prodottoTitleRect.x+prodottoTitleRect.width) ||
				    						(__roi.x>=prodottoTitleRect.x && __roi.x<=prodottoTitleRect.x+prodottoTitleRect.width) ||
				    						(__roi.x<=prodottoTitleRect.x && __roi.x+__roi.width>=prodottoTitleRect.x+prodottoTitleRect.width)
				    				)
				    			)
				    		{
				    			ocrTextList = new ArrayList<OcrRect>();
				    			if (DEBUG) Log.d(TAG,"roi: "+__roi.x+","+__roi.y+","+__roi.width+","+__roi.height);
				    			__roi = fixRoiBy(__roi,codiciArticoloList);
				    			//if (DEBUG) Imgproc.rectangle(collage, roi.tl(), roi.br(), new Scalar(255,255,255),-1);
				    			// originalBW ------------------------------------------------------------------------------------------------------------------			    	    		
			                    BW_OrecognizedText = GetText(getEnlargedCroppedBitmap(OriginalBW, __roi, count, "BW", false),__roi);
				    			ocrTextList.add(BW_OrecognizedText);
			                    if (DEBUG) Log.d(TAG,"BW - confidence: "+BW_OrecognizedText.getConfidence()+" - OCR:"+BW_OrecognizedText.getText().trim());
				    			// -----------------------------------------------------------------------------------------------------------------------------
				    			if (!checkAppId())
				    			{
					    			// adaptive_threshold ----------------------------------------------------------------------------------------------------------
				                    AD_OrecognizedText = GetText(getEnlargedCroppedBitmap(inputMat, __roi, count, "AD", false),__roi);
				                    AD_OrecognizedText.setConfidence(AD_OrecognizedText.getConfidence()+2); // diamo un po' più di importanza alla versione AD
				                    ocrTextList.add(AD_OrecognizedText);
				                    if (DEBUG) Log.d(TAG,"AD - confidence: "+AD_OrecognizedText.getConfidence()+" - OCR:"+AD_OrecognizedText.getText().trim());
				                    //-------------------------------------------------------------------------------------------------------------------------------
	
					    			// adaptive_threshold_morph -----------------------------------------------------------------------------------------------------
				                    MO_OrecognizedText = GetText(getEnlargedCroppedBitmap(inputMat_morph, __roi, count, "MO", false),__roi);
				                    MO_OrecognizedText.setConfidence(MO_OrecognizedText.getConfidence()); // diamo meno importanza alla versione MO
				                    ocrTextList.add(MO_OrecognizedText);
				                    if (DEBUG) Log.d(TAG,"MO - confidence: "+MO_OrecognizedText.getConfidence()+" - OCR:"+MO_OrecognizedText.getText().trim());
				                    //-------------------------------------------------------------------------------------------------------------------------------
				    			}
			                    count++;
			                    Collections.sort(ocrTextList, new Comparator<OcrRect>(){
				   	    		     public int compare(OcrRect o1, OcrRect o2){
				   	    		         if(o1.getConfidence() == o2.getConfidence())
				   	    		             return 0;
				   	    		         return o1.getConfidence() > o2.getConfidence() ? -1 : 1;
				   	    		     }
			    	    		});			   	    		

/*			                    if (OrecognizedText.getConfidence()<=Oroi.getConfidence())
			                    {
			                    	// se non ho miglioramenti mi tengo il valore vecchio
			                    	// ovvero se il delta di confidence è <=3
			                    	recognizedText = Oroi.getText().trim();
			                    }*/
			                    recognizedText = "";
			                    if(ocrTextList.size()>0)
			                    {				                    
			                    	recognizedText = ocrTextList.get(0).getText().trim();
			                    	if (recognizedText.length()>2)
			                    	{
				                    	if (recognizedText.substring(0,2).toLowerCase(Locale.ITALIAN).equals("i ") || recognizedText.substring(0,2).toLowerCase(Locale.ITALIAN).equals("l ") || recognizedText.substring(0,2).toLowerCase(Locale.ITALIAN).equals("1 "))
				                    	{
				                    		recognizedText = recognizedText.substring(2);
				                    	}
			                    	}
				                    if (DEBUG) Log.d(TAG,"BEST - confidence: "+ocrTextList.get(0).getConfidence()+" - OCR:"+recognizedText);
				                    if (DEBUG) Log.d(TAG,"----------------------------------------------------");
			                    }
/*		    					if (recognizedText.indexOf("|")>-1)
		    					{
		    						String[] tmp = recognizedText.split("\\|");
		    						recognizedText = tmp[tmp.length-1].trim();
//			    					OrecognizedText.setText(recognizedText);
		    					}*/
			    				if (recognizedText.length()>=6 && recognizedText.length()<=60)
			    				{
					    			try
									{
							    		if(stessaLinea_lotto || stessaLinea_scadenza)
							    		{
								    		jobj = new JSONObject();
								    		jobj.put("index", count);
								    		
											String Tfirst = recognizedText.substring(0, 1);
/*											if (Tfirst.equals("|"))
											{
												recognizedText = recognizedText.substring(1);
											}*/
											if (Tfirst.equals("'") || Tfirst.equals(".") || Tfirst.equals(","))
											{
												recognizedText = recognizedText.substring(1);
											}
								    		
											if (DEBUG) Log.d(TAG,"Prodotto: "+recognizedText);
											jobj.put("prodotto", recognizedText);										
											// qui devo cercare sulla stessa riga a destra lotto e scadenza
											if (stessaLinea_lotto)
											{
												if (DEBUG) Log.d(TAG,"Lotto: ---------------------------------------------------------");
												recognizedText_lotto = "";
								    			ocrTextList_lotto = new ArrayList<OcrRect>();
												BW_recognizedText_lotto = findNearestRightSameLine(lottoTitleRect, __roi, OriginalBW,"LOTT-BW");
												if (BW_recognizedText_lotto!=null)
												{
													ocrTextList_lotto.add(BW_recognizedText_lotto);
													if (DEBUG) Log.d(TAG,"BW - confidence: "+BW_recognizedText_lotto.getConfidence()+" - OCR:"+BW_recognizedText_lotto.getText().trim());
												}
												if (!checkAppId())
												{
													AD_recognizedText_lotto = findNearestRightSameLine(lottoTitleRect, __roi, inputMat,"LOTT-AD");
													if (AD_recognizedText_lotto!=null)
													{
														ocrTextList_lotto.add(AD_recognizedText_lotto);
														if (DEBUG) Log.d(TAG,"AD - confidence: "+AD_recognizedText_lotto.getConfidence()+" - OCR:"+AD_recognizedText_lotto.getText().trim());
													}
													/*OcrRect MO_recognizedText_lotto = findNearestRightSameLine(lottoTitleRect, roi, inputMat_morph,"LOTT-MO");												
													if (MO_recognizedText_lotto!=null)
													{
														ocrTextList_lotto.add(MO_recognizedText_lotto);
														if (DEBUG) Log.d(TAG,"MO - confidence: "+MO_recognizedText_lotto.getConfidence()+" - OCR:"+MO_recognizedText_lotto.getText().trim());
													}*/
												}
							                    Collections.sort(ocrTextList_lotto, new Comparator<OcrRect>(){
								   	    		     public int compare(OcrRect o1, OcrRect o2){
								   	    		         if(o1.getConfidence() == o2.getConfidence())
								   	    		             return 0;
								   	    		         return o1.getConfidence() > o2.getConfidence() ? -1 : 1;
								   	    		     }
							    	    		});			   	    		
							                    if (ocrTextList_lotto.size()>0)
							                    {
							                    	recognizedText_lotto = ocrTextList_lotto.get(0).getText().trim();
								                    if (DEBUG) Log.d(TAG,"BEST - confidence: "+ocrTextList_lotto.get(0).getConfidence()+" - OCR:"+recognizedText_lotto);
								                    if (DEBUG) Log.d(TAG,"----------------------------------------------------");
							                    }
												if (recognizedText_lotto.length()>=2)
												{
													hasError=true;														
													first = recognizedText_lotto.substring(0, 2).toLowerCase(Locale.ITALIAN);
													if (DEBUG) Log.d(TAG,"first char: '"+first+"'");
													if (first.equals("1 ") && recognizedText_lotto.length()>1)
													{
														recognizedText_lotto = recognizedText_lotto.substring(2).trim();
													}
													while (hasError && recognizedText_lotto.length()>0)
													{
														first = recognizedText_lotto.substring(0, 1).toLowerCase(Locale.ITALIAN);
														if (DEBUG) Log.d(TAG,"first char: '"+first+"'");
														try{
															Integer.parseInt(first);
															hasError=false;
														}
														catch(NumberFormatException nfe)
														{
															recognizedText_lotto = recognizedText_lotto.substring(1).trim();
														}
													}														
													hasError=true;																												
													while (hasError && recognizedText_lotto.length()>0)
													{
														last = recognizedText_lotto.substring(recognizedText_lotto.length()-1).toLowerCase(Locale.ITALIAN);
														if (DEBUG) Log.d(TAG,"last char: '"+last+"'");
														try{
															Integer.parseInt(last);
															hasError = false;
														}
														catch(NumberFormatException nfe)
														{
															recognizedText_lotto = recognizedText_lotto.substring(0,recognizedText_lotto.length()-1).trim();
														}														
														
													}
												}
												else
												{
													recognizedText_lotto = "nessuno";
												}
												if (recognizedText_lotto.trim().length()==1 || recognizedText_lotto.trim().equals(""))
												{
													recognizedText_lotto = "nessuno";
												}
												if (DEBUG) Log.d(TAG,"Lotto: "+recognizedText_lotto.trim());
												jobj.put("lotto", recognizedText_lotto.trim());
											}
											else
											{
												jobj.put("lotto", "nessuno");
											}
											if (stessaLinea_scadenza)
											{
												if (DEBUG) Log.d(TAG,"Scadenza: ---------------------------------------------------------");
												recognizedText_scadenza = ""; //findNearestRightSameLine(scadenzaTitleRect, roi, OriginalBW).trim().replaceAll("\\s+", "");
								    			List<OcrRect> ocrTextList_scadenza = new ArrayList<OcrRect>();
								    			//if (DEBUG) Imgproc.rectangle(collage, roi.tl(), roi.br(), new Scalar(255,255,255));
								    			
												BW_recognizedText_scadenza = findNearestRightSameLine(scadenzaTitleRect, __roi, OriginalBW,"SCAD-BW");
												if (BW_recognizedText_scadenza!=null)
												{
													ocrTextList_scadenza.add(BW_recognizedText_scadenza);
													if (DEBUG) Log.d(TAG,"BW - confidence: "+BW_recognizedText_scadenza.getConfidence()+" - OCR:"+BW_recognizedText_scadenza.getText().trim());
												}
												AD_recognizedText_scadenza = findNearestRightSameLine(scadenzaTitleRect, __roi, inputMat,"SCAD-AD");
												if (AD_recognizedText_scadenza!=null)
												{
													ocrTextList_scadenza.add(AD_recognizedText_scadenza);
													if (DEBUG) Log.d(TAG,"AD - confidence: "+AD_recognizedText_scadenza.getConfidence()+" - OCR:"+AD_recognizedText_scadenza.getText().trim());
												}
												/*OcrRect MO_recognizedText_scadenza = findNearestRightSameLine(scadenzaTitleRect, roi, inputMat_morph,"SCAD-MO");												
												if (MO_recognizedText_scadenza!=null)
												{
													ocrTextList_scadenza.add(MO_recognizedText_scadenza);
													if (DEBUG) Log.d(TAG,"MO - confidence: "+MO_recognizedText_scadenza.getConfidence()+" - OCR:"+MO_recognizedText_scadenza.getText().trim());
												}*/
												if (AD_recognizedText_scadenza.getText().split("/").length==2 && BW_recognizedText_scadenza.getText().split("/").length==AD_recognizedText_scadenza.getText().split("/").length)
												{
								                    Collections.sort(ocrTextList_scadenza, new Comparator<OcrRect>(){
									   	    		     public int compare(OcrRect o1, OcrRect o2){
									   	    		         if(o1.getConfidence() == o2.getConfidence())
									   	    		             return 0;
									   	    		         return o1.getConfidence() > o2.getConfidence() ? -1 : 1;
									   	    		     }
								    	    		});			   	    															
												}
												else
												{
								                    Collections.sort(ocrTextList_scadenza, new Comparator<OcrRect>(){
									   	    		     public int compare(OcrRect o1, OcrRect o2){
									   	    		         if(o1.getConfidence() == o2.getConfidence())
									   	    		             return 0;
									   	    		         return o1.getText().split("/").length > o2.getText().split("/").length ? -1 : 1;
									   	    		     }
								    	    		});			   	    															
													
												}
							                    if (ocrTextList_scadenza.size()>0)
							                    {
							                    	recognizedText_scadenza = ocrTextList_scadenza.get(0).getText().trim();
								                    if (DEBUG) Log.d(TAG,"BEST - confidence: "+ocrTextList_scadenza.get(0).getConfidence()+" - OCR:"+recognizedText_scadenza);
								                    if (DEBUG) Log.d(TAG,"----------------------------------------------------");							                    	
							                    }												
												if (recognizedText_scadenza.length()>0)
												{
													hasError=true;	
													first = recognizedText_scadenza.substring(0, 1).toLowerCase(Locale.ITALIAN);
													if (DEBUG) Log.d(TAG,"first char: "+first);
													while (hasError && recognizedText_scadenza.length()>0)
													{
														first = recognizedText_scadenza.substring(0, 1).toLowerCase(Locale.ITALIAN);
														try{
															Integer.parseInt(first);
															hasError=false;
														}
														catch(NumberFormatException nfe)
														{
															recognizedText_scadenza = recognizedText_scadenza.substring(1,recognizedText_scadenza.length());
														}														
													}
													hasError=true;	
													while (hasError && recognizedText_scadenza.length()>0)
													{
														try{
															last = recognizedText_scadenza.substring(recognizedText_scadenza.length()-1).toLowerCase(Locale.ITALIAN);
															if (DEBUG) Log.d(TAG,"last char: "+last);
															Integer.parseInt(last);	
															hasError=false;
														}
														catch(NumberFormatException nfe)
														{
															recognizedText_scadenza = recognizedText_scadenza.substring(0,recognizedText_scadenza.length()-1);
														}														
													}
													if (recognizedText_scadenza.length()>1)
													{
														recognizedText_scadenza = fixYear(recognizedText_scadenza);
													}
												}
												if (recognizedText_scadenza.trim().length()==1 || recognizedText_scadenza.trim().equals(""))
												{
													recognizedText_scadenza = "nessuna";
												}
												if (DEBUG) Log.d(TAG,"Scadenza: "+recognizedText_scadenza);
												jobj.put("scadenza", recognizedText_scadenza);
											}
											else
											{
												jobj.put("scadenza", "nessuna");
											}
											/*jobj.put("usati", "0");
											jobj.put("quantita", "0");
											jobj.put("note", "---");*/
											jobj.put("conforme", "Conforme");
											jrows.put(jobj);
											//Imgproc.rectangle(OriginalBW, roi.tl(), roi.br(), new Scalar(100,100,100),1);											
							    		}
							    		else
							    		{
							    			// devo cercare i valori attorno il testo del prodotto, tipicamente al di sotto per ogni riga
							    			// l'area di OCR se fatta bene dovrebbe gia' inglobare tutto, pertanto nel testo OCR dovrebbe
							    			// essere presente una stringa tipo: Lotto: 107 Scadenza: 30/09/2015
//											String stringToFind = Cimino.toSlug(recognizedText.toLowerCase(Locale.ITALIAN)); // .replace("lollo", "lotto")							    			
											recognizedText = recognizedText.replace("lollo", "Lotto"); // FIX							    			
											recognizedText = recognizedText.replace("Lollo", "Lotto"); // FIX							    			
							    			// devo fare attenzione perche' alternativamente lotto o scadenza potrebbero mancare											
											if ( 
													(
														(
															recognizedText.indexOf("Lotto")>-1
//															 || StringSimilarity.similarity(recognizedText, "lotto")>=0.6
														)
													||
														(
															recognizedText.indexOf("Scadenza")>-1
//															 || StringSimilarity.similarity(recognizedText, "scadenza")>=0.6
														)
														
													)
												)
											{

												lottoPresent = recognizedText.indexOf("Lotto")>-1;
												scadenzaPresent = recognizedText.indexOf("Scadenza")>-1;
												if (DEBUG) Log.d(TAG,"Campo Lotto e/o Scadenza trovato!");	
												// estraggo lotto e scadenza per assegnarli al successivo prodotto
												// dato che la sequenza di lettura dal basso verso l'alto è
												// lotto-scad
												// prodotto
												// lotto-scad
												// prodotto
												// ...
												recognizedText = recognizedText.replace("otto", "otto:");
												recognizedText = recognizedText.replace("cadenza", "cadenza:");
												recognizedText = recognizedText.replace("::",":").replace(": ", ":").trim();
												first = recognizedText.substring(0, 1);
												if (!first.equals("L"))
												{
													recognizedText = recognizedText.substring(1).trim();
												}
/*												if (!first.equals("L"))
												{
													recognizedText = recognizedText.substring(1).trim();
												}*/
												if (DEBUG) Log.d(TAG,"Prodotto: "+recognizedText);
												parts = new String[2];
												if (scadenzaPresent && lottoPresent)
												{
													parts = recognizedText.replace("Lotto:", "").split(" Scadenza:");
												}
												else if (!lottoPresent && scadenzaPresent)
												{
													parts = ("Lotto: "+lotto+" "+recognizedText).replace("Lotto:", "").split(" Scadenza:");
												}
												else if(lottoPresent && !scadenzaPresent)
												{
													parts = (recognizedText+" Scadenza: "+scadenza).replace("Lotto:", "").split(" Scadenza:");
												}
												if (DEBUG) Log.d(TAG,"parts: "+parts.length);

												lotto = parts[0].trim();
												scadenza = parts[1].trim();
												if (DEBUG) Log.d(TAG,"RESULT => Lotto: "+lotto+" - Scadenza: "+scadenza);
												if (DEBUG) Log.d(TAG,"----------------------------------------------------");
											}
											else
											{
									    		jobj = new JSONObject();
									    		jobj.put("index", count);												
												jobj.put("prodotto", recognizedText);												
												jobj.put("lotto", lotto);												
												jobj.put("scadenza", scadenza);
												jobj.put("conforme", "Conforme");
												/*jobj.put("usati", "0");
												jobj.put("quantita", "0");
												jobj.put("note", "---");*/												
												jrows.put(jobj);
												lotto = "nessuno";
												scadenza = "nessuna";
											}
							    		}
																			
									}
									catch (JSONException e)
									{
										e.printStackTrace();
									}
			    				}

				    		}
			    	    	current_progress+=step;
			    	    	progressDialog.setProgress((int)current_progress);
			    		}
			    		catch(CvException cve)
			    		{
			    			if (DEBUG) Log.d(TAG,cve.getLocalizedMessage());
			    			cve.printStackTrace();
			    		}
						catch (Exception e1)
						{
							if (DEBUG) Log.d(TAG,e1.getLocalizedMessage());
							e1.printStackTrace();
						}
		    		}
		    	}
		    	try
				{
					finalData.put("rowdata", jrows);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
		    	baseApi.clear();
		    	baseApi.end();
				baseApi=null;	    		
		        
				//String imageFile_roiMerge = getTempDirectoryPath()+"/9-image_rois.jpg";
				//if (DEBUG) Imgcodecs.imwrite(imageFile_roiMerge, collage);	    	
		    	
		    	//String headerFile_ocr="";
		    	/*cropped = new Mat(quad,roi);
		   		String headerFile_ocr = getTempDirectoryPath()+"/headerOcr.jpg";
		    	if (DEBUG) Imgcodecs.imwrite(headerFile_ocr, cropped);*/

		    	resultIntent.putExtra("imageFile", imageFile);
		    	resultIntent.putExtra("headerFile", "");
//		    	resultIntent.putExtra("imageFileOcr", imageFile_ocr);
//		    	resultIntent.putExtra("headerFileOcr", headerFile_ocr);
		    	resultIntent.putExtra("productRows", finalData.toString());
		    	resultIntent.putExtra("realSizePixelRatio", realSizePixelRatio);		    	
		    	progressDialog.setProgress(100);
		    	progressDialog.dismiss();
		    	progressDialog=null;
		    	thresh_dilate.release();
				thresh_dilate = null;
		    	prodottoTitleRect = null;
		    	articoloTitleRect = null;
		    	lottoTitleRect = null;
		    	scadenzaTitleRect = null;
		    	ocrTextList = null;
		    	BW_OrecognizedText = null;
		    	AD_OrecognizedText = null;
		    	MO_OrecognizedText = null;
		    	jobj = null;
		    	recognizedText_lotto = null;
    			ocrTextList_lotto = null;
				BW_recognizedText_lotto = null;
				AD_recognizedText_lotto = null;
				first = null;
				last = null;
				recognizedText_scadenza = null;
				BW_recognizedText_scadenza = null;
				AD_recognizedText_scadenza = null;
				jrows=null;
	    	}
	    	else
	    	{	    		
//				imageFile_ocr = getTempDirectoryPath()+"/9-image_detectLetters.jpg";
//				if (DEBUG) Imgcodecs.imwrite(imageFile_ocr, OriginalBW);	    	

	    		resultIntent.putExtra("imageFile", "");
	    		resultIntent.putExtra("headerFile", "");
		    	resultIntent.putExtra("productRows", "");
		    	resultIntent.putExtra("realSizePixelRatio", realSizePixelRatio);
	    		status=false;
	    	}
			//sobel_dilated = null;
	    	thresh.release();
			thresh = null;
			inputMat.release();
			inputMat=null;
			inputMat_morph.release();
			inputMat_morph=null;
			OriginalBW.release();
			OriginalBW=null;
			OriginalBW_copy.release();
			OriginalBW_copy=null;
			OriginalBW_copy2.release();
			OriginalBW_copy2=null;
	    	roi=null;
	    	if (cropped!=null) cropped.release();
	    	cropped=null;
	    	if (Ocropped!=null) Ocropped.release();
	    	Ocropped=null;
	    	if (Inv_Ocropped!=null) Inv_Ocropped.release();
	    	Inv_Ocropped=null;
	    	if (thresh_dilate_cut!=null) thresh_dilate_cut.release();
			thresh_dilate_cut=null;
	    	if(bitmap!=null) bitmap.recycle();
	    	bitmap=null;
	    	if(Obitmap!=null) Obitmap.recycle();
	    	Obitmap=null;
	    	if(Inv_Obitmap!=null) Inv_Obitmap.recycle();
	    	Inv_Obitmap=null;
	    	recognizedText=null;
	    	OrecognizedText=null;
	    	Inv_OrecognizedText=null;
	    	stringToFind=null;
	    	OstringToFind=null;
			Inv_OstringToFind=null; 
			finalData=null;
    		leftColumn=null;


			//collage=null;
			//baseApi.clear();
			//baseApi.end();
	    	setResult(RESULT_OK, resultIntent);
	    	if (DEBUG) Log.d(TAG,"###### doInBackground finish");
			return status;
		}
		
		private boolean intersect_Y(Rect A, Rect B)
		{
			return (B.tl().y>=A.tl().y && B.tl().y<=A.br().y) 
					|| (A.tl().y<=B.br().y && A.tl().y>=B.tl().y);
		}

		private boolean intersect_X(Rect A, Rect B)
		{
			return (B.tl().x>=A.tl().x && B.tl().x<=A.br().x) 
					|| (A.tl().x<=B.br().x && A.tl().x>=B.tl().x);
		}
		
		private Rect fixRoiBy(Rect roi, List<Rect> splitters)
		{
			Iterator<Rect> it = splitters.iterator();
			boolean found = false;
			while (it.hasNext() && !found)
			{
				Rect splitter = it.next();
				if(intersect_X(roi, splitter) && intersect_Y(roi, splitter))
				{
					// il 10 vedo se mi aiuta ad evitare la barretta verticale della tabella che viene spesso interpretata per I,1 o |
					roi = new Rect(splitter.x+splitter.width+15,roi.y+2,(roi.x+roi.width)-(splitter.x+splitter.width),roi.height-4);
					found=true;
				}
			}
			return roi;
		}
		
		private String fixYear(String data)
		{			
			if (data.length()>1)
			{
				boolean parseError = true;
				String first = data.substring(0,1).toLowerCase(Locale.ITALIAN).replace("f", "/");
				while (parseError && data.length()>1)
				{
					try
					{
						first = data.substring(0,1).toLowerCase(Locale.ITALIAN).replace("f", "/");
						Integer.parseInt(first);
						parseError=false;
					}
					catch(NumberFormatException nfe)
					{
						data = data.substring(1).trim();
					}
				}
				
			}
			if (data.length()>1)
			{
				boolean parseError = true;
				String last = data.substring(data.length()-1).toLowerCase(Locale.ITALIAN);
				while (parseError && data.length()>1)
				{
					try
					{
						last = data.substring(data.length()-1).toLowerCase(Locale.ITALIAN);
						Integer.parseInt(last);
						parseError = false;
					}
					catch(NumberFormatException nfe)
					{
						data = data.substring(0,data.length()-1).trim();
					}
				}				
			}
			if (data.length()>1)
			{
				data = data.replaceAll("-", "/").replaceAll("\\s","");
				String[] parts = data.split("/");
				if (data.length()==8 && parts.length<3)
				{
					data = data.substring(0,1)+"/"+data.substring(3,4)+"/20"+data.substring(6,7);
				}
				else if (data.length()<8 && parts.length==3)
				{
					String year = parts[2];
					String month = (parts[1].length()==1 ? "0":"")+parts[1];
					String day = (parts[1].length()==1 ? "0":"")+parts[0];
					if (year.length()==2)
					{
						year="20"+year;
					}
					else if(year.length()==1)
					{
						Calendar now = Calendar.getInstance();
						now.add(Calendar.YEAR,1);
						Date currentDate = now.getTime();
						java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy",Locale.ITALIAN);
						year = simpleDateFormat.format(currentDate);
					}			
					data = day+"/"+month+"/"+year;
					
				}
				else if (data.length()<8 && parts.length==2)
				{
					String year = parts[1];
					String month = (parts[0].length()==1 ? "0":"")+parts[0];
					
					if (year.length()==2)
					{
						year="20"+year;
					}
					else if(year.length()==1)
					{
						Calendar now = Calendar.getInstance();
						now.add(Calendar.YEAR,1);
						Date currentDate = now.getTime();
						java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy",Locale.ITALIAN);
						year = simpleDateFormat.format(currentDate);
					}								
					data = "01/"+month+"/"+year;
				}
				else if(data.length()==8 && parts.length==3)
				{
					data = parts[0]+"/"+parts[1]+"/20"+parts[2];
				}
			}
			return data;
		}
		
		private OcrRect findNearestRightSameLine(Rect roiColumn, Rect roiRow, Mat image, String prefix) throws Exception
		{			
			OcrRect Oresult = null;
			if (roiColumn!=null)
			{
//				Rect searchArea = new Rect(roiColumn.x-25,roiRow.y-15,roiColumn.width+50,roiRow.height+40);
				Rect searchArea = new Rect(new Point(roiColumn.x-10,roiRow.y-10), new Point(roiColumn.x+roiColumn.width+20,roiRow.y+roiRow.height+20));
				if (searchArea.x+searchArea.width>image.cols())
				{
					int diff = searchArea.x+searchArea.width-image.cols();
					searchArea.width = searchArea.width-diff;
				}
				if (searchArea.y+searchArea.height>image.rows())
				{
					int diff = searchArea.y+searchArea.height-image.rows();
					searchArea.height = searchArea.height-diff;
				}
	    		Mat cropped = new Mat(image, searchArea);     
                Bitmap bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped, bitmap);
                if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/"+prefix+"_cropped_"+roiRow.y+".jpg", cropped);
                Oresult = GetText(bitmap,searchArea);
                bitmap.recycle();
                bitmap=null;
                cropped.release();
                cropped = null;
			}
			return Oresult;
		}
		/*
		private boolean intersect(Rect A, Rect B)
		{
			return (A.contains(B.tl()) || A.contains(B.br()) || A.contains(new Point(B.x+B.width,B.y)) || A.contains(new Point(B.x,B.y+B.height)) ) ||
					(B.contains(A.tl()) || B.contains(A.br()) || B.contains(new Point(A.x+A.width,A.y)) || B.contains(new Point(A.x,A.y+A.height)) );			
			//return (!( (A.tl().x > B.br().x) || (B.tl().x > A.br().x) || (A.tl().y < B.br().y) || (B.tl().y < A.br().y)));
		}
		
		private String findNearestRightSameLine(Rect roiColumn, Rect roiRow, List<Rect>rois, Mat image) throws Exception
		{
			String result = "";
			if (roiColumn!=null)
			{
				Rect searchArea = new Rect(roiColumn.x,roiRow.y,roiColumn.width,roiRow.height);
		    	Iterator<Rect> broi = rois.iterator();
		    	while(broi.hasNext() && result=="")
		    	{
		    		Rect roi = broi.next();
		    		if ( roi.area()>20 && intersect(searchArea,roi) )
		    		{
			    		Mat cropped = new Mat(image, roi);     
	                    Bitmap bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
	                    Utils.matToBitmap(cropped, bitmap);
	                    OcrRect Oresult = GetText(bitmap,roi);
	                    result = Oresult.getText();
//	    				baseApi.setImage(bitmap);
//	    				result = baseApi.getUTF8Text();		    			
		    		}
		    	}
			}
			return result;
		}		 
		 */
		
		private List<Rect> detectTextBoxes(Mat inputMat, Mat Original, int idx)
		{
			List<Rect> boundRect = new ArrayList<Rect>();
//			Mat element = new Mat();	
			Mat hierachy = new Mat();
			
			List<MatOfPoint> contours = new ArrayList<MatOfPoint>();			
			Imgproc.findContours(inputMat, contours, hierachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
			Iterator<MatOfPoint> ci = contours.iterator();
			
			while(ci.hasNext())			
			{				
				MatOfPoint2f contour = new MatOfPoint2f(ci.next().toArray());
				MatOfPoint2f approx = new MatOfPoint2f();
				//double epsilon = Imgproc.arcLength(contour, true) * 0.02;
				Imgproc.approxPolyDP( contour, approx, 3, true);
				Rect appRect = Imgproc.boundingRect(new MatOfPoint(approx.toArray()) );
				if (DEBUG) Log.d(TAG,"Area: "+contour.size().area()+" -height: "+appRect.height);
				if(contour.size().area()>10 && contour.size().area()<1000)
				{
					if(appRect.height>10 && appRect.width>10 && appRect.height<=150)
					{
						boundRect.add(appRect);		
						if (DEBUG) Imgproc.rectangle(Original, appRect.tl(), appRect.br(), new Scalar(200,200,200),2);						
					}

				}
			}			
			if (DEBUG) Imgcodecs.imwrite( getTempDirectoryPath()+"/detectTextBoxes_"+idx+".jpg", Original);
			ci = null;
			//element = null;
			hierachy.release();
			hierachy = null;
			return boundRect;					
		}
	}

    /* ABBYY */
    
    private OcrRect GetText(Bitmap bitmap, Rect roi) throws Exception
    {
    	String result = "";
    	int confidence = -1;
        if (checkAppId())
        {
        	String xmlresult = performTextFieldRecognition(bitmap);
        	if (DEBUG) Log.d(TAG,"xml: "+xmlresult);
        	if (!xmlresult.equals("gotesseract"))
        	{
            	DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            	InputSource is = new InputSource();
            	is.setCharacterStream(new StringReader(xmlresult));
            	org.w3c.dom.Document doc = db.parse(is);
            	
            	NodeList nodes = doc.getElementsByTagName("value");
            	for (int i = 0; i < nodes.getLength(); i++) {
            		Node node = nodes.item(i);
            		result += node.getTextContent()+" ";
            	}

        	}
        	else
        	{
            	// se Abbyy non funziona ricorro a Tesseract
                baseApi.setImage(bitmap);          	
                result = baseApi.getUTF8Text().replace("\n", "");
                confidence = baseApi.meanConfidence();        		
        	}        		
        }            	
        else
        {
        	// se Abbyy non funziona ricorro a Tesseract
            baseApi.setImage(bitmap);          	
            result = baseApi.getUTF8Text().replace("\n", "");
            confidence = baseApi.meanConfidence();
        }
        bitmap.recycle();
        bitmap=null;
        return new OcrRect(roi,result,confidence);
    }
    
	private String performTextFieldRecognition(Bitmap bitmap) throws Exception {
		String options = extractExtraOptions(abbyyArgList);

		TextFieldSettings settings = new TextFieldSettings();
		settings.setLanguage(abbyLanguage);
		if (options != null) {
			settings.setOptions(options);
		}

		System.out.println("Uploading..");
		Task task = restClient.processTextFieldByBitmap(bitmap, settings);
		
		return waitAndDownloadResult(task);
	}

	/** 
	 * Wait until task processing finishes
	 */
	private Task waitForCompletion(Task task) throws Exception {
		// Note: it's recommended that your application waits
		// at least 2 seconds before making the first getTaskStatus request
		// and also between such requests for the same task.
		// Making requests more often will not improve your application performance.
		// Note: if your application queues several files and waits for them
		// it's recommended that you use listFinishedTasks instead (which is described
		// at http://ocrsdk.com/documentation/apireference/listFinishedTasks/).
		while (task.isTaskActive()) {

			Thread.sleep(5000);
			System.out.println("Waiting..");
			task = restClient.getTaskStatus(task.Id);
		}
		return task;
	}
	
	/**
	 * Wait until task processing finishes and download result.
	 */
	private String waitAndDownloadResult(Task task)
			throws Exception {
		task = waitForCompletion(task);
		String result = "";
		if (task.Status == Task.TaskStatus.Completed) {
			System.out.println("Downloading..");
			result = getResult(task);
			System.out.println("Ready");
		} else if (task.Status == Task.TaskStatus.NotEnoughCredits) {
			System.out.println("Not enough credits to process document. "
					+ "Please add more pages to your application's account.");
			result = "gotesseract";
		} else {
			System.out.println("Task failed");
		}
		return result;
	}
	
	/**
	 * Extract extra RESTful options from command-line parameters. Parameter is
	 * removed after extraction
	 * 
	 * @return extra options string or null
	 */
	private String extractExtraOptions(Vector<String> args) {
		// Extra options parameter has from --options=<options>
		return CmdLineOptions.extractParameterValue("options", args);
	}

	public String getResult(Task task) throws Exception {
		if (task.Status != Task.TaskStatus.Completed) {
			throw new IllegalArgumentException("Invalid task status");
		}

		if (task.DownloadUrl == null) {
			throw new IllegalArgumentException(
					"Cannot download result without url");
		}

		URL url = new URL(task.DownloadUrl);
		URLConnection connection = url.openConnection(); // do not use
															// authenticated
															// connection
	
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
		String result = "";
//		byte[] data = new byte[2048];
//		int count = 0;
		String str = "";
//		while ((count = reader.read(data, 0, data.length)) != -1) {
		while ((str = reader.readLine())!=null) {
//			String str = new String(data, StandardCharsets.UTF_8);
			result+=str;
		}
		
		return result;
	}
	
	private Bitmap getEnlargedCroppedBitmap(Mat image, Rect roi, int count, String prefix, boolean enlarge)
	{
		Mat cropped = new Mat(image,roi);		                    
        Bitmap bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
        if (enlarge)
        {
            Utils.matToBitmap(cropped, bitmap);
            Bitmap enlargedBitmap = Bitmap.createBitmap(cropped.cols()+50, cropped.rows()+50, Bitmap.Config.ARGB_8888);		                    
            Canvas wideBmpCanvas = new Canvas(enlargedBitmap);
            wideBmpCanvas.drawColor(Color.WHITE);
            wideBmpCanvas.drawBitmap(bitmap, 25, 25, null);
            Mat new_bitmap = new Mat();
            Utils.bitmapToMat(enlargedBitmap, new_bitmap);
            //if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/"+prefix+"_cropped_"+(count++)+".jpg", new_bitmap);
            cropped.release();
    		cropped = null;
    		bitmap.recycle();
    		bitmap = null;
    		return enlargedBitmap;		
        }
        else
        {
        	Utils.matToBitmap(cropped, bitmap);
            cropped.release();
    		cropped = null;
            //if (DEBUG) Imgcodecs.imwrite(getTempDirectoryPath()+"/"+prefix+"_cropped_"+(count++)+".jpg", cropped);
    		return bitmap;		
        }
	}
} 

