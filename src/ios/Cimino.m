//
//  Cimino.m
//  Cimino
//
//  Created by Alessandro Randazzo on 23/07/15.
//
//

#import "Cimino.h"
#import "CaptureViewController.h"

@implementation Cimino

- (void)init:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@""
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];
}

- (void)capture:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@""
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];
	
}

-(void)process:(CDVInvokedUrlCommand*)command
{
			// Save the CDVInvokedUrlCommand as a property.  We will need it later.
			self.latestCommand = command;
			self.callbackId = command.callbackId;
			self.imagePath = [[command.arguments objectAtIndex:0] valueForKey:@"filepath"];
			self.currentHeight = [[[command.arguments objectAtIndex:0] valueForKey:@"height"] integerValue];
			self.currentHeight = [[[command.arguments objectAtIndex:0] valueForKey:@"width"] integerValue];
	
			if (self.captureViewController == nil) {
				self.captureViewController = [[CaptureViewController alloc] initWithNibName:@"CaptureViewController" bundle:nil];
			}
	
			self.captureViewController.plugin = self;
			// Display the view.  This will "slide up" a modal view from the bottom of the screen.
			[self.viewController presentViewController:self.captureViewController animated:YES completion:nil];	
}

-(void)hasSession:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@""
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];
}

-(void)hasResult:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@""
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];
}

-(void)recoverSession:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@""
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];

}

-(void)recoverResult:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@""
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];

}

-(void)clearResult:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@"OK"
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];

}

-(void)getSessionDocumentID:(CDVInvokedUrlCommand*)command
{
	//DUMMY FOR IOS
	CDVPluginResult* pluginResult = [
									 CDVPluginResult
									 resultWithStatus: CDVCommandStatus_OK
									 messageAsString:@"0"
									 ];
	
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];

}


-(NSString*) getImagePath{
    return self.imagePath;
}

-(NSInteger) getCurrentWidth{
    return self.currentWidth;
}

-(NSInteger) getCurrentHeight{
    return self.currentHeight;
}


// Method called by the overlay when the image is ready to be sent back to the web view
//-(void) returnResult:(NSString*)imageFile headerFile:(NSString*)headerFile productRows:(NSMutableDictionary*)finalData //realSizePixelRatio:(NSString*)realSizePixelRatio
-(void) returnResult:(NSDictionary*)finalData //realSizePixelRatio:(NSString*)realSizePixelRatio
{
	
    NSDictionary *jsonObj = [ NSDictionary dictionaryWithObjectsAndKeys:finalData,@"productRows",[NSNumber numberWithBool:true],@"success",nil ];
/*    NSDictionary *jsonObj = [ NSDictionary dictionaryWithObjectsAndKeys:
                             imageFile, @"imageFile",
                             headerFile, @"headerFile",
							 finalData, @"productRows",
							 realSizePixelRatio, @"realSizePixelRatio",
                             true, @"success",
                             nil
                             ];
*/
    CDVPluginResult* pluginResult = [
                                     CDVPluginResult
                                     resultWithStatus: CDVCommandStatus_OK
                                     messageAsDictionary:jsonObj
                                     ];

    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];
    [self.viewController dismissViewControllerAnimated:YES completion:nil];
}

-(void) close {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
     
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.latestCommand.callbackId];
    [self.viewController dismissViewControllerAnimated:YES completion:Nil];

}


@end
