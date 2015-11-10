package it.cimino;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.camera.FileHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

/**
 * This class echoes a string called from JavaScript.
 */
public class Cimino extends CordovaPlugin {

	private static final int DATA_URL = 0;              // Return base64 encoded string
    private static final int FILE_URI = 1;              // Return file uri (content://media/external/images/media/2 for Android)

    private static final int JPEG = 0;                  // Take a picture of type JPEG
    private static final int PNG = 1;                   // Take a picture of type PNG

    //private int targetWidth = 0;                // desired width of the image
    //private int targetHeight = 0;               // desired height of the image

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String toSlug(String input) {
      String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
      String normalized = Normalizer.normalize(nowhitespace, Form.NFD);
      String slug = NONLATIN.matcher(normalized).replaceAll("");
      slug.replace("0", "O").replace('1','I').replace("2", "Z").replace("3", "B").replace("4", "A").replace("5", "S");
      return slug.toLowerCase(Locale.ITALIAN);
    }    
    
	private Uri imageUri;                   // Uri of captured image
    private static final String TAG = "Cimino::Main";

    private int encodingType = JPEG;               // Type of encoding to use
    private int numPics;
    public CallbackContext callbackContext;
    
    @Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
	    super.initialize(cordova, webView);
	    // your init code here

	    if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            Context context = cordova.getActivity().getApplicationContext();
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, null);
                        
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
        }
	    
	}
	
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	Log.d(TAG, "executing  "+action);
    	Log.d(TAG, "args:"+args.toString());
    	this.callbackContext = callbackContext;
        if(action.equals("capture"))
        {
        	try
        	{
            	this.capture(args.getJSONObject(0));
    	    }
    	    catch (IllegalArgumentException e)
    	    {
    	        callbackContext.error("Illegal Argument Exception");
    	        e.printStackTrace();
    	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
    	        callbackContext.sendPluginResult(r);
    	    }        	
            return true;
        }
        if(action.equals("process"))
        {
        	try
        	{
            	this.process(args.getJSONObject(0));
    	    }
    	    catch (IllegalArgumentException e)
    	    {
    	        callbackContext.error("Illegal Argument Exception");
    	        e.printStackTrace();
    	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
    	        callbackContext.sendPluginResult(r);
    	    }        	
            return true;
        }
        if (action.equals("init"))
        {
//        	InputStream input = null;
//            OutputStream output = null;
            HttpURLConnection connection = null;
        	try
        	{
        		JSONObject arg_object = args.getJSONObject(0);
                String tessdata_url = (String) arg_object.get("tessdata_url");
                // qui posso caricare da internet il file ita.traineddata e memorizzarlo su
                // getFileDirectoryPath()+"/tessdata
                // se la cartella tessdata non e' repsente la devo creare
        	    // se il file tessdata.ita non esiste lo devo scaricare da un URL.
        	    String tessdatafilepath = getFileDirectoryPath()+"/tessdata";
        	    String tessdatafilename = "ita.traineddata";
        	    File file = new File(tessdatafilepath,tessdatafilename);
        	    if(!file.exists()) 
        	    {
        	    	File dir = new File(tessdatafilepath);
        	    	dir.mkdir();
        	    	try{
            	    	URL url = new URL(tessdata_url);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.connect();
                        
                        // expect HTTP 200 OK, so we don't mistakenly save error report
                        // instead of the file
                        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                	        callbackContext.error("Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                	        callbackContext.sendPluginResult(r);
                	        return false;
                            
                        }
                        else
                        {

                            // this will be useful to display download percentage
                            // might be -1: server did not report the length
                            int fileLength = connection.getContentLength();
                            if (fileLength>0)
                            {
	                            FileOutputStream fileOutput = new FileOutputStream(file);
	
	                    		//Stream used for reading the data from the internet
	                    		InputStream inputStream = connection.getInputStream();
	                    		
	                    		//create a buffer...
	                    		byte[] buffer = new byte[1024];
	                    		int bufferLength = 0;
	
	                    		while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
	                    			fileOutput.write(buffer, 0, bufferLength);
	                    			// update the progressbar //
	                    		}
	                    		//close the output stream when complete //
	                    		fileOutput.close();
                            }
                            else
                            {
                    	        callbackContext.error("File is empty!");
                    	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
                    	        callbackContext.sendPluginResult(r);
                    	        return false;
                            	
                            }
                        }
        	    		
        	    	} catch (final MalformedURLException e) {
        	    		e.printStackTrace();
            	        callbackContext.error("Error : MalformedURLException " + e);
            	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
            	        callbackContext.sendPluginResult(r);
            	        return false;        		    	
        	    	} catch (final IOException e) {
        	    		e.printStackTrace();
            	        callbackContext.error( "Error : IOException " + e);
            	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
            	        callbackContext.sendPluginResult(r);
            	        return false;        		    	
        	    	}
        	    	catch (final Exception e) {
        	    		e.printStackTrace();
            	        callbackContext.error( "Error : Please check your internet connection " + e);
            	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
            	        callbackContext.sendPluginResult(r);
            	        return false;        		    	
        	    	}    
        	    }       		        		

        	    callbackContext.success("Init DONE");
    	        PluginResult r = new PluginResult(PluginResult.Status.OK);
    	        callbackContext.sendPluginResult(r);        		
    	    }
    	    catch (IllegalArgumentException e)
    	    {
    	        callbackContext.error("Illegal Argument Exception");
    	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
    	        callbackContext.sendPluginResult(r);
    	    }        	
            return true;
        }
        return false;
    }

    
    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------
    private String getTempDirectoryPath() 
    {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) 
        {
            cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Android/data/" + cordova.getActivity().getPackageName() + "/cache/");
        }
        // Use internal storage
        else 
        {
            cache = cordova.getActivity().getCacheDir();
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
                    "/Android/data/" + cordova.getActivity().getPackageName() + "/files/");
        }
        // Use internal storage
        else 
        {
            cache = cordova.getActivity().getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }    
    
    /**
     * Create a file in the applications temporary directory based upon the supplied encoding.
     *
     * @param encodingType of the image to be taken
     * @return a File object pointing to the temporary picture
     */
    private File createCaptureFile(int encodingType) {
        File photo = null;
        if (encodingType == JPEG) {
            photo = new File(getTempDirectoryPath(), ".Pic.jpg");
        } else if (encodingType == PNG) {
            photo = new File(getTempDirectoryPath(), ".Pic.png");
        } else {
            throw new IllegalArgumentException("Invalid Encoding Type: " + encodingType);
        }
        return photo;
    }
       
    /**
     * 
     */
    private void capture(JSONObject data) 
    {
    	File photo = createCaptureFile(encodingType);
    	this.imageUri = Uri.fromFile(photo);
    	this.numPics = queryImgDB(whichContentStore()).getCount();    	
        Context context = cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context, CameraCaptureActivity.class);
