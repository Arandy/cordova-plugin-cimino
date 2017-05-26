//
//  CaptureViewController.h
//  Cimino
//
//  Created by Alessandro Randazzo on 26/07/15.
//
//

#import <UIKit/UIKit.h>
#import <opencv2/highgui/cap_ios.h>
#import <opencv2/highgui/ios.h>
#import <TesseractOCR/TesseractOCR.h>
//#import "G8Tesseract+meanConfidence.h"
//#import "AsyncTask.h"
#import "OcrRect.h"
#import "Cimino.h"
#import "FirstScanData.h"

//#define TimeStamp [NSString stringWithFormat:@"%f",[[NSDate date] timeIntervalSince1970] * 1000]

@class Cimino;

@interface CaptureViewController : UIViewController
{
}

-(NSInteger)processImage:(cv::Mat&)image;
-(std::vector<cv::Rect>)detectTextBoxes:(cv::Mat&)inputMat original:(cv::Mat&)Original idx:(int)idx;
-(OcrRect*)findNearestRightSameLine:(cv::Rect)roiColumn roi:(cv::Rect)roiRow prefix:(NSString*)prefix tesseract:(G8Tesseract**)tesseract image:(cv::Mat&)image;
-(NSString *)slugify:(NSString *)string;
-(OcrRect*)GetText:(cv::Rect)roi tess:(G8Tesseract**)tesseract;
-(FirstScanData*)extractFirstScanData:(cv::Mat)inputMat
	bwimg:(cv::Mat)bwimg
	OriginalBW:(cv::Mat)OriginalBW
	OriginalBW_copy2:(cv::Mat)OriginalBW_copy2
	step:(NSString *)step
	tesseract:(G8Tesseract *)tesseract
	Otesseract:(G8Tesseract *)Otesseract
	Inv_Otesseract:(G8Tesseract *)Inv_Otesseract;

@property (strong, nonatomic) IBOutlet UIImageView *imageView;
@property (strong, nonatomic) Cimino* plugin;
@property (nonatomic, strong) UIProgressView *progressView;
@property NSMutableDictionary* finalData;
@property bool DEBUG;
@property bool DISABLE_LIMITS;
@property bool EMULATE_LOWRES_DEVICE;
@property NSInteger minHeight;
@property NSInteger minWidth;
@property NSInteger minArea;
@property NSInteger maxHeight;
@property NSInteger maxWidth;
@property NSInteger maxArea;

@property NSInteger minHeight2;
@property NSInteger minWidth2;
@property NSInteger minArea2;
@property NSInteger maxHeight2;
@property NSInteger maxWidth2;
@property NSInteger maxArea2;

@property NSInteger currentWidth;
@property NSInteger currentHeight;


@end
