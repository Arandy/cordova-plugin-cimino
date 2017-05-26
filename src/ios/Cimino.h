//
//  Cimino.h
//  Cimino
//
//  Created by Alessandro Randazzo on 23/07/15.
//
//

#import <Cordova/CDVPlugin.h>

#import "CaptureViewController.h"

@interface Cimino : CDVPlugin 

@property (nonatomic, copy) NSString *callbackId;
@property (nonatomic, copy) NSString *imagePath;
@property NSInteger currentWidth;
@property NSInteger currentHeight;
@property (nonatomic, retain) CaptureViewController* captureViewController;
@property (strong, nonatomic) CDVInvokedUrlCommand* latestCommand;

-(void)capture:(CDVInvokedUrlCommand*) command;
-(NSString*) getImagePath;
-(NSInteger) getCurrentWidth;
-(NSInteger) getCurrentHeight;
-(void) returnResult:(NSDictionary*)finalData;
//-(void) returnResult:(NSString*)imageFile headerFile:(NSString*)headeFile productRows:(NSMutableDictionary*)finalData realSizePixelRatio:(NSString*)realSizePixelRatio;
-(void) close;
@end