//        Intent intent = new Intent(context, ImageOcrActivity.class);
        try
		{
			intent.putExtra("rows", data.getInt("rows") );
			intent.putExtra("extra_fields",data.getJSONArray("extra_fields").toString());
        	intent.putExtra("filepath", data.getString("filepath"));
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        cordova.startActivityForResult((CordovaPlugin) this, intent, 0 );
    }
    
    /**
     * 
     */
    private void process(JSONObject data) 
    {
    	this.numPics = queryImgDB(whichContentStore()).getCount();    	
        Context context = cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context, ImageOcrActivity.class);
        try
		{
        	intent.putExtra("filepath", data.getString("filepath"));
		}
		catch (JSONException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        cordova.startActivityForResult((CordovaPlugin) this, intent, 0 );
    }    
    /**
     * Called when the camera view exits.
     *
     * @param requestCode       The request code originally supplied to startActivityForResult(),
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	Log.d("Cimino::Main", "ActivityResult = requestCode:"+requestCode+" resultCode: "+resultCode);
    	int destType = (requestCode % 16) - 1;
    	try
		{
			this.processResultFromOCR(destType, intent);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
    }    

 
    /**
     * Creates a cursor that can be used to determine how many images we have.
     *
     * @return a cursor
     */
    private Cursor queryImgDB(Uri contentStore) {
        return this.cordova.getActivity().getContentResolver().query(
                contentStore,
                new String[] { MediaStore.Images.Media._ID },
                null,
                null,
                null);
    }
        
    /**
     * Determine if we are storing the images in internal or external storage
     * @return Uri
     */
    private Uri whichContentStore() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else {
            return android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI;
        }
    }
    
    /**
     * Used to find out if we are in a situation where the Camera Intent adds to images
     * to the content store. If we are using a FILE_URI and the number of images in the DB
     * increases by 2 we have a duplicate, when using a DATA_URL the number is 1.
     *
     * @param type FILE_URI or DATA_URL
     */
    private void checkForDuplicateImage(int type) {
        int diff = 1;
        Uri contentStore = whichContentStore();
        Cursor cursor = queryImgDB(contentStore);
        int currentNumOfImages = cursor.getCount();

        // delete the duplicate file if the difference is 2 for file URI or 1 for Data URL
        if ((currentNumOfImages - numPics) == diff) {
            cursor.moveToLast();
            int id = Integer.valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
            if (diff == 2) {
                id--;
            }
            Uri uri = Uri.parse(contentStore + "/" + id);
            this.cordova.getActivity().getContentResolver().delete(uri, null, null);
            cursor.close();
        }
    }
    
    /**
     * Applies all needed transformation to the image received from the camera.
     *
     * @param destType          In which form should we return the image
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    private void processResultFromOCR(int destType, Intent intent) throws IOException {

        if (intent!=null && intent.hasExtra("imageFile"))
        {
            checkForDuplicateImage(DATA_URL);

            // Cleans up after picture taking. Checking for duplicates and that kind of stuff.

            // Clean up initial camera-written image file.
            (new File(FileHelper.stripFileProtocol(this.imageUri.toString()))).delete();

            checkForDuplicateImage(FILE_URI);


        	JSONObject json = new JSONObject();
            try
			{
				json.put("imageFile", intent.getExtras().get("imageFile").toString());
	            json.put("headerFile", intent.getExtras().get("headerFile").toString());
	            json.put("realSizePixelRatio", intent.getExtras().get("realSizePixelRatio").toString());
				json.put("productRows", intent.getExtras().get("productRows").toString());
	            this.callbackContext.success(json);
	                   	
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();

	        	json = null; 
			}
            
        }
        else
        {
        	failPicture("No image captured");
        }
    }
    
	/**
	 * Send error message to JavaScript.
	 *
	 * @param err
	 */
	public void failPicture(String err) {
	    this.callbackContext.error(err);
	}

}
