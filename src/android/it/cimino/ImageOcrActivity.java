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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageOcrActivity extends Activity{
    private static final String TAG = "Cimino::ImageOCR";

    private ImageView selected_photo;
    private Bitmap inputBitmap;
    private ProgressDialog progressDialog;
    @SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private String currentDateandTime = "";
    private String currentFileName  = "";
    boolean bIsPictureTaking = false; 
    private TessBaseAPI baseApi = null;
    private Client restClient;
    private String abbyLanguage = "Italian";
    private Vector<String> abbyyArgList;
    private String charWhiteList = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890'.,:/|-àèìòùé";
    private String charWhiteListLetters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
      
    public ImageOcrActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());   
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
			System.out.println("Error: No application id and password are specified.");
			System.out.println("Please specify them in ClientSettings.java.");
			return false;
		}
		return false; //true;
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
                    Log.i(TAG, "OpenCV loaded successfully");
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
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        
        Bundle extras = getIntent().getExtras(); 
        currentFileName = extras.getString("filepath");
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.ocr_view);
        selected_photo = (ImageView) findViewById(R.id.selected_photo);
        File imgFile = new File(currentFileName);
        
        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        
        selected_photo.setImageBitmap(myBitmap);
                
		Log.d(TAG,"###### new ProgressDialog");
		progressDialog = new ProgressDialog(ImageOcrActivity.this);
		progressDialog.setMessage("Attendere, elaborazione in corso...");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setProgress(0);  	    	
		progressDialog.setMax(100);        
		progressDialog.setCancelable(false);
		Log.d(TAG,"###### progressDialog.show()");
    	progressDialog.show();           	    
    	
    	currentDateandTime = sdf.format(new Date());
    	currentFileName = getTempDirectoryPath() + "/sample_picture_" + currentDateandTime + ".jpg";

		Log.d(TAG,"###### mOpenCvCameraView.takePicture start");
        bIsPictureTaking = true;  //set it to true to avoid onKeyDown dispatching during taking picture. it may be time-consuming
        this.doBackgroundTask();
		Log.d(TAG,"###### mOpenCvCameraView.takePicture end");
		
    }

    public void doBackgroundTask()
    {
		Log.d(TAG,"###### IAmABackgroundTask().execute() start");
    	new IAmABackgroundTask().execute();
    	Log.d(TAG,"###### IAmABackgroundTask().execute() end");
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
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
    	if ( progressDialog!=null && progressDialog.isShowing() ){
            progressDialog.dismiss();
        }
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
		    Log.d(TAG,"###### IAmABackgroundTask.onPreExecute");
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			Log.d(TAG,"###### IAmABackgroundTask.onPostExecute");

		    if (ImageOcrActivity.this != null) {
	        	if ( progressDialog!=null && progressDialog.isShowing() ){
	                progressDialog.dismiss();
	                ImageOcrActivity.this.finish();
	            }
	    	}
		
		}
		
		@SuppressLint("SimpleDateFormat")
		@Override
		protected Boolean doInBackground(String... params) {
			boolean status = true;
	    	Intent resultIntent = new Intent();
			Log.d(TAG,"###### IAmABackgroundTask.doInBackground");
			// qui devo caricare nell'ImageView l'immagine da file

			Log.d(TAG,"###### progressDialog.setProgress");
	        
//		    inputBitmap = BitmapFactory.decodeByteArray(data,0,data.length); //changeBitmapContrastBrightness(,1,0);
		    progressDialog.setProgress(10);

		    Bitmap rotatedBitmap = null;
	        
	        if(inputBitmap.getWidth()>inputBitmap.getHeight()) // devi controllare l'orientamento (width>height) 
	        {
		        // 1) devo ruotare la bitmap di 90 gradi in senso orario
		        // l'immagine da 3246x2448, deve diventare 2448x3246
		        Matrix matrix = new Matrix();
		        matrix.setRotate(90, 0, 0);
		        rotatedBitmap = Bitmap.createBitmap(inputBitmap , 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);
		        inputBitmap.recycle();
		        inputBitmap=null;
	        }
	        else
	        {
	        	rotatedBitmap = inputBitmap;
	        }
	        
	        progressDialog.setProgress(20);

	        Mat inputMat = new Mat(rotatedBitmap.getHeight(), rotatedBitmap.getWidth(), CvType.CV_8UC3);            
	        Utils.bitmapToMat(rotatedBitmap, inputMat);
	        
	    	float realSizePixelRatio = 0; // px/mm
	        
	        String imageFile = getTempDirectoryPath()+"/resultImage_"+currentDateandTime+".jpg";
	    	//Imgcodecs.imwrite(imageFile, quad);
	        Mat finalMat = new Mat();
	        Imgproc.cvtColor(inputMat, finalMat, Imgproc.COLOR_BGR2RGB);
	    	Imgcodecs.imwrite(imageFile, finalMat);
	    	finalMat=null;
	    	
            baseApi = new TessBaseAPI();
			// DATA_PATH = Path to the storage
			// lang = for which the language data exists, usually "eng"
			//baseApi.init(DATA_PATH, lang);
            //baseApi.setPageSegMode(TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED);
            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
            baseApi.setDebug(true);
			baseApi.init(getFileDirectoryPath(), "ita"); // /tessdata/ita.traineddata
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, charWhiteListLetters);
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "\\s");
			
			progressDialog.setProgress(30);
	    	Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_BGR2GRAY);
			Mat OriginalBW = inputMat.clone();
			Mat OriginalBW_copy = inputMat.clone();
			Mat OriginalBW_copy2 = inputMat.clone();
	    	Imgproc.GaussianBlur(inputMat, inputMat, new Size(5, 5), 5); //denoise
	    	Imgproc.medianBlur(inputMat, inputMat, 5); 
	    	Imgproc.adaptiveThreshold(inputMat, inputMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,11,5);
	    	Imgcodecs.imwrite( getTempDirectoryPath()+"/0-imageOcr_adaptivetresh.jpg", inputMat);
	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,5)));
	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,1)));
	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5)));		    		
	    	Imgcodecs.imwrite( getTempDirectoryPath()+"/1-imageOcr_adaptivetresh_morph.jpg", inputMat);
    
