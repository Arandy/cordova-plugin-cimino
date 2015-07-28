package it.cimino;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.camera.FileHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;


/**
 * This class echoes a string called from JavaScript.
 */
public class Cimino extends CordovaPlugin {

    private static final int DATA_URL = 0;              // Return base64 encoded string
    private static final int FILE_URI = 1;              // Return file uri (content://media/external/images/media/2 for Android)

    private static final int JPEG = 0;                  // Take a picture of type JPEG
    private static final int PNG = 1;                   // Take a picture of type PNG

    private int targetWidth = 0;                // desired width of the image
    private int targetHeight = 0;               // desired height of the image
    
    private Uri imageUri;                   // Uri of captured image
    private static final String TAG = "Cimino::Main";

    private int encodingType = JPEG;               // Type of encoding to use
    private int numPics;
    public CallbackContext callbackContext;
    
    @Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
	    super.initialize(cordova, webView);
	    // your init code here
	}
	
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	Log.d(TAG, "executing  "+action);
    	this.callbackContext = callbackContext;
        if (action.equals("calibrate")) 
        {
        	try
        	{
            	this.calibrate();
    	    }
    	    catch (IllegalArgumentException e)
    	    {
    	        callbackContext.error("Illegal Argument Exception");
    	        PluginResult r = new PluginResult(PluginResult.Status.ERROR);
    	        callbackContext.sendPluginResult(r);
    	    }
    	     
    	    PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
    	    r.setKeepCallback(true);
    	    callbackContext.sendPluginResult(r);
	        return true;
        }
        if(action.equals("capture"))
        {
            //this.encodingType = args.getInt(0);
        	try
        	{
            	this.capture();
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
    
    /**
     * 
     */
    private void calibrate() 
    {    	
        Context context = cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context, CameraCalibrationActivity.class);
        cordova.startActivityForResult((CordovaPlugin) this, intent, 0 );
    }
    
    /**
     * 
     */
    private void capture() 
    {
    	File photo = createCaptureFile(encodingType);
    	this.imageUri = Uri.fromFile(photo);
    	this.numPics = queryImgDB(whichContentStore()).getCount();
        Context context = cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context, CameraCaptureActivity.class);
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
			this.processResultFromCamera(destType, intent);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
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
    private void processResultFromCamera(int destType, Intent intent) throws IOException {

        if (intent!=null && intent.hasExtra("imageFile"))
        {
/*            Bitmap s_image = null;
            try {
            	s_image = getScaledBitmap(intent.getExtras().get("imageFile").toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
*/
        	//Bitmap s_image = BitmapFactory.decodeFile(intent.getExtras().get("imageFile").toString());
/*        	ByteArrayOutputStream stream = new ByteArrayOutputStream();
        	s_image.compress(Bitmap.CompressFormat.JPEG, 90, stream);        	
            byte[] bitmap = stream.toByteArray();
            stream.close();
            stream=null;
            s_image.recycle();
            s_image=null;
            
            Bitmap s_header = null;
            try {
            	s_header = getScaledBitmap(intent.getExtras().get("imageFile").toString());
            } catch (IOException e) {
                e.printStackTrace();
            }            
        	//Bitmap s_header = BitmapFactory.decodeFile(intent.getExtras().get("headerFile").toString());
        	ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
        	s_header.compress(Bitmap.CompressFormat.JPEG, 90, stream2);        	
            byte[] header = stream2.toByteArray();
            stream2.close();
            stream2=null;
            s_header.recycle();
            s_header=null;
            
            // Double-check the bitmap.
            if (bitmap == null) {
                Log.d(TAG, "I either have a null image path or bitmap");
                this.failPicture("Unable to create bitmap!");
                return;
            }
            this.processData(bitmap,header);
*/
        	JSONObject json = new JSONObject();
            try
			{
				json.put("imageFile", intent.getExtras().get("imageFile").toString());
	            json.put("headerFile", intent.getExtras().get("headerFile").toString());
	            this.callbackContext.success(json.toString());
	                   	
			}
			catch (JSONException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            

            checkForDuplicateImage(DATA_URL);

            // Cleans up after picture taking. Checking for duplicates and that kind of stuff.

                // Clean up initial camera-written image file.
            (new File(FileHelper.stripFileProtocol(this.imageUri.toString()))).delete();

            checkForDuplicateImage(FILE_URI);

//            System.gc();
//            bitmap = null;
//            header = null;
        	json = null; 
        }
        else
        {
        	 //this.callbackContext.error("No image captured");
        }
    }

    /**
     * Return a scaled bitmap based on the target width and height
     *
     * @param imagePath
     * @return
     * @throws IOException 
     *//*
    private Bitmap getScaledBitmap(String imageUrl) throws IOException {
        // If no new width or height were specified return the original bitmap
        if (this.targetWidth <= 0 && this.targetHeight <= 0) {
            InputStream fileStream = null;
            Bitmap image = null;
            try {
                fileStream = FileHelper.getInputStreamFromUriString(imageUrl, cordova);
                image = BitmapFactory.decodeStream(fileStream);
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        Log.d(TAG,"Exception while closing file input stream.");
                    }
                }
            }
            return image;
        }

        // figure out the original width and height of the image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream fileStream = null;
        try {
            fileStream = FileHelper.getInputStreamFromUriString(imageUrl, cordova);
            BitmapFactory.decodeStream(fileStream, null, options);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    Log.d(TAG,"Exception while closing file input stream.");
                }
            }
        }
        
        //CB-2292: WTF? Why is the width null?
        if(options.outWidth == 0 || options.outHeight == 0)
        {
            return null;
        }
        
        // determine the correct aspect ratio
        int[] widthHeight = calculateAspectRatio(options.outWidth, options.outHeight);

        // Load in the smallest bitmap possible that is closest to the size we want
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, this.targetWidth, this.targetHeight);
        Bitmap unscaledBitmap = null;
        try {
            fileStream = FileHelper.getInputStreamFromUriString(imageUrl, cordova);
            unscaledBitmap = BitmapFactory.decodeStream(fileStream, null, options);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    Log.d(TAG,"Exception while closing file input stream.");
                }
            }
        }
        if (unscaledBitmap == null) {
            return null;
        }

        return Bitmap.createScaledBitmap(unscaledBitmap, widthHeight[0], widthHeight[1], true);
    }*/    
    
    /**
     * Maintain the aspect ratio so the resulting image does not look smooshed
     *
     * @param origWidth
     * @param origHeight
     * @return
     *//*
    public int[] calculateAspectRatio(int origWidth, int origHeight) {
        int newWidth = this.targetWidth;
        int newHeight = this.targetHeight;

        // If no new width or height were specified return the original bitmap
        if (newWidth <= 0 && newHeight <= 0) {
            newWidth = origWidth;
            newHeight = origHeight;
        }
        // Only the width was specified
        else if (newWidth > 0 && newHeight <= 0) {
            newHeight = (newWidth * origHeight) / origWidth;
        }
        // only the height was specified
        else if (newWidth <= 0 && newHeight > 0) {
            newWidth = (newHeight * origWidth) / origHeight;
        }
        // If the user specified both a positive width and height
        // (potentially different aspect ratio) then the width or height is
        // scaled so that the image fits while maintaining aspect ratio.
        // Alternatively, the specified width and height could have been
        // kept and Bitmap.SCALE_TO_FIT specified when scaling, but this
        // would result in whitespace in the new image.
        else {
            double newRatio = newWidth / (double) newHeight;
            double origRatio = origWidth / (double) origHeight;

            if (origRatio > newRatio) {
                newHeight = (newWidth * origHeight) / origWidth;
            } else if (origRatio < newRatio) {
                newWidth = (newHeight * origWidth) / origHeight;
            }
        }

        int[] retval = new int[2];
        retval[0] = newWidth;
        retval[1] = newHeight;
        return retval;
    }*/
    
    /**
     * Figure out what ratio we can load our image into memory at while still being bigger than
     * our desired width and height
     *
     * @param srcWidth
     * @param srcHeight
     * @param dstWidth
     * @param dstHeight
     * @return
     *//*
    public static int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        final float srcAspect = (float)srcWidth / (float)srcHeight;
        final float dstAspect = (float)dstWidth / (float)dstHeight;

        if (srcAspect > dstAspect) {
            return srcWidth / dstWidth;
        } else {
            return srcHeight / dstHeight;
        }
      }
    */
/**
 * Compress bitmap using jpeg, convert to Base64 encoded string, and return to JavaScript.
 *
 * @param bitmap
 */
/*
public void processData(byte[] bitmap, byte[] header) {
    try {
    	
            byte[] output_image_encoded = Base64.encode(bitmap, Base64.NO_WRAP);
            byte[] output_header_encoded = Base64.encode(header, Base64.NO_WRAP);
            JSONObject json = new JSONObject();
            json.put("image", new String(output_image_encoded));
            json.put("header", new String(output_header_encoded));
            
            this.callbackContext.success(json.toString());
            json = null;
            output_image_encoded = null;
            output_header_encoded = null;
    } catch (Exception e) {
    	e.printStackTrace();
        this.failPicture("Error compressing image.");
    }
}
*/
    
/**
 * Send error message to JavaScript.
 *
 * @param err
 */
public void failPicture(String err) {
    this.callbackContext.error(err);
}

}
