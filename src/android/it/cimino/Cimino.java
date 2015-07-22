package it.cimino;

import java.io.File;
import java.io.IOException;
import java.sql.Time;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

/**
 * This class echoes a string called from JavaScript.
 */
public class Cimino extends CordovaPlugin {

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
	    super.initialize(cordova, webView);
	    // your init code here
	}
	
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	Log.d("Cimino::Main", "executing  "+action);
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
    }    
    
}