//	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));		    		
//	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3)));		    		
//	        progressDialog.setProgress(40);
	    	
	    	String imageFile_ocr = getTempDirectoryPath()+"/imageOcr.jpg";
	    	Imgcodecs.imwrite(imageFile_ocr, inputMat);	    	
    	
	    	Rect prodottoTitleRect = null;
	    	Rect lottoTitleRect = null;
	    	Rect scadenzaTitleRect = null;
	    	
			Mat sobel = new Mat();
			Mat thresh = new Mat();
			Mat thresh_dilate = new Mat();

			Imgproc.Sobel(inputMat, sobel, CvType.CV_8U, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);			
			Imgproc.Sobel(sobel, sobel, CvType.CV_8U, 0, 1, 3, 1, 0, Core.BORDER_DEFAULT);
			Imgcodecs.imwrite(getTempDirectoryPath()+"/11-image_sobel.jpg", sobel);	    
			Imgproc.erode(sobel, sobel, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3)));
			Imgcodecs.imwrite(getTempDirectoryPath()+"/22-image_sobel_erode_3x3.jpg", sobel);	    			
			Imgproc.dilate(sobel, sobel, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)),new Point(-1,-1), 8);
			Imgproc.dilate(sobel, sobel, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,1)),new Point(-1,-1), 8);
			Imgcodecs.imwrite(getTempDirectoryPath()+"/33-image_sobelxy_dilated.jpg", sobel);	    
			
	    	Imgproc.medianBlur(sobel, sobel, 3); 
	    	Imgproc.GaussianBlur(sobel, sobel, new Size(5, 5), 5); //denoise
			double mean = Core.mean(sobel).val[0]*2; // *3
			Log.d(TAG,"mean: "+mean);
//			Imgproc.threshold(sobel, thresh, Core.mean(sobel).val[0]/2, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
			Imgproc.threshold(sobel, thresh, (mean>255 ? 255 : mean), 255, Imgproc.THRESH_BINARY); // +Imgproc.THRESH_OTSU
			Imgcodecs.imwrite(getTempDirectoryPath()+"/44-image_sobel_thresh.jpg", thresh);
			
			Imgproc.morphologyEx(thresh, thresh_dilate, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(61,1) ));
			Imgcodecs.imwrite(getTempDirectoryPath()+"/55-image_morphologyEx.jpg", thresh_dilate);
			
	    	// trovo tutte le aree contenenti possibili elementi testuali
	    	List<Rect> textBoxes = detectTextBoxes(thresh_dilate,OriginalBW_copy,1);
	    	
	    	List<Rect> textBoxesOthers = detectTextBoxes(thresh,OriginalBW_copy2,2); // lo uso per trovare lotto e scadenza	    	
	    	
	    	List<OcrRect> ocrTextBoxes = new ArrayList<OcrRect>();
	    	Iterator<Rect> lb = textBoxesOthers.iterator();
	    	int boxes_count = textBoxesOthers.size();
	    	double step = (double)35/(double)(boxes_count*2);
	    	double current_progress = 30;
	    	//int _count =0;
	    	// qui cerco la descrizione, lotto e scadenza
	    	while(lb.hasNext())
	    	{
	    		Rect roi = lb.next();
	    		Mat cropped = new Mat(inputMat,roi);        	    	
	    		Mat Ocropped = new Mat(OriginalBW,roi);        	    	
	    		//Imgproc.rectangle(OriginalBW, roi.tl(), roi.br(), new Scalar(0,0,0));
                
                Bitmap bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped, bitmap);
                //Imgcodecs.imwrite(getTempDirectoryPath()+"/crop_"+_count+".jpg", cropped);
                Bitmap Obitmap = Bitmap.createBitmap(Ocropped.cols(), Ocropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(Ocropped, Obitmap);
                //Imgcodecs.imwrite(getTempDirectoryPath()+"/Ocrop_"+_count+".jpg", Ocropped);
                //_count++;
                
				baseApi.setImage(bitmap);
				String recognizedText = baseApi.getUTF8Text();
				int confidence = baseApi.meanConfidence();

				baseApi.setImage(Obitmap);
				String OrecognizedText = baseApi.getUTF8Text();
				int Oconfidence = baseApi.meanConfidence();
				
				Log.d(TAG,"roi: "+roi.x+","+roi.y+","+roi.width+","+roi.height);
				Log.d(TAG,"Only letters ROI confidence: "+confidence+" - OCR:"+recognizedText);
				Log.d(TAG,"Only letters ROI Oconfidence: "+Oconfidence+" - OOCR:"+OrecognizedText);

				String stringToFind = Cimino.toSlug(recognizedText.toLowerCase(Locale.ITALIAN));
				String OstringToFind = Cimino.toSlug(OrecognizedText.toLowerCase(Locale.ITALIAN));
				
				if (
						stringToFind.indexOf("descrizione")>-1 || 
						StringSimilarity.similarity(stringToFind, "descrizione")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "cod descrizione")>=0.6 ||
						StringSimilarity.similarity(stringToFind, "descrizione articolo")>=0.6
					)
				{
					prodottoTitleRect = roi;
					Log.d(TAG,"Colonna Descrizione trovata!");
				}
				else if (
						OstringToFind.indexOf("descrizione")>-1 || 
						StringSimilarity.similarity(OstringToFind, "descrizione")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "cod I descrizione")>=0.6 ||
						StringSimilarity.similarity(OstringToFind, "descrizione articolo")>=0.6
					)
				{
					prodottoTitleRect = roi;
					Log.d(TAG,"Colonna Descrizione trovata!");
				}			
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
					Log.d(TAG,"scadenza StringSimilarity.similarity: "+StringSimilarity.similarity(stringToFind, "scadenza"));
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
//						lottoScadenzaTitleRect = roi;
						Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						lottoTitleRect = roi;
						Log.d(TAG,"Campo Lotto trovato!");						
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
					Log.d(TAG,"scadenza StringSimilarity.similarity: "+StringSimilarity.similarity(OstringToFind, "scadenza"));
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
//						lottoScadenzaTitleRect = roi;
						Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						lottoTitleRect = roi;
						Log.d(TAG,"Campo Lotto trovato!");						
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
					Log.d(TAG,"lotto StringSimilarity.similarity: "+StringSimilarity.similarity(stringToFind, "lotto"));
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
//						lottoScadenzaTitleRect = roi;
						Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						scadenzaTitleRect = roi;
						Log.d(TAG,"Campo Scadenza trovato!");						
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
					Log.d(TAG,"lotto StringSimilarity.similarity: "+StringSimilarity.similarity(OstringToFind, "lotto"));
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
//						lottoScadenzaTitleRect = roi;
						Log.d(TAG,"Campo Lotto e Scadenza trovato!");						
					}
					else
					{
						scadenzaTitleRect = roi;
						Log.d(TAG,"Campo Scadenza trovato!");						
					}
				}

				cropped = null;
				bitmap = null;
    	    	current_progress+=step;
    	    	progressDialog.setProgress((int)current_progress);
	    		
	    	}
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, charWhiteList);
			Iterator<Rect> lb2 = textBoxes.iterator();
	    	while(lb2.hasNext())
	    	{
	    		Rect roi = lb2.next();
	    		
	    		if (!roi.equals(prodottoTitleRect))
	    		{
		    		Mat cropped = new Mat(inputMat,roi);        	    	
	                
	                Bitmap bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
	                Utils.matToBitmap(cropped, bitmap);
	                
					baseApi.setImage(bitmap);
					String recognizedText = baseApi.getUTF8Text();
					int confidence = baseApi.meanConfidence();
					
					Log.d(TAG,"Full ROI confidence: "+confidence+" - OCR:"+recognizedText);

					if (recognizedText.length()>=6 && recognizedText.length()<=60 && confidence>30)
					{
						ocrTextBoxes.add(new OcrRect(roi, recognizedText, confidence));
					}
					cropped = null;
					bitmap = null;
	    	    	current_progress+=step;
	    	    	progressDialog.setProgress((int)current_progress);
	    		}
	    	}

	    	Imgproc.GaussianBlur(inputMat, inputMat, new Size(5, 5), 5); //denoise
	    	Imgproc.medianBlur(inputMat, inputMat, 5); 
			Imgcodecs.imwrite( getTempDirectoryPath()+"/1b-imageOcr_tresh_blur.jpg", inputMat);
//			double mean = Core.mean(inputMat).val[0];
//			Log.d(TAG,"mean: "+mean);
			Imgproc.threshold(inputMat, inputMat, 247, 255, Imgproc.THRESH_BINARY); // +Imgproc.THRESH_OTSU
			Imgcodecs.imwrite( getTempDirectoryPath()+"/2-imageOcr_tresh_new.jpg", inputMat);
			Imgproc.dilate(inputMat, inputMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)), new Point(-1,-1),2);
/*	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,5)));
	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,1)));
	    	Imgproc.morphologyEx(inputMat, inputMat, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5,5)));*/
			Imgcodecs.imwrite( getTempDirectoryPath()+"/2b-imageOcr_tresh_new_dilate.jpg", inputMat);
	    	
	    	// fuori da questo ciclo devo trovare sulla stessa linea di desxcrizione lotto e scadenza
	    	// basta controllare se sono "sotto" cioe' y>ydescrizione e 
	    	// se non ci sono allora lotto e scadenza sono nell'area descrizione prodotto
	    	JSONObject finalData = new JSONObject();
	    	if (prodottoTitleRect!=null)
	    	{

		    	boolean stessaLinea_lotto = false;
		    	boolean stessaLinea_scadenza = false;
		    	if (lottoTitleRect!=null)
		    	{
			    	Rect searchAreaLotto = new Rect(lottoTitleRect.x,prodottoTitleRect.y,lottoTitleRect.width,prodottoTitleRect.height);
			    	if (intersect_Y(searchAreaLotto, lottoTitleRect))
			    	{
			    		stessaLinea_lotto = true;
			    		lottoTitleRect.x = (int) (lottoTitleRect.x-(lottoTitleRect.width*0.7));
			    		lottoTitleRect.width = (int) (lottoTitleRect.width+(2*lottoTitleRect.width*0.7));
			    		Log.d(TAG,"Il campo lotto sta sulla stessa linea di descrizione!");
			    	}
		    	}
		    	if (scadenzaTitleRect!=null)
		    	{
			    	Rect searchAreaScadenza = new Rect(scadenzaTitleRect.x,prodottoTitleRect.y,scadenzaTitleRect.width,prodottoTitleRect.height);
			    	if (intersect_Y(searchAreaScadenza, scadenzaTitleRect))
			    	{
			    		stessaLinea_scadenza = true;
//			    		scadenzaTitleRect.x = scadenzaTitleRect.x-10;
//			    		scadenzaTitleRect.width = lottoTitleRect.width+20;
			    		Log.d(TAG,"Il campo scadenza sta sulla stessa linea di descrizione!");
			    	}
		    	}
		    	
		    	JSONArray jrows = new JSONArray();	
	    		
	    		// devo ordinarly per y crescente;
	    		Collections.sort(textBoxes, new Comparator<Rect>(){
	    		     public int compare(Rect o1, Rect o2){
	    		         if(o1.y == o2.y)
	    		             return 0;
	    		         return o1.y < o2.y ? -1 : 1;
	    		     }
	    		});
	    		
		    	Iterator<Rect> broi = textBoxes.iterator();
		    	
		    	List<Rect> tableHeader = new ArrayList<Rect>();
		    	//tableHeader.add(productTitleRect);
		    	
		    	while(broi.hasNext())
		    	{
		    		Rect roi = broi.next();
		    		if (
		    				(roi.y>=prodottoTitleRect.y && roi.y<prodottoTitleRect.y+prodottoTitleRect.height/3) ||
		    				(roi.y<prodottoTitleRect.y && roi.y+roi.height>prodottoTitleRect.y)
		    			)
		    		{
		    			tableHeader.add(roi);
		    		}
		    			
		    	}
	    		Collections.sort(tableHeader, new Comparator<Rect>(){
	    		     public int compare(Rect o1, Rect o2){
	    		         if(o1.x == o2.x)
	    		             return 0;
	    		         return o1.x < o2.x ? -1 : 1;
	    		     }
	    		});
	    		
	    		
	    		int index = tableHeader.indexOf(prodottoTitleRect);
	    		Rect leftColumn = new Rect(0,prodottoTitleRect.y,1,prodottoTitleRect.y+prodottoTitleRect.height);
	    		//Rect rightColumn = new Rect(inputMat.cols()-1,productTitleRect.y,inputMat.cols(),productTitleRect.y+productTitleRect.height);
	    		if (index>0)
	    		{
	    			leftColumn = tableHeader.get(index-1);
	    		}
	    		
	    		/*if (index<tableHeader.size())
	    		{
	    			rightColumn = tableHeader.get(index+1);
	    		}*/
	    		// TODO: verificare con = 3 per inglobare pure i codici prodotto
	    		int newX = (int) ((leftColumn.x+leftColumn.width)*1.5); //+50%
	    		int widthIncrease = prodottoTitleRect.x-newX;  
	    		prodottoTitleRect.width += widthIncrease; //(int) ((rightColumn.x-productTitleRect.x)/1.2); //-20%
	    		prodottoTitleRect.x = newX;
	    		//Imgproc.rectangle(OriginalBW, prodottoTitleRect.tl(), prodottoTitleRect.br(), new Scalar(50,50,50),1);
	    			    			    		
		    	Iterator<OcrRect> broiBody = ocrTextBoxes.iterator();
		    	progressDialog.setProgress(65);
		    	step = (double)25/(double)textBoxes.size();
		    	current_progress=65;		    	
		    	int count =0;
		    	String lotto = "";
		    	String scadenza = "";
		    	while(broiBody.hasNext())
		    	{
		    		OcrRect Oroi = broiBody.next();
		    		Rect roi = Oroi.getArea();
		    		// Possibile eccezione:
		    		// 0 <= _colRange.start && _colRange.start <= _colRange.end && _colRange.end <= m.cols
					Log.d(TAG,"OLD - confidence: "+Oroi.getConfidence()+" - OCR:"+Oroi.getText());
					
		    		if(roi.width>=0 && roi.height>=0)
		    		{
			    		try{
		    	    		Mat cropped = new Mat(inputMat,roi);
		                    
		                    Bitmap bitmap = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
		                    Utils.matToBitmap(cropped, bitmap);
		                    
		                    Bitmap enlargedBitmap = Bitmap.createBitmap(cropped.cols()+50, cropped.rows()+50, Bitmap.Config.ARGB_8888);		                    
		                    Canvas wideBmpCanvas = new Canvas(enlargedBitmap);
		                    wideBmpCanvas.drawColor(Color.WHITE);
		                    wideBmpCanvas.drawBitmap(bitmap, 25, 25, null);
		                    Mat new_bitmap = new Mat();
		                    Utils.bitmapToMat(enlargedBitmap, new_bitmap);
				    		if (		    				
//				    				((roi.y+roi.height)>=(prodottoTitleRect.y+prodottoTitleRect.height)) &&  
				    				((roi.y)>=(prodottoTitleRect.y-prodottoTitleRect.height)) && // TODO: TESTARE: originariente: +prodottoTitleRect.height/3   
				    				(
				    						(roi.x+roi.width>=prodottoTitleRect.x && roi.x+roi.width<=prodottoTitleRect.x+prodottoTitleRect.width) ||
				    						(roi.x>=prodottoTitleRect.x && roi.x<=prodottoTitleRect.x+prodottoTitleRect.width) ||
				    						(roi.x<=prodottoTitleRect.x && roi.x+roi.width>=prodottoTitleRect.x+prodottoTitleRect.width)
				    				)
				    			)
				    		{
			    	    		Imgcodecs.imwrite(getTempDirectoryPath()+"/cropped_"+(count++)+".jpg", new_bitmap);
//			    	    		Imgcodecs.imwrite(getTempDirectoryPath()+"/cropped_"+(count++)+".jpg", cropped);
//			                    OcrRect OrecognizedText = GetText(bitmap,roi);
			                    OcrRect OrecognizedText = GetText(enlargedBitmap,roi);
								Log.d(TAG,"----------------------------------------------------");			                    
			                    String recognizedText = OrecognizedText.getText();
			                    if (OrecognizedText.getConfidence()<=Oroi.getConfidence())
			                    {
			                    	// se non ho miglioramenti mi tengo il valore vecchio
			                    	// ovvero se il delta di confidence è <=3
			                    	recognizedText = Oroi.getText();
			                    }
		    					if (recognizedText.indexOf("|")>-1)
		    					{
		    						String[] tmp = recognizedText.split("\\|");
		    						recognizedText = tmp[tmp.length-1];
			    					OrecognizedText.setText(recognizedText);
		    					}
			    				if (recognizedText.length()>=6 && recognizedText.length()<=60)
			    				{
//				    				Log.d(TAG,"Prodotto: "+recognizedText);
			    					
									// devo controllare che sia "in linea" con descrizione
						    		// se lo sono, allora sono nel caso in cui lotto e scadenza sono le intestazioni della tabella
						    		// se non lo sono allora si tratta di elementi della descrizione del prodotto

							    	// fuori da questo ciclo devo trovare sulla stessa linea di desxcrizione lotto e scadenza
							    	// basta controllare se sono "sotto" ciow' y>ydescrizione e 
							    	// se non ci sono allora lotto e scadenza sono nell'area descrizione prodotto
					    			try
									{
							    		if(stessaLinea_lotto || stessaLinea_scadenza)
							    		{
								    		JSONObject jobj = new JSONObject();
								    		jobj.put("index", count);
								    		Log.d(TAG,"Prodotto: "+recognizedText);
											jobj.put("prodotto", recognizedText);										
											// qui devo cercare sulla stessa riga a destra lotto e scadenza
											if (stessaLinea_lotto)
											{
												// TODO: devo fare un contronto con imageMat
												String recognizedText_lotto = findNearestRightSameLine(lottoTitleRect, roi, OriginalBW);
							    				Log.d(TAG,"Lotto: "+recognizedText_lotto);
												jobj.put("lotto", recognizedText_lotto);
											}
											else
											{
												jobj.put("lotto", "");
											}
											if (stessaLinea_scadenza)
											{
												// TODO: devo fare un contronto con imageMat
												String recognizedText_scadenza = findNearestRightSameLine(scadenzaTitleRect, roi, OriginalBW); 
												Log.d(TAG,"Scadenza: "+recognizedText_scadenza);
												jobj.put("scadenza", recognizedText_scadenza);
											}
											else
											{
												jobj.put("scadenza", "");
											}
											jrows.put(jobj);
											//Imgproc.rectangle(OriginalBW, roi.tl(), roi.br(), new Scalar(100,100,100),1);
											
							    		}
							    		else
							    		{
							    			// TODO:
							    			// devo cercare i valori attorno il testo del prodotto, tipicamente al di sotto per ogni riga
							    			// l'area di OCR se fatta bene dovrebbe gia' inglobare tutto, pertanto nel testo OCR dovrebbe
							    			// essere presente una stringa tipo: Lotto: 107 Scadenza: 30/09/2015
											String stringToFind = Cimino.toSlug(recognizedText.toLowerCase(Locale.ITALIAN));
							    			// devo fare attenzione perche' alternativamente lotto o scadenza potrebbero mancare
											Log.d(TAG,"lotto StringSimilarity.similarity: "+StringSimilarity.similarity(stringToFind, "lotto"));
											Log.d(TAG,"scadenza StringSimilarity.similarity: "+StringSimilarity.similarity(stringToFind, "scadenza"));
											if ( 
													(
														stringToFind.indexOf("lotto")>-1 
													&&
														stringToFind.indexOf("scadenza")>-1
													)
												)
											{

												Log.d(TAG,"Campo Lotto e Scadenza trovato!");	
												// estraggo lotto e scadenza per assegnarli al successivo prodotto
												// dato che la sequenza di lettura dal basso verso l'alto è
												// lotto-scad
												// prodotto
												// lotto-scad
												// prodotto
												// ...
												recognizedText = recognizedText.replace("otto", "otto:");
												recognizedText = recognizedText.replace("casenza", "cadenza:");
												recognizedText = recognizedText.replace("::",":").replace(": ", ":").trim();
												Log.d(TAG,"Prodotto: "+recognizedText);
												String[] parts = recognizedText.split(" ");
												Log.d(TAG,"parts: "+parts.length);
												String[] lotto_parts = parts[0].split(":");
												Log.d(TAG,"lotto parts: "+lotto_parts.length);
												String[] scad_parts = parts[1].split(":");
												Log.d(TAG,"scadenza parts: "+scad_parts.length);
												lotto = lotto_parts[1];
												scadenza = scad_parts[1];
												Log.d(TAG,"Lotto: "+lotto+" - Scadenza: "+scadenza);
												Log.d(TAG,"----------------------------------------------------");
											}
											else
											{
									    		JSONObject jobj = new JSONObject();
									    		jobj.put("index", count);												
												jobj.put("prodotto", recognizedText);												
												jobj.put("lotto", lotto);												
												jobj.put("scadenza", scadenza);												
												jrows.put(jobj);
												lotto = "";
												scadenza = "";
												//Imgproc.rectangle(OriginalBW, roi.tl(), roi.br(), new Scalar(100,100,100),1);
											}
							    		}
																			
										cropped = null;
										bitmap = null;
									}
									catch (JSONException e)
									{
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
			    				}

				    		}
			    	    	current_progress+=step;
			    	    	progressDialog.setProgress((int)current_progress);
			    		}
			    		catch(CvException cve)
			    		{
			    			Log.d(TAG,cve.getLocalizedMessage());
			    			cve.printStackTrace();
			    		}
						catch (Exception e1)
						{
							Log.d(TAG,e1.getLocalizedMessage());
							// TODO Auto-generated catch block
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	baseApi.end();
				baseApi=null;	    		
		        
				imageFile_ocr = getTempDirectoryPath()+"/9-image_detectLetters.jpg";
		    	Imgcodecs.imwrite(imageFile_ocr, OriginalBW);	    	
		    	
		    	//String headerFile_ocr="";
		    	/*cropped = new Mat(quad,roi);
		   		String headerFile_ocr = getTempDirectoryPath()+"/headerOcr.jpg";
		    	Imgcodecs.imwrite(headerFile_ocr, cropped);*/

		    	resultIntent.putExtra("imageFile", imageFile);
		    	resultIntent.putExtra("headerFile", "");
//		    	resultIntent.putExtra("imageFileOcr", imageFile_ocr);
//		    	resultIntent.putExtra("headerFileOcr", headerFile_ocr);
		    	resultIntent.putExtra("productRows", finalData.toString());
		    	resultIntent.putExtra("realSizePixelRatio", realSizePixelRatio);		    	
		    	progressDialog.setProgress(100);
		    	
	    	}
	    	else
	    	{	    		
				imageFile_ocr = getTempDirectoryPath()+"/9-image_detectLetters.jpg";
		    	Imgcodecs.imwrite(imageFile_ocr, OriginalBW);	    	

	    		resultIntent.putExtra("imageFile", "");
	    		resultIntent.putExtra("headerFile", "");
		    	resultIntent.putExtra("productRows", "");
		    	resultIntent.putExtra("realSizePixelRatio", realSizePixelRatio);
	    		status=false;
	    	}
			sobel = null;
			thresh = null;
			thresh_dilate = null;

	    	setResult(RESULT_OK, resultIntent);
			Log.d(TAG,"###### doInBackground finish");
			return status;
		}
		
		private boolean intersect_Y(Rect A, Rect B)
		{
			return (B.tl().y>=A.tl().y && B.tl().y<=A.br().y) 
					|| (A.tl().y<=B.br().y && A.tl().y>=B.tl().y);
		}
		
		private String findNearestRightSameLine(Rect roiColumn, Rect roiRow, Mat image) throws Exception
		{			
			String result = "";
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
                Imgcodecs.imwrite(getTempDirectoryPath()+"/new_cropped"+roiRow.y+".jpg", cropped);
                OcrRect Oresult = GetText(bitmap,searchArea);
                result = Oresult.getText();
//	    				baseApi.setImage(bitmap);
//	    				result = baseApi.getUTF8Text();
                cropped = null;
			}
			return result;
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
				Log.d(TAG,"Area: "+contour.size().area());
				//double epsilon = Imgproc.arcLength(contour, true) * 0.02;
				Imgproc.approxPolyDP( contour, approx, 3, true);
				Rect appRect = Imgproc.boundingRect(new MatOfPoint(approx.toArray()) );
				if(contour.size().area()>10 && contour.size().area()<600)
				{
					if(appRect.height>10 && appRect.width>10)
					{
						boundRect.add(appRect);		
						Imgproc.rectangle(Original, appRect.tl(), appRect.br(), new Scalar(200,200,200),2);						
					}

				}
			}			
			Imgcodecs.imwrite( getTempDirectoryPath()+"/detectTextBoxes_"+idx+".jpg", Original);
			ci = null;
			//element = null;
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
        	Log.d(TAG,"xml: "+xmlresult);
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
		Log.d(TAG,"NEW - confidence: "+confidence+" - OCR:"+result);
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
} 

