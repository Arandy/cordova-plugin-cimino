//
//  CaptureViewController.m
//  Cimino
//
//  Created by Alessandro Randazzo on 26/07/15.
//
//
#import "UIImage+OpenCV.h"
#import "NSString+Similarity.h"
#import "Cimino.h"
#import "CaptureViewController.h"
#import "G8Constants.h"
#import "RoiObject.h"
#import "OcrRect.h"
#include <string>

/*
 *  System Versioning Preprocessor Macros
 */ 

#define SYSTEM_VERSION_EQUAL_TO(v)                  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedSame)
#define SYSTEM_VERSION_GREATER_THAN(v)              ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedDescending)
#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)
#define SYSTEM_VERSION_LESS_THAN(v)                 ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] == NSOrderedAscending)
#define SYSTEM_VERSION_LESS_THAN_OR_EQUAL_TO(v)     ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedDescending)

using namespace cv;

@interface CaptureViewController () <G8TesseractDelegate>

@end

@implementation CaptureViewController

// Entry point method
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        CGRect screenFrame = [[UIScreen mainScreen] bounds];
        self.view.frame = screenFrame;
        

    }
	self.DEBUG = true;
	self.minHeight = 15;
	self.minWidth = 100;
	self.minArea = 2000;
	self.maxHeight = 150;
	self.maxWidth = 2000;
	self.maxArea = 200000;

	self.minHeight2 = 15;
	self.minWidth2 = 100;
	self.minArea2 = 6000;
	self.maxHeight2 = 250;
	self.maxWidth2 = 5000;
	self.maxArea2 = 1200000;
    return self;
}

// (RotationIn_IOS6)
-(BOOL)shouldAutorotate
{
	return false;
}

- (void)viewDidLoad {
    [super viewDidLoad];
	
    self.progressView = [[UIProgressView alloc] initWithProgressViewStyle:UIProgressViewStyleDefault];
    self.progressView.progressTintColor = [UIColor colorWithRed:187.0/255 green:160.0/255 blue:209.0/255 alpha:1.0];
    [[self.progressView layer]setCornerRadius:10.0f];
    [[self.progressView layer]setBorderWidth:2.0f];
    [[self.progressView layer]setMasksToBounds:TRUE];
    self.progressView.clipsToBounds = YES;
    [[self.progressView layer]setFrame:CGRectMake(30, 400, 260, 25)];
    self.progressView.layer.borderColor = [[UIColor whiteColor]CGColor];
    self.progressView.trackTintColor = [UIColor clearColor];
//    self.progressView.hidden = YES;
	
    self.progressView.center = self.view.center;
    [self.view addSubview:self.progressView];
	

}

- (void)viewDidAppear:(BOOL)animated
{
    __block NSInteger result;
	NSArray *params;
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        result = [self doInBackground:params];
        dispatch_async(dispatch_get_main_queue(), ^{
            [self postExecute:result];
			// devo cancellare l'immagine generata qui col timestamp
        });
    });

}

- (NSInteger) doInBackground: (NSArray *) parameters{

	NSString *imagePath = [self.plugin getImagePath];
	
	//currentHeight = []

	NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
	NSString *basePath = ([paths count] > 0) ? [paths objectAtIndex:0] : nil;

	imagePath = [imagePath stringByReplacingOccurrencesOfString:@"file://" withString:@""];

	if (self.DEBUG ) NSLog(@"Base Path: %@",basePath);
	if (self.DEBUG ) NSLog(@"Image Path: %@",imagePath);
	
	/*
	Base Path: /var/mobile/Applications/0B1B5C8D-E7A9-4754-AD7D-125EAA14A144/Documents
	Image Path: file:///var/mobile/Applications/0B1B5C8D-E7A9-4754-AD7D-125EAA14A144/tmp/cdv_photo_001.jpg
*/
//	UIImage* resImage = [UIImage imageWithContentsOfFile:[NSString stringWithFormat:@"%@/%@",basePath,imagePath]];
	UIImage* resImage = [UIImage imageWithContentsOfFile:[NSString stringWithFormat:@"%@",imagePath]];
	cv::Mat image;
	UIImageToMat(resImage, image);

	NSFileManager *fileManager = [NSFileManager defaultManager];
	NSError *error;
	BOOL success = [fileManager removeItemAtPath:[NSString stringWithFormat:@"%@/%@",basePath,imagePath] error:&error];
	if (!success) {
		if (self.DEBUG ) NSLog(@"Could not delete file -:%@ ",[error localizedDescription]);
	}
	fileManager=nil;
	resImage=nil;
	
    return [self processImage:image];
}

- (void) postExecute: (NSInteger) result{
    //Method to override
    //Run on main thread (UIThread)
		
       // Get a file path to save the JPEG
//    filename = [NSString stringWithFormat:@"sample_picture_%@.jpg",currentDatetime];
//    imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
//    [self.plugin returnResult:@"" headerFile:@"" productRows:self.finalData realSizePixelRatio:@"0" ];
//    [self.plugin returnResult:[NSString stringWithFormat:@"%@", self.finalData]];
    [self.plugin returnResult:self.finalData];

}

#ifdef __cplusplus
- (NSInteger)processImage:(Mat&)inputMat;
{
	//NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
	
	//NSInteger day = [components day];
	//NSInteger month = [components month];
	//NSInteger year = [components year];
	//NSInteger hour = [components hour];
	//NSInteger minute = [components minute];
	//NSInteger second = [components second];
	
	// Get a file path to save the JPEG
	NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
	NSString* documentsDirectory = [paths objectAtIndex:0];
	//NSString *currentDatetime = [NSString stringWithFormat:@"%ld-%ld-%ld_%ld-%ld-%ld", (long)year, (long)month, (long)day, (long)hour, (long)minute, (long)second];
	
	NSString *charWhiteList = @"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890'.,:/^-àèìòùé+=-";
	NSString *charWhiteListLetters = @"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
    NSLog(@"Start processing...");
	
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.progressView setProgress:(CGFloat)1/100 animated:YES];
        });
	
    if(inputMat.cols>inputMat.rows)
	{
		cv::transpose(inputMat, inputMat);
		cv::flip(inputMat,inputMat,1);
	}
	
	// qui devo controllare la dimensione dell'immagine
	// se è superiore a 2448x3264 devo controllare che la fotocamera supporti questa risoluzione per fare uno scale down
	// se è inferiore non devo scalare l'immagine ma calcolare il rapporto e fare il resize
	/*
	*  Usage
	*/

//if (SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"3.1.1")) {
//}
	if (SYSTEM_VERSION_LESS_THAN(@"8.0"))
	{
		if (inputMat.cols>2448)
		{
			double ratio = 2448/inputMat.cols;
			cv::resize(inputMat, inputMat, cv::Size(2448,(int)inputMat.rows*ratio));
		}
	}
	
	// Languages are used for recognition (e.g. eng, ita, etc.). Tesseract engine
	// will search for the .traineddata language file in the tessdata directory.
	// For example, specifying "eng+ita" will search for "eng.traineddata" and
	// "ita.traineddata". Cube engine will search for "eng.cube.*" files.
	// See https://code.google.com/p/tesseract-ocr/downloads/list.
	
	// Create your G8Tesseract object using the initWithLanguage method:
	NSLog(@"Initializing TesseractOCR-iOS");
	G8Tesseract *tesseract = [[G8Tesseract alloc] initWithLanguage:@"ita"]; // -21MB!
	tesseract.pageSegmentationMode = G8PageSegmentationModeSingleLine;
	G8Tesseract *Otesseract = [[G8Tesseract alloc] initWithLanguage:@"ita"]; // -11MB
	Otesseract.pageSegmentationMode = G8PageSegmentationModeSingleLine;
	G8Tesseract *Inv_Otesseract = [[G8Tesseract alloc] initWithLanguage:@"ita"]; // -10MB
	Inv_Otesseract.pageSegmentationMode = G8PageSegmentationModeSingleLine;
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)2/100 animated:YES];
	});
	// Optionaly: You could specify engine to recognize with.
	// G8OCREngineModeTesseractOnly by default. It provides more features and faster
	// than Cube engine. See G8Constants.h for more information.
	//tesseract.engineMode = G8OCREngineModeTesseractOnly;
	
	// Set up the delegate to receive Tesseract's callbacks.
	// self should respond to TesseractDelegate and implement a
	// "- (BOOL)shouldCancelImageRecognitionForTesseract:(G8Tesseract *)tesseract"
	// method to receive a callback to decide whether or not to interrupt
	// Tesseract before it finishes a recognition.
	tesseract.delegate = self;
	
	// Optional: Limit the character set Tesseract should try to recognize from
	tesseract.charWhitelist = charWhiteListLetters;
	tesseract.charBlacklist = @"\\s";
	Otesseract.charWhitelist = charWhiteListLetters;
	Otesseract.charBlacklist = @"\\s";
	Inv_Otesseract.charWhitelist = charWhiteListLetters;
	Inv_Otesseract.charBlacklist = @"\\s";
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)3/100 animated:YES];
	});
	
	cvtColor(inputMat, inputMat, CV_BGRA2GRAY); //  +15MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)4/100 animated:YES];
	});

	UIImage* tmpuiimage;
	NSString *filename = @"0-imageOcr_BW.jpg";
	NSString *imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		/*if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(inputMat);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
		*/
	Mat OriginalBW = inputMat.clone(); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)5/100 animated:YES];
	});
	
	Mat OriginalBW_copy = inputMat.clone(); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)6/100 animated:YES];
	});
	
	Mat OriginalBW_copy2 = inputMat.clone(); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)7/100 animated:YES];
	});
	
	GaussianBlur(inputMat, inputMat, cv::Size(5, 5), 5);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)8/100 animated:YES];
	});
	
	medianBlur(inputMat, inputMat, 5);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)9/100 animated:YES];
	});
	
	adaptiveThreshold(inputMat, inputMat, 254, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 15, 5);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)10/100 animated:YES];
	});
	
//	threshold(inputMat, inputMat, 160, 255, THRESH_BINARY);
//	dispatch_async(dispatch_get_main_queue(), ^{
//		[self.progressView setProgress:(CGFloat)11/100 animated:YES];
//	});
	
		filename = @"0-imageOcr.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(inputMat);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	
	Mat Inv_OriginalBW = *new Mat();
	Mat Inv_OriginalBW2 = *new Mat();
	GaussianBlur(OriginalBW, OriginalBW, cv::Size(3, 3), 3);
	cv::bitwise_not(OriginalBW,Inv_OriginalBW2); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)12/100 animated:YES];
	});
	threshold(Inv_OriginalBW2, Inv_OriginalBW2, 160, 255, THRESH_BINARY);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)11/100 animated:YES];
	});

		filename = @"INV_bitwise_not.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(Inv_OriginalBW);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}

	NSInteger index = 0;
	int newX = 0;
	int widthIncrease = 0;
	bool stessaLinea_lotto = false;
	bool stessaLinea_scadenza = false;
	bool secondTry = false;

	cv::Rect prodottoTitleRect = *new cv::Rect();
	cv::Rect articoloTitleRect = *new cv::Rect();
	bool hasArticolo = false;
	cv::Rect lottoTitleRect = *new cv::Rect();
	bool hasLotto = false;
	cv::Rect scadenzaTitleRect = *new cv::Rect();
	bool hasScadenza = false;
	cv::Rect leftColumn;
    Mat thresh_dilate_cut = *new Mat();
	Mat inputMat_morph = *new Mat();
	Mat sobel = *new Mat();
	Mat thresh = *new Mat();
	bool force_exit = false;
	vector<cv::Rect> textBoxesOthers;
		cv::Rect roi;
	NSString* recognizedText = @"";
	
	FirstScanData* case2 = [self extractFirstScanData:inputMat
		bwimg:Inv_OriginalBW2
		OriginalBW:(cv::Mat)OriginalBW
		OriginalBW_copy2:OriginalBW_copy2
		step:@"2"
		tesseract:tesseract
		Otesseract:Otesseract
		Inv_Otesseract:Inv_Otesseract
		];

	textBoxesOthers = [case2 getTextBoxesOthers];
	prodottoTitleRect = [case2 getProdottoTitleRect];
	articoloTitleRect = [case2 getArticoloTitleRect];
	lottoTitleRect = [case2 getLottoTitleRect];
	scadenzaTitleRect = [case2 getScadenzaTitleRect];
	sobel = [case2 getSobel];
	thresh = [case2 getThresh];
	inputMat_morph = [case2 getInputMat_morph];
	
	if(prodottoTitleRect.area()==0){
		threshold(OriginalBW, Inv_OriginalBW, 190, 215, THRESH_BINARY);
		cv::bitwise_not(Inv_OriginalBW,Inv_OriginalBW); // -5MB
		FirstScanData* case1 = [self extractFirstScanData:inputMat
			bwimg:Inv_OriginalBW
			OriginalBW:(cv::Mat)OriginalBW
			OriginalBW_copy2:OriginalBW_copy2
			step:@"1"
			tesseract:tesseract
			Otesseract:Otesseract
			Inv_Otesseract:Inv_Otesseract
		];
		textBoxesOthers = [case1 getTextBoxesOthers];
		prodottoTitleRect = [case1 getProdottoTitleRect];
		articoloTitleRect = [case1 getArticoloTitleRect];
		lottoTitleRect = [case1 getLottoTitleRect];
		scadenzaTitleRect = [case1 getScadenzaTitleRect];
		sobel = [case1 getSobel];
		thresh = [case1 getThresh];
		inputMat_morph = [case1 getInputMat_morph];
	}
	
	/*
	dilate(Inv_OriginalBW,Inv_OriginalBW,getStructuringElement(MORPH_RECT, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)13/100 animated:YES];
	});
	
	erode(Inv_OriginalBW, Inv_OriginalBW, getStructuringElement(MORPH_RECT, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)14/100 animated:YES];
	});

//		filename = @"INV_erode_dilate.jpg";
//		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
//		if (self.DEBUG ) {
//			tmpuiimage = MatToUIImage(Inv_OriginalBW);
//			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
//			tmpuiimage=nil;
//		}

	GaussianBlur(Inv_OriginalBW, Inv_OriginalBW, cv::Size(5, 5), 5);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)15/100 animated:YES];
	});

//		filename = @"INV_GaussianBlur5x5.jpg";
//		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
//		if (self.DEBUG ) {
//			tmpuiimage = MatToUIImage(Inv_OriginalBW);
//			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
//			tmpuiimage=nil;
//		}

	threshold(Inv_OriginalBW, Inv_OriginalBW, 127, 255, THRESH_BINARY);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)16/100 animated:YES];
	});
	
		filename = @"INV_threshold127.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(Inv_OriginalBW);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)17/100 animated:YES];
	});

	Mat inputMat_morph = *new Mat();
	GaussianBlur(inputMat, inputMat_morph, cv::Size(5, 5), 5); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)18/100 animated:YES];
	});
	
	medianBlur(inputMat_morph, inputMat_morph, 5);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)19/100 animated:YES];
	});

//		filename = @"1-imageOcr_tresh_blur.jpg";
//		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
//		if (self.DEBUG ) {
//			tmpuiimage = MatToUIImage(inputMat_morph);
//			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
//			tmpuiimage=nil;
//		}
	
	threshold(inputMat_morph, inputMat_morph, 248, 255, THRESH_BINARY);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)20/100 animated:YES];
	});

//		filename = @"2-imageOcr_tresh_new.jpg";
//		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
//		if (self.DEBUG ) {
//			tmpuiimage = MatToUIImage(inputMat_morph);
//			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
//			tmpuiimage=nil;
//		}

	dilate(inputMat_morph, inputMat_morph, getStructuringElement(MORPH_RECT, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)21/100 animated:YES];
	});
	
		if (self.DEBUG ) {
			filename = @"3-imageOcr_tresh_new_dilate.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			tmpuiimage = MatToUIImage(inputMat_morph);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	
	cv::Rect prodottoTitleRect = *new cv::Rect();
	bool hasProdotto = false;
	cv::Rect articoloTitleRect = *new cv::Rect();
	bool hasArticolo = false;
	cv::Rect lottoTitleRect = *new cv::Rect();
	bool hasLotto = false;
	cv::Rect scadenzaTitleRect = *new cv::Rect();
	bool hasScadenza = false;
	
	Mat sobel = *new Mat();
	Mat thresh = *new Mat();
	
	Sobel(inputMat_morph,sobel,CV_8U, 1,0,3,1,0,BORDER_DEFAULT); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)22/100 animated:YES];
	});
	
	Sobel(sobel,sobel,CV_8U, 0,1,3,1,0,BORDER_DEFAULT);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)23/100 animated:YES];
	});
	
//		filename = @"11-sobel.jpg";
//		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
//		if (self.DEBUG ) {
//			tmpuiimage = MatToUIImage(sobel);
//			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
//			tmpuiimage=nil;
//		}

	erode(sobel, sobel, getStructuringElement(MORPH_ELLIPSE, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)24/100 animated:YES];
	});

//		filename = @"22-sobel_erode_3x3.jpg";
//		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
//		if (self.DEBUG ) {
//			tmpuiimage = MatToUIImage(sobel);
//			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
//			tmpuiimage=nil;
//		}

	if(sobel.cols>=1536)
	{
		dilate(sobel,thresh,getStructuringElement(MORPH_RECT, cv::Size(7,3)),cv::Point(-1,-1),9);
	}
	else
	{
		dilate(sobel,thresh,getStructuringElement(MORPH_RECT, cv::Size(5,3)),cv::Point(-1,-1),9);
	}
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)25/100 animated:YES];
	});
	
		if (self.DEBUG ) {
			filename = @"33-sobelxy_dilated.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			tmpuiimage = MatToUIImage(thresh);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	
	vector<cv::Rect> textBoxesOthers = [self detectTextBoxes:thresh original:OriginalBW_copy2 idx:1 minWidth:self.minWidth minHeight:self.minHeight maxWidth:self.maxWidth maxHeight:self.maxHeight minArea:self.minArea maxArea:self.maxArea];
	
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)30/100 animated:YES];
	});

	double _step = (double)15/(double)textBoxesOthers.size();
	double current_progress=30;
	
	NSLog(@"Inizio la ricerca delle parole descrizione lotto e scadenza.");
	
	bool force_exit = false;
	int seach_count = 0;
	// primo ciclo: cerco le etichette delle colonne della tabella (codice articolo, nome prodotto, lotto e scadenza)
	cv::Rect roi;
	NSString* recognizedText = @"";
	NSInteger confidence = 0;
	NSString* OrecognizedText;
	NSInteger Oconfidence = 0;
	NSString* Inv_OrecognizedText;
	NSInteger Inv_Oconfidence = 0;
	NSString* stringToFind;
	NSString* OstringToFind;
	NSString* Inv_OstringToFind;

//	NSArray *characterChoices;
	tesseract.image = MatToUIImage(inputMat); // +10MB
	Otesseract.image = MatToUIImage(OriginalBW); //+10MB
	Inv_Otesseract.image = MatToUIImage(Inv_OriginalBW); //+10MB
	for (int i=0; ((i < textBoxesOthers.size()) && !force_exit ); i++)
	{
		if (prodottoTitleRect.area()==0 || scadenzaTitleRect.area()==0 || lottoTitleRect.area()==0 || articoloTitleRect.area()==0)
		{
			roi = textBoxesOthers[i];
			if (prodottoTitleRect.area()>0)
			{
				if(roi.y<(prodottoTitleRect.y-(prodottoTitleRect.height*2)))
				{
					force_exit = true;
				}
			}
			if (self.DEBUG ) NSLog(@"roi (%d): %d,%d,%d,%d",seach_count,roi.x,roi.y,roi.width,roi.height);
			
			tesseract.rect = CGRectMake(roi.x, roi.y, roi.width, roi.height);
			[tesseract recognize];
			recognizedText = [tesseract recognizedText];
//			characterChoices = [tesseract recognizedBlocksByIteratorLevel:G8PageIteratorLevelTextline];
//			NSLog(@"characterChoices: %lu",(unsigned long)characterChoices.count);
			confidence = [tesseract meanConfidence];
					if (self.DEBUG ) NSLog(@"Only letters ROI confidence: %ld - OCR: %@",(long)confidence,recognizedText);
			
			//tesseract.image = MatToUIImage(Ocropped);
			Otesseract.rect = CGRectMake(roi.x, roi.y, roi.width, roi.height);
			[Otesseract recognize];
			OrecognizedText = [Otesseract recognizedText];
//			characterChoices = [Otesseract recognizedBlocksByIteratorLevel:G8PageIteratorLevelTextline];
//			NSLog(@"OcharacterChoices: %lu",(unsigned long)characterChoices.count);
			Oconfidence = [Otesseract meanConfidence];
					if (self.DEBUG ) NSLog(@"Only letters ROI Oconfidenc	e: %ld - OOCR: %@",(long)Oconfidence,OrecognizedText);
			
			//tesseract.image = MatToUIImage(Inv_Ocropped);
			Inv_Otesseract.rect = CGRectMake(roi.x, roi.y, roi.width, roi.height);
			[Inv_Otesseract recognize];
			Inv_OrecognizedText = [Inv_Otesseract recognizedText];
//			characterChoices = [Inv_Otesseract recognizedBlocksByIteratorLevel:G8PageIteratorLevelTextline];
//			NSLog(@"Inv_OcharacterChoices: %lu",(unsigned long)characterChoices.count);
			Inv_Oconfidence = [Inv_Otesseract meanConfidence];
					if (self.DEBUG ) NSLog(@"Only letters ROI Inv_Oconfidence: %ld - Inv_OOCR: %@",(long)Inv_Oconfidence,Inv_OrecognizedText);
			
//			characterChoices=nil;
			
			stringToFind = [self slugify:recognizedText];
			OstringToFind = [self slugify:OrecognizedText];
			Inv_OstringToFind = [self slugify:Inv_OrecognizedText];
			if (
				[stringToFind rangeOfString:@"descrizione"].location != NSNotFound ||
				[stringToFind rangeOfString:@"desc"].location != NSNotFound ||
				[stringToFind rangeOfString:@"des cri"].location != NSNotFound ||
				[@"descrizione"			similarity:stringToFind]>=0.6 ||
				[@"cod descrizione"		similarity:stringToFind]>=0.6 ||
				[@"descrizione articolo" similarity:stringToFind]>=0.6 ||
				[@"descri"				similarity:stringToFind]>=0.6 ||
				[@"doscriziono"			similarity:stringToFind]>=0.6 ||
				[@"doscrizione dei beni"			similarity:stringToFind]>=0.6 ||
				[@"doscrizione dei"			similarity:stringToFind]>=0.6
				)
			{
				prodottoTitleRect = roi;
				hasProdotto=true;
				rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Descrizione trovata!");
				
			}
			else if(
					[OstringToFind rangeOfString:@"descrizione"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"desc"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"des cri"].location != NSNotFound ||
					[@"descrizione"			similarity:OstringToFind]>=0.6 ||
					[@"cod descrizione"		similarity:OstringToFind]>=0.6 ||
					[@"descrizione articolo" similarity:OstringToFind]>=0.6 ||
					[@"descri"				similarity:OstringToFind]>=0.6 ||
					[@"doscriziono"			similarity:OstringToFind]>=0.6 ||
					[@"doscrizione dei beni"			similarity:OstringToFind]>=0.6 ||
					[@"doscrizione dei"			similarity:OstringToFind]>=0.6
					
				)
			{
				prodottoTitleRect = roi;
				hasProdotto=true;
				rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Descrizione trovata!");
				
			}
			else if(
					[Inv_OstringToFind rangeOfString:@"descrizione"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"desc"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"des cri"].location != NSNotFound ||
					[@"descrizione"			similarity:Inv_OstringToFind]>=0.6 ||
					[@"cod descrizione"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"descrizione articolo" similarity:Inv_OstringToFind]>=0.6 ||
					[@"descri"				similarity:Inv_OstringToFind]>=0.6 ||
					[@"doscriziono"			similarity:Inv_OstringToFind]>=0.6 ||
					[@"doscrizione dei beni"			similarity:Inv_OstringToFind]>=0.6 ||
					[@"doscrizione dei"			similarity:Inv_OstringToFind]>=0.6
					
					)
			{
				prodottoTitleRect = roi;
				hasProdotto=true;
				rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Descrizione trovata!");
				
			}
			else if(
					[stringToFind rangeOfString:@"codice articolo"].location != NSNotFound ||
					[@"codice articolo" similarity:stringToFind]>=0.6 ||
					[@"codicearticolo"	similarity:stringToFind]>=0.6 ||
					[@"articolo"		similarity:stringToFind]>=0.6 ||
					[@"cod."			similarity:stringToFind]>=0.7 ||
					[@"cod.art."		similarity:stringToFind]>=0.6 ||
					[@"dodi ari"		similarity:stringToFind]>=0.6 ||
					[@"cod--ari"		similarity:stringToFind]>=0.6 ||
					[@"codice"			similarity:stringToFind]>=0.6
					
					)
			{
				articoloTitleRect = roi;
				hasArticolo=true;
				rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Codice Articolo trovata!");
			}
			else if(
					[OstringToFind rangeOfString:@"codice articolo"].location != NSNotFound ||
					[@"codice articolo" similarity:OstringToFind]>=0.6 ||
					[@"codicearticolo"	similarity:OstringToFind]>=0.6 ||
					[@"articolo"		similarity:OstringToFind]>=0.6 ||
					[@"cod."			similarity:OstringToFind]>=0.7 ||
					[@"cod.art."		similarity:OstringToFind]>=0.6 ||
					[@"dodi ari"		similarity:OstringToFind]>=0.6 ||
					[@"cod--ari"		similarity:OstringToFind]>=0.6 ||
					[@"codice"			similarity:OstringToFind]>=0.6
					)
			{
				articoloTitleRect = roi;
				hasArticolo=true;
				rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Codice Articolo trovata!");
			}
			else if(
					[Inv_OstringToFind rangeOfString:@"codice articolo"].location != NSNotFound ||
					[@"codice articolo" similarity:Inv_OstringToFind]>=0.6 ||
					[@"codicearticolo"	similarity:Inv_OstringToFind]>=0.6 ||
					[@"articolo"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"cod."			similarity:Inv_OstringToFind]>=0.7 ||
					[@"cod.art."		similarity:Inv_OstringToFind]>=0.6 ||
					[@"dodi ari"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"cod--ari"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"codice"			similarity:Inv_OstringToFind]>=0.6
					)
			{
				articoloTitleRect = roi;
				hasArticolo=true;
				rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Codice Articolo trovata!");
			}
			else if (
					 [stringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:stringToFind]>=0.6 ||
					 [@"lono" similarity:stringToFind]>=0.6 ||
					 [@"loùo" similarity:stringToFind]>=0.6 ||
					 [@"loro" similarity:stringToFind]>=0.6
				)
			{
				if (
					[stringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[stringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[stringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[stringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:stringToFind]>=0.6 ||
					[@"seed" similarity:stringToFind]>=0.6 ||
					[@"sced" similarity:stringToFind]>=0.6 ||
					[@"sead" similarity:stringToFind]>=0.6 ||
					[@"scad." similarity:stringToFind]>=0.6 ||
					[@"scadenza" similarity:stringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					lottoTitleRect = roi;
					hasLotto=true;
					rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Lotto trovata!");
				}
			}
			else if (
					 [OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:OstringToFind]>=0.6 ||
					 [@"lono" similarity:OstringToFind]>=0.6 ||
					 [@"loùo" similarity:OstringToFind]>=0.6 ||
					 [@"loro" similarity:OstringToFind]>=0.6
					 )
			{
				if (
					[OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:OstringToFind]>=0.6 ||
					[@"seed" similarity:OstringToFind]>=0.6 ||
					[@"sced" similarity:OstringToFind]>=0.6 ||
					[@"sead" similarity:OstringToFind]>=0.6 ||
					[@"scad." similarity:OstringToFind]>=0.6 ||
					[@"scadenza" similarity:OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					lottoTitleRect = roi;
					hasLotto=true;
					rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Lotto trovata!");
				}
			}
			else if (
					 [Inv_OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"lono" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loùo" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loro" similarity:Inv_OstringToFind]>=0.6
					 )
			{
				if (
					[Inv_OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:Inv_OstringToFind]>=0.6 ||
					[@"seed" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sced" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sead" similarity:Inv_OstringToFind]>=0.6 ||
					[@"scad." similarity:Inv_OstringToFind]>=0.6 ||
					[@"scadenza" similarity:Inv_OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					lottoTitleRect = roi;
					hasLotto=true;
					rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Lotto trovata!");
				}
			}
			else if (
					[stringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[stringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[stringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[stringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:stringToFind]>=0.6 ||
					[@"seed" similarity:stringToFind]>=0.6 ||
					[@"sced" similarity:stringToFind]>=0.6 ||
					[@"sead" similarity:stringToFind]>=0.6 ||
					[@"scad." similarity:stringToFind]>=0.6 ||
					[@"scadenza" similarity:stringToFind]>=0.6
					 )
			{
				if (
					 [stringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:stringToFind]>=0.6 ||
					 [@"lono" similarity:stringToFind]>=0.6 ||
					 [@"loùo" similarity:stringToFind]>=0.6 ||
					 [@"loro" similarity:stringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					scadenzaTitleRect = roi;
					hasScadenza=true;
					rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Scadenza trovata!");
				}
			}
			else if (
					[OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:OstringToFind]>=0.6 ||
					[@"seed" similarity:OstringToFind]>=0.6 ||
					[@"sced" similarity:OstringToFind]>=0.6 ||
					[@"sead" similarity:OstringToFind]>=0.6 ||
					[@"scad." similarity:OstringToFind]>=0.6 ||
					[@"scadenza" similarity:OstringToFind]>=0.6
					 )
			{
				if (
					 [OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:OstringToFind]>=0.6 ||
					 [@"lono" similarity:OstringToFind]>=0.6 ||
					 [@"loùo" similarity:OstringToFind]>=0.6 ||
					 [@"loro" similarity:OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					scadenzaTitleRect = roi;
					hasScadenza=true;
					rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Scadenza trovata!");
				}
			}
			else if (
					[Inv_OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:Inv_OstringToFind]>=0.6 ||
					[@"seed" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sced" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sead" similarity:Inv_OstringToFind]>=0.6 ||
					[@"scad." similarity:Inv_OstringToFind]>=0.6 ||
					[@"scadenza" similarity:Inv_OstringToFind]>=0.6
					 )
			{
				if (
					 [Inv_OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"lono" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loùo" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loro" similarity:Inv_OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					scadenzaTitleRect = roi;
					hasScadenza=true;
					rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Scadenza trovata!");
				}
			}
			current_progress+=_step;
			self.progressView.progress = current_progress/100.0f;
			dispatch_async(dispatch_get_main_queue(), ^{
				[self.progressView setProgress:(CGFloat)current_progress/100 animated:YES];
			});
			
		}
		seach_count++;
	}
	*/
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)45/100 animated:YES];
	});
	
	tesseract.charWhitelist = charWhiteList;
	Otesseract.charWhitelist = charWhiteList;
	Inv_Otesseract.charWhitelist = charWhiteList;
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)46/100 animated:YES];
	});
	force_exit = false;
	
	self.finalData = [[NSMutableDictionary alloc] init];
	if(prodottoTitleRect.area()>0)
	{
		NSMutableArray *codiciArticoloList = [[NSMutableArray alloc] init];
		if (hasArticolo)
		{
			cv::Rect _roi;
			for (int i=0; ((i < textBoxesOthers.size()) && !force_exit ); i++)
			{
				_roi = textBoxesOthers[i];
				if (
					( _roi.y>articoloTitleRect.y+articoloTitleRect.height)
					&&
					(
						(_roi.x<=articoloTitleRect.x && _roi.x+_roi.width>=articoloTitleRect.x+articoloTitleRect.width) ||
   						(_roi.x<=articoloTitleRect.x && _roi.x+_roi.width<=articoloTitleRect.x+articoloTitleRect.width && _roi.x+_roi.width>articoloTitleRect.x)
					)
				)
				{
					RoiObject *rObj = [[RoiObject alloc] init];
					rObj.item = _roi;
					[codiciArticoloList addObject:rObj];
					//if (self.DEBUG) rectangle(OriginalBW_copy, _roi.tl(), _roi.br(), Scalar(100,100,100), 3);
					if (self.DEBUG) rectangle(OriginalBW_copy2, _roi.tl(), _roi.br(), Scalar(100,100,100), 3);
				}
			}
			if(self.DEBUG){
				filename = @"detectTextBoxes_1.jpg";
				imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
				UIImage *temp = MatToUIImage(OriginalBW_copy2);
				NSData *tempimg = UIImageJPEGRepresentation(temp,100.0f);
				if (self.DEBUG ) [tempimg writeToFile:imagePath atomically:YES];
				temp=nil;
				tempimg=nil;
			}
		}
		dispatch_async(dispatch_get_main_queue(), ^{
			[self.progressView setProgress:(CGFloat)47/100 animated:YES];
		});
		OriginalBW_copy2.release();			

		if(hasLotto)
		{
			cv::Rect searchAreaLotto = *new cv::Rect(lottoTitleRect.x,prodottoTitleRect.y,lottoTitleRect.width,prodottoTitleRect.height);
			if ([self intersect_X:searchAreaLotto second:lottoTitleRect])
			{
				stessaLinea_lotto = true;
				lottoTitleRect.x = (int)(lottoTitleRect.x-(lottoTitleRect.width*0.5))+5;
				lottoTitleRect.width = (int)(lottoTitleRect.width+(lottoTitleRect.width*0.5)+(lottoTitleRect.width*0.7));
				NSLog(@"Il campo lotto sta sulla stessa linea di descrizione!");
			}
		}
		if(hasScadenza)
		{
			cv::Rect searchAreaScadenza = *new cv::Rect(scadenzaTitleRect.x,prodottoTitleRect.y,scadenzaTitleRect.width,prodottoTitleRect.height);
			if ([self intersect_X:searchAreaScadenza second:scadenzaTitleRect])
			{
				stessaLinea_scadenza = true;
				NSLog(@"Il campo scadenza sta sulla stessa linea di descrizione!");
			}
		}
		NSMutableArray *jrows = [[NSMutableArray alloc] init];
		
		dispatch_async(dispatch_get_main_queue(), ^{
			[self.progressView setProgress:(CGFloat)48/100 animated:YES];
		});
		
		if(!hasArticolo)
		{
			NSMutableArray *tableHeader = [[NSMutableArray alloc] init];
			
			RoiObject *prodotto_rObj = [[RoiObject alloc] init];
			prodotto_rObj.item = prodottoTitleRect;
			[tableHeader addObject:prodotto_rObj];
			
			cv::Rect __troi;

			for (int i=0; ((i < textBoxesOthers.size()) && !force_exit ); i++)
			{
				__troi = textBoxesOthers[i];

				if(
						(
							(__troi.y>=prodottoTitleRect.y && __troi.y<prodottoTitleRect.y+prodottoTitleRect.height/3) ||
							(__troi.y<prodottoTitleRect.y && __troi.y+__troi.height>prodottoTitleRect.y)
						) && __troi.x<prodottoTitleRect.x
					)
				{
					RoiObject *rObj = [[RoiObject alloc] init];
					rObj.item = __troi;
					[tableHeader addObject:rObj];
					rObj=nil;
				}
			}
			
			[tableHeader sortUsingFunction:compare context: NULL];
			
			index = [tableHeader indexOfObject:prodotto_rObj];
			
			leftColumn = *new cv::Rect(0,prodottoTitleRect.y,1,prodottoTitleRect.height);
			
			if (index>0)
			{
				leftColumn = [(RoiObject *)[tableHeader objectAtIndex:(index-1)] item];
			}
			
			newX = (int)(leftColumn.x+leftColumn.width*1.3);
			if (newX>prodottoTitleRect.x)
			{
				newX = prodottoTitleRect.x;
			}
			widthIncrease = prodottoTitleRect.x-newX;
			prodottoTitleRect.width += widthIncrease;
			prodottoTitleRect.x = newX;
		}
		else
		{
			newX = (int)((articoloTitleRect.x+articoloTitleRect.width)*1.15);
			if (newX>prodottoTitleRect.x) 
    		{
    			newX = prodottoTitleRect.x;
    		}
			widthIncrease = prodottoTitleRect.x-newX;
			prodottoTitleRect.width += widthIncrease;
			prodottoTitleRect.x = newX;
		}
		
		cv::Rect final_area = *new cv::Rect(prodottoTitleRect.x, prodottoTitleRect.y, sobel.cols-prodottoTitleRect.x, sobel.rows-prodottoTitleRect.y);

		cv::Mat OriginalBW_copy_cut = *new cv::Mat(OriginalBW_copy,final_area);
		if(secondTry)
		{
			cv::bitwise_not(Inv_OriginalBW,Inv_OriginalBW); // -5MB
			cv::Mat OriginalBW_copy_cut = *new cv::Mat(Inv_OriginalBW,final_area);
		}
		else
		{
			cv::bitwise_not(Inv_OriginalBW2,Inv_OriginalBW2); // -5MB
			cv::Mat OriginalBW_copy_cut = *new cv::Mat(Inv_OriginalBW2,final_area);
		}

		if (self.DEBUG )
		{
			filename = @"44-OriginalBW_copy_cut.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			UIImage *temp = MatToUIImage(OriginalBW_copy_cut);
			NSData *tempimg = UIImageJPEGRepresentation(temp,100.0f);
			[tempimg writeToFile:imagePath atomically:YES];
			temp = nil;
			tempimg=nil;
		}
		Mat sobel_cut = *new Mat(sobel,final_area);
		Mat thresh_cut = *new Mat(thresh,final_area);
		
		dilate(sobel_cut,thresh_dilate_cut,getStructuringElement(MORPH_RECT, cv::Size(19,1)),cv::Point(-1,-1),9); //-3MB
		bitwise_or(thresh_dilate_cut, thresh_cut, thresh_dilate_cut);
		
		erode(thresh_dilate_cut,thresh_dilate_cut,getStructuringElement(MORPH_RECT, cv::Size(1,3))); //-2MB
		erode(thresh_dilate_cut,thresh_dilate_cut,getStructuringElement(MORPH_RECT, cv::Size(1,3))); //-2MB
		erode(thresh_dilate_cut,thresh_dilate_cut,getStructuringElement(MORPH_RECT, cv::Size(1,3))); //-2MB
		dilate(thresh_dilate_cut,thresh_dilate_cut,getStructuringElement(MORPH_RECT, cv::Size(1,3)));
		dilate(thresh_dilate_cut,thresh_dilate_cut,getStructuringElement(MORPH_RECT, cv::Size(1,3)));
		dilate(thresh_dilate_cut,thresh_dilate_cut,getStructuringElement(MORPH_RECT, cv::Size(1,3)));
		
		Mat thresh_dilate = *new Mat(OriginalBW_copy.rows, OriginalBW_copy.cols, thresh_dilate_cut.type());
		Mat dst_roi = thresh_dilate(final_area);
		thresh_dilate_cut.copyTo(dst_roi); //-3MB
		
		erode(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(3,3))); //-2MB
		dilate(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(3,3)));
		erode(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(3,3)),cv::Point(-1,-1),3); //-2MB
		dilate(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(3,3)),cv::Point(-1,-1),2);
		dilate(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(19,1)),cv::Point(-1,-1),9); //-3MB
		erode(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(3,3)),cv::Point(-1,-1),3); //-2MB
		dilate(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(19,1)),cv::Point(-1,-1),9); //-3MB
		dilate(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(3,3)),cv::Point(-1,-1),6); //-3MB
		GaussianBlur(thresh_dilate, thresh_dilate, cv::Size(11,11), 3);
		threshold(thresh_dilate, thresh_dilate, 10, 255, THRESH_BINARY);
//		erode(thresh_dilate,thresh_dilate,getStructuringElement(MORPH_RECT, cv::Size(3,3))); //-2MB
		if (self.DEBUG )
		{
			filename = @"55-OriginalBW_copy_cut.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			UIImage *temp = MatToUIImage(thresh_dilate);
			NSData *tempimg = UIImageJPEGRepresentation(temp,100.0f);
			[tempimg writeToFile:imagePath atomically:YES];
			temp = nil;
			tempimg=nil;
		}
		
		Mat final_cut = *new Mat(thresh_dilate,final_area);
		Mat final_thresh_dilate = *new Mat(OriginalBW_copy.rows, OriginalBW_copy.cols, thresh_dilate_cut.type());
		dst_roi = final_thresh_dilate(final_area);
		final_cut.copyTo(dst_roi);
		
		if (self.DEBUG )
		{
			filename = @"60-morphologyEx_cut.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			UIImage *temp = MatToUIImage(final_thresh_dilate);
			NSData *tempimg = UIImageJPEGRepresentation(temp,100.0f);
			[tempimg writeToFile:imagePath atomically:YES];
			temp=nil;
			tempimg=nil;
		}
		
		dispatch_async(dispatch_get_main_queue(), ^{
			[self.progressView setProgress:(CGFloat)49/100 animated:YES];
		});
		
		if (self.DEBUG) NSLog(@"detectTextBoxes(thresh_dilate,OriginalBW_copy,2)");
		vector<cv::Rect> textBoxes = [self detectTextBoxes:thresh_dilate original:OriginalBW_copy idx:2 minWidth:self.minWidth2 minHeight:self.minHeight2 maxWidth:self.maxWidth2 maxHeight:self.maxHeight2 minArea:self.minArea2 maxArea:self.maxArea2];

		if (self.DEBUG )
		{
			filename = @"detectTextBoxes_2.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			UIImage *temp = MatToUIImage(OriginalBW_copy);
			NSData *tempimg = UIImageJPEGRepresentation(temp,100.0f);
			[tempimg writeToFile:imagePath atomically:YES];
			temp=nil;
			tempimg=nil;
		}
		dst_roi.release();
		thresh_dilate_cut.release(); //+2MB
		thresh_cut.release();
		sobel.release();
		OriginalBW_copy_cut.release();
		
		dispatch_async(dispatch_get_main_queue(), ^{
			[self.progressView setProgress:(CGFloat)50/100 animated:YES];
		});
		double step = (double)50/(double)textBoxes.size();
		double current_progress = 50;
		
		int count = 0;
		NSString* lotto = @"nessuno";
		NSString* scadenza = @"nessuna";
		
		cv::Rect __roi;
		NSMutableArray *ocrTextList;
    	OcrRect *BW_OrecognizedText;
    	OcrRect *AD_OrecognizedText;
    	OcrRect *MO_OrecognizedText;
		
		NSMutableDictionary *jobj;
		NSString *recognizedText_lotto = @"";
		
		NSMutableArray *ocrTextList_lotto;
    	OcrRect *BW_recognizedText_lotto;
    	OcrRect *AD_recognizedText_lotto;
    	OcrRect *MO_recognizedText_lotto;
		bool hasError = false;
		NSString *first = @"";
		NSString *last = @"";
		NSString *recognizedText_scadenza = @"";
		
		NSMutableArray *ocrTextList_scadenza;
    	OcrRect *BW_recognizedText_scadenza;
    	OcrRect *AD_recognizedText_scadenza;
    	OcrRect *MO_recognizedText_scadenza;
		bool lottoPresent = false;
		bool scadenzaPresent = false;
		Inv_Otesseract.image = MatToUIImage(inputMat_morph); //+5MB
		NSArray *parts;
		force_exit = false;
		for (int i=0; ((i < textBoxes.size()) && !force_exit ); i++)
		{
			__roi = textBoxes[i];
			if(__roi.width>=0 && __roi.height>=0)
			{
					if (
		    				((__roi.y)>=(prodottoTitleRect.y+prodottoTitleRect.height)) && // originariente: +prodottoTitleRect.height/3
		    				(
	    						(__roi.x+__roi.width>=prodottoTitleRect.x && __roi.x+__roi.width<=prodottoTitleRect.x+prodottoTitleRect.width) ||
	    						(__roi.x>=prodottoTitleRect.x && __roi.x<=prodottoTitleRect.x+prodottoTitleRect.width) ||
	    						(__roi.x<=prodottoTitleRect.x && __roi.x+__roi.width>=prodottoTitleRect.x+prodottoTitleRect.width)
							)
						)
		    		{
						ocrTextList = [[NSMutableArray alloc] init];
						if(self.DEBUG) NSLog(@"roi: %d,%d,%d,%d",__roi.x,__roi.y,__roi.width,__roi.height);
						//__roi =[self fixRoiBy:__roi codiciArticoloList:codiciArticoloList];

						BW_OrecognizedText = [self GetText:__roi tess:&Otesseract];
						[ocrTextList addObject:BW_OrecognizedText];
						if(self.DEBUG) NSLog(@"BW - confidence: %ld - OCR: %@",(long)[BW_OrecognizedText getConfidence],[BW_OrecognizedText getText]);

						AD_OrecognizedText = [self GetText:__roi tess:&tesseract];
						[ocrTextList addObject:AD_OrecognizedText];
						if(self.DEBUG) NSLog(@"AD - confidence: %ld - OCR: %@",(long)[AD_OrecognizedText getConfidence],[AD_OrecognizedText getText]);

						MO_OrecognizedText = [self GetText:__roi tess:&Inv_Otesseract];
						[ocrTextList addObject:MO_OrecognizedText];
						if(self.DEBUG) NSLog(@"MO - confidence: %ld - OCR: %@",(long)[MO_OrecognizedText getConfidence],[MO_OrecognizedText getText]);

						count++;
						
						NSLog(@"Before sort");
						for(OcrRect *subArray in ocrTextList) {
							NSLog(@"Array in ocrTextList: %ld - %@",(long)[subArray getConfidence],[subArray getText]);
						}
						
						[ocrTextList sortUsingFunction:compareOcrRect context: NULL];
						
						NSLog(@"After sort");
						for(OcrRect *subArray in ocrTextList) {
							NSLog(@"Array in ocrTextList: %ld - %@",(long)[subArray getConfidence],[subArray getText]);
						}
						
						recognizedText = @"";
						
						if(ocrTextList.count>0)
						{
							OcrRect *orect = (OcrRect*)[ocrTextList objectAtIndex:0];
							recognizedText = [[orect getText] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
							if(recognizedText.length>2 &&
								(
									[[[recognizedText substringWithRange:NSMakeRange(0,2)] lowercaseString] isEqualToString:@"i "] ||
									[[[recognizedText substringWithRange:NSMakeRange(0,2)] lowercaseString] isEqualToString:@"1 "] ||
									[[[recognizedText substringWithRange:NSMakeRange(0,2)] lowercaseString] isEqualToString:@"l "]
								)
								)
							{
								recognizedText = [recognizedText substringWithRange:NSMakeRange(0,2)];
							}
							if (self.DEBUG) NSLog(@"BEST - confidence: %ld - OCR: %@ ",(long)[orect getConfidence], recognizedText);
							if (self.DEBUG) NSLog(@"-------------------------------------------------------");
						}
						ocrTextList=nil;
						if(recognizedText.length>=6 && recognizedText.length<=100)
						{
							if(stessaLinea_lotto || stessaLinea_scadenza)
							{
								jobj = [[NSMutableDictionary alloc] init];
								NSString *Tfirst = [recognizedText substringWithRange:NSMakeRange(0,1)];
								
								if([Tfirst isEqualToString:@"'"] || [Tfirst isEqualToString:@"."] || [Tfirst isEqualToString:@","])
								{
									recognizedText = [recognizedText substringWithRange:NSMakeRange(1,recognizedText.length-1)];
								}
								
								if (self.DEBUG) NSLog(@"Prodotto: %@",recognizedText);
								jobj[@"prodotto"]=recognizedText;
								
								if(stessaLinea_lotto)
								{
									if (self.DEBUG) NSLog(@"Lotto:-------------------------------------------------------");
									recognizedText_lotto = @"";
									ocrTextList_lotto = [[NSMutableArray alloc] init];
									BW_recognizedText_lotto = [self findNearestRightSameLine:lottoTitleRect roi:__roi prefix:@"LOTT-BW" tesseract:&tesseract image:OriginalBW];
									if(BW_recognizedText_lotto)
									{
										[ocrTextList_lotto addObject:BW_recognizedText_lotto];
										if(self.DEBUG) NSLog(@"BW - confidence: %ld - OCR: %@",(long)BW_recognizedText_lotto.getConfidence,BW_recognizedText_lotto.getText);
									}
									AD_recognizedText_lotto = [self findNearestRightSameLine:lottoTitleRect roi:__roi prefix:@"LOTT-AD" tesseract:&tesseract image:inputMat];
									if(AD_recognizedText_lotto)
									{
										[ocrTextList_lotto addObject:AD_recognizedText_lotto];
										if(self.DEBUG) NSLog(@"AD - confidence: %ld - OCR: %@",(long)AD_recognizedText_lotto.getConfidence,AD_recognizedText_lotto.getText);
									}
									MO_recognizedText_lotto = [self findNearestRightSameLine:lottoTitleRect roi:__roi prefix:@"LOTT-AD" tesseract:&tesseract image:inputMat_morph];
									if(MO_recognizedText_lotto)
									{
										[ocrTextList_lotto addObject:MO_recognizedText_lotto];
										if(self.DEBUG) NSLog(@"MO - confidence: %ld - OCR: %@",(long)MO_recognizedText_lotto.getConfidence,MO_recognizedText_lotto.getText);
									}
									[ocrTextList_lotto sortUsingFunction:compareOcrRect context: NULL];
									
									if(ocrTextList_lotto.count>0)
									{
										OcrRect *orect = (OcrRect*)[ocrTextList_lotto objectAtIndex:0];
										recognizedText_lotto = [[orect getText] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
										recognizedText_lotto = [recognizedText_lotto stringByReplacingOccurrencesOfString:@"\n" withString:@""];
										if (self.DEBUG) NSLog(@"BEST - confidence: %ld - OCR: %@",(long)orect.getConfidence,recognizedText_lotto);
										if (self.DEBUG) NSLog(@"-------------------------------------------------------");
									}
									if(recognizedText_lotto.length>=2)
									{
										hasError = true;
										first = [[recognizedText_lotto substringWithRange:NSMakeRange(0,1)] lowercaseString];
										if(self.DEBUG) NSLog(@"first char: %@",first);
										if([first isEqualToString:@"1 "] && recognizedText_lotto.length>1)
										{
											recognizedText_lotto = [recognizedText_lotto substringWithRange:NSMakeRange(1,recognizedText_lotto.length-1)];
										}
										recognizedText_lotto = [recognizedText_lotto stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
										while(hasError && recognizedText_lotto.length>1)
										{
											first = [[recognizedText_lotto substringWithRange:NSMakeRange(0,1)] lowercaseString];
											if(self.DEBUG) NSLog(@"first char: %@",first);
											
											if([first rangeOfCharacterFromSet:[NSCharacterSet decimalDigitCharacterSet]].location!=NSNotFound)
											{
												hasError = false;
											}
											else
											{
												recognizedText_lotto = [[recognizedText_lotto substringWithRange:NSMakeRange(1,recognizedText_lotto.length-2)] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
											}
											
										}
										hasError = true;
										while (hasError && recognizedText_lotto.length>1)
										{
											last = [[recognizedText_lotto substringWithRange:NSMakeRange(recognizedText_lotto.length-2,1)] lowercaseString];
											if (self.DEBUG) NSLog(@"last char: %@",last);
											if([last rangeOfCharacterFromSet:[NSCharacterSet decimalDigitCharacterSet]].location!=NSNotFound)
											{
												hasError = false;
											}
											else
											{
												recognizedText_lotto = [[recognizedText_lotto substringWithRange:NSMakeRange(0,recognizedText_lotto.length-2)]stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
											}
										}
										
									}
									else
									{
										recognizedText_lotto = @"nessuno";
									}
									recognizedText_lotto = [recognizedText_lotto stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
									if (recognizedText_lotto.length==1 || [recognizedText_lotto isEqualToString:@""])
									{
										recognizedText_lotto = @"nessuno";
									}
									if (recognizedText_lotto==nil)
									{
										recognizedText_lotto = @"nessuno";
									}
									if(self.DEBUG) NSLog(@"Lotto: %@",recognizedText_lotto);
									jobj[@"lotto"]=recognizedText_lotto;
								}
								else
								{
									jobj[@"lotto"]=recognizedText_lotto;
								}
								if(stessaLinea_scadenza)
								{
									if (self.DEBUG) NSLog(@"Scadenza: ---------------------------------------------------------");
									recognizedText_scadenza = @"";
									ocrTextList_scadenza = [[NSMutableArray alloc] init];
									BW_recognizedText_scadenza = [self findNearestRightSameLine:scadenzaTitleRect roi:__roi prefix:@"SCAD-BW" tesseract:&tesseract image:OriginalBW];
									if(BW_recognizedText_scadenza)
									{
										[ocrTextList_scadenza addObject:BW_recognizedText_scadenza];
										if(self.DEBUG) NSLog(@"BW - confidence: %ld - OCR: %@",(long)BW_recognizedText_scadenza.getConfidence,BW_recognizedText_scadenza.getText);

									}
									AD_recognizedText_scadenza = [self findNearestRightSameLine:scadenzaTitleRect roi:__roi prefix:@"SCAD-AD" tesseract:&tesseract image:inputMat];
									if(AD_recognizedText_scadenza)
									{
										[ocrTextList_scadenza addObject:AD_recognizedText_scadenza];
										if(self.DEBUG) NSLog(@"BW - confidence: %ld - OCR: %@",(long)AD_recognizedText_scadenza.getConfidence,AD_recognizedText_scadenza.getText);

									}
									MO_recognizedText_scadenza = [self findNearestRightSameLine:scadenzaTitleRect roi:__roi prefix:@"SCAD-MO" tesseract:&tesseract image:inputMat_morph];
									if(MO_recognizedText_scadenza)
									{
										[ocrTextList_scadenza addObject:MO_recognizedText_scadenza];
										if(self.DEBUG) NSLog(@"MO - confidence: %ld - OCR: %@",(long)MO_recognizedText_scadenza.getConfidence,MO_recognizedText_scadenza.getText);
									}

									[ocrTextList_scadenza sortUsingFunction:compareOcrRect context: NULL];
									if(ocrTextList_scadenza.count>0)
									{
										OcrRect *orect = (OcrRect*)[ocrTextList_scadenza objectAtIndex:0];
										recognizedText_scadenza = [[orect getText] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
										recognizedText_scadenza = [recognizedText_scadenza stringByReplacingOccurrencesOfString:@"\n" withString:@"" ];
										if (self.DEBUG) NSLog(@"BEST - confidence: %ld - OCR: %@",(long)orect.getConfidence,recognizedText_scadenza);
										if (self.DEBUG) NSLog(@"-------------------------------------------------------");
									}
									if(recognizedText_scadenza.length>0)
									{
										hasError = true;
//										first = [[recognizedText_scadenza substringWithRange:NSMakeRange(0,1)] lowercaseString];
//										if(self.DEBUG) NSLog(@"first char: %@",first);
										while(hasError && recognizedText_scadenza.length>0)
										{
											first = [[recognizedText_scadenza substringWithRange:NSMakeRange(0,1)] lowercaseString];
											if(self.DEBUG) NSLog(@"first char: %@",first);
											
											if([first rangeOfCharacterFromSet:[NSCharacterSet decimalDigitCharacterSet]].location!=NSNotFound)
											{
												hasError = false;
											}
											else
											{
												recognizedText_scadenza = [[recognizedText_scadenza substringWithRange:NSMakeRange(1,recognizedText_scadenza.length-1)] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
											}
											
										}
										hasError = true;
										while (hasError && recognizedText_scadenza.length>0)
										{
											last = [[recognizedText_scadenza substringWithRange:NSMakeRange(recognizedText_scadenza.length-1,1)] lowercaseString];
											if (self.DEBUG) NSLog(@"last char: %@",last);
											if([last rangeOfCharacterFromSet:[NSCharacterSet decimalDigitCharacterSet]].location!=NSNotFound)
											{
												hasError = false;
											}
											else
											{
												recognizedText_scadenza = [[recognizedText_scadenza substringWithRange:NSMakeRange(0,recognizedText_scadenza.length-1)]stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
											}
										}
										if (recognizedText_scadenza.length>1)
										{
											recognizedText_scadenza = [self fixYear:recognizedText_scadenza];
										}
									}
									recognizedText_scadenza = [recognizedText_scadenza stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
									if(recognizedText_scadenza.length==1 || [recognizedText_scadenza isEqualToString:@""])
									{
										recognizedText_scadenza = @"nessuna";
									}
									if (recognizedText_scadenza==nil)
									{
										recognizedText_scadenza = @"nessuna";
									}
									
									if(self.DEBUG) NSLog(@"Scadenza: %@",recognizedText_scadenza);
									jobj[@"scadenza"]=recognizedText_scadenza;
								}
								else
								{
									jobj[@"scadenza"]=@"nessuna";
								}
								jobj[@"conforme"]=@"Conforme";
								jobj[@"index"]=[NSString stringWithFormat:@"%d", count];
								[jrows addObject:jobj];
							}
							else
							{
								recognizedText = [recognizedText stringByReplacingOccurrencesOfString:@"lollo" withString:@"Lotto"];
								recognizedText = [recognizedText stringByReplacingOccurrencesOfString:@"Lollo" withString:@"Lotto"];
								if(
									( [recognizedText rangeOfString:@"Lotto"].location!=NSNotFound )
									||
									( [recognizedText rangeOfString:@"Scadenza"].location!=NSNotFound ) )
								{
									lottoPresent = ([recognizedText rangeOfString:@"Lotto"].location!=NSNotFound);
									scadenzaPresent = ([recognizedText rangeOfString:@"Scadenza"].location!=NSNotFound);
									if(self.DEBUG) NSLog(@"Campo Lotto e/o Scadenza trovato!");
									recognizedText = [recognizedText stringByReplacingOccurrencesOfString:@"otto" withString:@"otto:"];
									recognizedText = [recognizedText stringByReplacingOccurrencesOfString:@"cadenza" withString:@"cadenza:"];
									recognizedText = [recognizedText stringByReplacingOccurrencesOfString:@"::" withString:@":"];
									recognizedText = [recognizedText stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
									first = [[recognizedText substringWithRange:NSMakeRange(0,1)] lowercaseString];
									if(![first isEqualToString:@"L"])
									{
										recognizedText = [[recognizedText substringWithRange:NSMakeRange(1,recognizedText.length-2)] lowercaseString];
									}
									if(self.DEBUG) NSLog(@"Prodotto: %@",recognizedText);
									if(scadenzaPresent && lottoPresent)
									{
										parts = [[recognizedText stringByReplacingOccurrencesOfString:@"Lotto" withString:@""] componentsSeparatedByString:@"Scadenza:"];
									}
									else if(!lottoPresent && scadenzaPresent)
									{
										recognizedText = [NSString stringWithFormat:@"Lotto: %@ %@",lotto, recognizedText];
										parts = [[recognizedText stringByReplacingOccurrencesOfString:@"Lotto" withString:@""] componentsSeparatedByString:@"Scadenza:"];
									}
									else if(lottoPresent && !scadenzaPresent)
									{
										recognizedText = [NSString stringWithFormat:@"%@ Scadenza: %@",recognizedText, scadenza];
										parts = [[recognizedText stringByReplacingOccurrencesOfString:@"Lotto" withString:@""] componentsSeparatedByString:@"Scadenza:"];
									}
									@try {
    									lotto = (NSString*)[parts objectAtIndex:0];
										scadenza = (NSString*)[parts objectAtIndex:1];
									}
									@catch (NSException *exception) {
										lotto = @"nessuno";
										scadenza = @"nessuno";
									}
									if (self.DEBUG) NSLog(@"RESULT => Lotto: %@ - Scadenza: %@", lotto, scadenza);
									if (self.DEBUG) NSLog(@"----------------------------------------------------");
									
								}
								else
								{
									jobj = [[NSMutableDictionary alloc] init];
									jobj[@"index"]=[NSString stringWithFormat:@"%d", count];
									jobj[@"prodotto"]=recognizedText;
									jobj[@"lotto"]=lotto;
									jobj[@"scadenza"]=scadenza;
									jobj[@"conforme"]=@"Conforme";
									[jrows addObject:jobj];
									lotto = @"nessuno";
									scadenza = @"nessuna";
									
								}
							}
						}

					}
					current_progress+=step;
					dispatch_async(dispatch_get_main_queue(), ^{
						[self.progressView setProgress:(CGFloat)current_progress/100 animated:YES];
					});
				
			}
		}
		codiciArticoloList=nil;
		self.finalData[@"rowdata"]=jrows;
		
		dispatch_async(dispatch_get_main_queue(), ^{
			[self.progressView setProgress:(CGFloat)100/100 animated:YES];
		});
		
		thresh_dilate.release();
		
		
	}
	thresh.release();
	inputMat.release();
	inputMat_morph.release();
	OriginalBW.release();
	OriginalBW_copy.release();
	OriginalBW_copy2.release();
	Inv_OriginalBW.release();
	Inv_OriginalBW2.release();
	thresh_dilate_cut.release();
	
    NSLog(@"Processing done!");

	return 0;
}
-(NSString*)fixYear:(NSString*)data
{
	NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
	NSInteger current_year = [components year];

	if(data.length>1)
	{
		bool parseError = true;
		NSString *first = @"";
		while (parseError && data.length>1)
		{
			first = [[data substringWithRange:NSMakeRange(0,1)] lowercaseString];
			if([first rangeOfCharacterFromSet:[NSCharacterSet decimalDigitCharacterSet]].location!=NSNotFound)
			{
				parseError = false;
			}
			else
			{
				data = [[data substringWithRange:NSMakeRange(1,data.length-1)] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
			}
		}
	}
	if(data.length>1)
	{
		bool parseError = true;
		NSString *last = @"";
		while (parseError && data.length>1)
		{
			last = [[data substringWithRange:NSMakeRange(data.length-1,1)] lowercaseString];
			if([last rangeOfCharacterFromSet:[NSCharacterSet decimalDigitCharacterSet]].location!=NSNotFound)
			{
				parseError = false;
			}
			else
			{
				data = [[data substringWithRange:NSMakeRange(0,data.length-1)] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
			}
		}
	}
	if(data.length>1)
	{
		data = [data stringByReplacingOccurrencesOfString:@"-" withString:@"/"];
		data = [data stringByReplacingOccurrencesOfString:@" " withString:@""];
		NSArray *parts = [data componentsSeparatedByString:@"/"];
		if(data.length==8 && parts.count<3)
		{
			data = [NSString stringWithFormat:@"%@/%@/20%@",[data substringWithRange:NSMakeRange(0, 1)],[data substringWithRange:NSMakeRange(3, 4)],[data substringWithRange:NSMakeRange(6, 7)]];
		}
		else if(data.length<8 && parts.count==3)
		{
			NSString *year = (NSString*)[parts objectAtIndex:2];
			NSString *month = (NSString*)[parts objectAtIndex:1];
			if (month.length==1)
			{
				month = [NSString stringWithFormat:@"0%@",month];
			}
			NSString *day = (NSString*)[parts objectAtIndex:0];
			if (day.length==1)
			{
				day = [NSString stringWithFormat:@"0%@",day];
			}
			if (year.length==2)
			{
				year = [NSString stringWithFormat:@"20%@",year];
			}
			else if(year.length==1)
			{
				year = [NSString stringWithFormat:@"%ld",(long)(current_year+1)];
			}
			data = [NSString stringWithFormat:@"%@/%@/20%@",day,month,year];
		}
		else if(data.length<8 && parts.count==2)
		{
			NSString *year = (NSString*)[parts objectAtIndex:1];
			NSString *month = (NSString*)[parts objectAtIndex:0];
			if (month.length==1)
			{
				month = [NSString stringWithFormat:@"0%@",month];
			}
			if(year.length==1)
			{
				year = [NSString stringWithFormat:@"%ld",(long)(current_year+1)];
			}
			data = [NSString stringWithFormat:@"01/%@/20%@",month,year];
		}
		else if(data.length==8 && parts.count==3)
		{
			data = [NSString stringWithFormat:@"%@/%@/20%@",(NSString*)[parts objectAtIndex:0],(NSString*)[parts objectAtIndex:1],(NSString*)[parts objectAtIndex:2]];
		}
	}
	return data;
}

-(OcrRect*)findNearestRightSameLine:(cv::Rect)roiColumn roi:(cv::Rect)roiRow prefix:(NSString*)prefix tesseract:(G8Tesseract**)tesseract image:(cv::Mat&)image
{
	OcrRect *Oresult = [[OcrRect alloc] init];
	if(roiColumn.area()>0)
	{
		cv::Rect searchArea = *new cv::Rect(cv::Point(roiColumn.x,roiRow.y-10),cv::Point(roiColumn.x+roiColumn.width,roiRow.y+roiRow.height+20));
		int diff = 0;
		if(searchArea.x+searchArea.width>image.cols)
		{
			diff = searchArea.x+searchArea.width-image.cols;
			searchArea.width = searchArea.width-diff;
		}
		if(searchArea.y+searchArea.height>image.rows)
		{
			diff = searchArea.y+searchArea.height-image.rows;
			searchArea.height = searchArea.height-diff;
		}
		(*tesseract).rect=CGRectMake(searchArea.x, searchArea.y, searchArea.width, searchArea.height);
		[*tesseract recognize];
		[Oresult setArea:searchArea];
		[Oresult setText:[*tesseract recognizedText]];
		NSInteger mconf = [*tesseract meanConfidence];
		[Oresult setConfidence:mconf];
	}
	return Oresult;
}

-(OcrRect*)GetText:(cv::Rect)roi tess:(G8Tesseract**)tesseract
{
	(*tesseract).rect=CGRectMake(roi.x, roi.y, roi.width, roi.height);
	[*tesseract recognize];
	OcrRect *tmp = [[OcrRect alloc] init];
	[tmp setArea:roi];
	[tmp setText:[*tesseract recognizedText]];
	NSInteger mconf = [*tesseract meanConfidence];
	[tmp setConfidence:mconf];
	return tmp;
}

NSComparisonResult compare(id obj1, id obj2, void *context) {
	RoiObject *o1 = (RoiObject *)obj1;
	RoiObject *o2 = (RoiObject *)obj2;

  if (o1.item.x < o2.item.x)
      return NSOrderedAscending;
  else if (o1.item.x > o2.item.x)
      return NSOrderedDescending;
  else 
    return NSOrderedSame;
}

NSComparisonResult compareOcrRect(id obj1, id obj2, void *context){
	OcrRect *o1 = (OcrRect *)obj1;
	OcrRect *o2 = (OcrRect *)obj2;

  if ([o1 getConfidence] > [o2 getConfidence])
      return NSOrderedAscending;
  else if ([o1 getConfidence] < [o2 getConfidence])
      return NSOrderedDescending;
  else 
    return NSOrderedSame;
}

-(bool)intersect_Y:(cv::Rect)A second:(cv::Rect)B
{
	return (B.tl().y>=A.tl().y && B.tl().y<=A.br().y)
			|| (A.tl().y<=B.br().y && A.tl().y>=B.tl().y);
}

-(bool)intersect_X:(cv::Rect)A second:(cv::Rect)B
{
	return (B.tl().x>=A.tl().x && B.tl().x<=A.br().x)
			|| (A.tl().x<=B.br().x && A.tl().x>=B.tl().x);
}

-(cv::Rect)fixRoiBy:(cv::Rect)roi codiciArticoloList:(NSMutableArray*)splitters{
	bool found = false;
	for (int i=0; i<splitters.count && !found; i++)
	{
		RoiObject *splitter = splitters[i];
		if ([self intersect_X:roi second:splitter.item] && [self intersect_Y:roi second:splitter.item])
		{
			roi = *new cv::Rect(splitter.item.x+splitter.item.width+15,roi.y+2,(roi.x+roi.width)-(splitter.item.x+splitter.item.width),roi.height-4);
			found = true;
		}
	}
	return roi;
}

-(vector<cv::Rect>) detectTextBoxes:(Mat&)inputMat
	original:(Mat&)Original
	idx:(int)idx
	minWidth:(NSInteger)minWidth
	minHeight:(NSInteger)minHeight
	maxWidth:(NSInteger)maxWidth
	maxHeight:(NSInteger)maxHeight
	minArea:(NSInteger)minArea
	maxArea:(NSInteger)maxArea
	{

	NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
	NSString* documentsDirectory = [paths objectAtIndex:0];

	vector<vector<cv::Point> > contours;
	vector<cv::Rect> boundRect;
	
	cv::findContours(inputMat, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
	
	std::vector<cv::Point> approx;
	cv::Rect appRect;
	for( int i = 0; i< contours.size(); i++ )
	{
		cv::approxPolyDP(cv::Mat(contours[i]), approx, 3, true); // arcLength(cv::Mat(contours[i]), true)*0.02
		appRect = boundingRect(cv::Mat(approx));
		if (self.DEBUG ) NSLog(@"--poi detect A: %d - W:%d H: %d",appRect.area(),appRect.width, appRect.height);
		if(appRect.area()>=minArea && appRect.area()<=maxArea) // && appRect.height<=200
		{
			if (self.DEBUG) NSLog(@"Area is OK");
			if (appRect.height>minHeight && appRect.width>minWidth)
			{
				if (self.DEBUG) NSLog(@"Size is OK");
				boundRect.push_back(appRect);
				if (self.DEBUG ) rectangle(Original, appRect.tl(), appRect.br(), Scalar(200,200,200),2);
			}
			if (self.DEBUG) NSLog(@"---------");
		}
	}

	NSString *filename = [NSString stringWithFormat:@"detectTextBoxes_%d.jpg",idx];
	NSString* imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
	if (self.DEBUG ) [UIImagePNGRepresentation(MatToUIImage(Original)) writeToFile:imagePath atomically:YES];
	
	return boundRect;
}

#endif

- (void)progressImageRecognitionForTesseract:(G8Tesseract *)tesseract {
	//NSLog(@"progress: %lu", (unsigned long)tesseract.progress);
}

- (BOOL)shouldCancelImageRecognitionForTesseract:(G8Tesseract *)tesseract {
	return NO;  // return YES, if you need to interrupt tesseract before it finishes
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(NSString *)slugify:(NSString *)string
{
	string = [string stringByReplacingOccurrencesOfString:@" "withString:@"-"];
	string = [string lowercaseString];
	NSMutableString *resultString = [NSMutableString stringWithCapacity:string.length];
	NSScanner *scanner = [NSScanner scannerWithString:string];
	NSCharacterSet *allowedChars = [NSCharacterSet characterSetWithCharactersInString:@"-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"];
	while ([scanner isAtEnd] == NO) {
		NSString *buffer;
		if ([scanner scanCharactersFromSet:allowedChars intoString:&buffer]) {
			[resultString appendString:buffer];
		} else {
			[scanner setScanLocation:([scanner scanLocation] + 1)];
		}
	}
	return resultString;
}

-(FirstScanData*)extractFirstScanData:(cv::Mat)inputMat
	bwimg:(cv::Mat)bwimg
	OriginalBW:(cv::Mat)OriginalBW
	OriginalBW_copy2:(cv::Mat)OriginalBW_copy2
	step:(NSString *)step
	tesseract:(G8Tesseract *)tesseract
	Otesseract:(G8Tesseract *)Otesseract
	Inv_Otesseract:(G8Tesseract *)Inv_Otesseract
{

	dilate(bwimg,bwimg,getStructuringElement(MORPH_RECT, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)13/100 animated:YES];
	});
	
	erode(bwimg, bwimg, getStructuringElement(MORPH_RECT, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)14/100 animated:YES];
	});
	/*
		filename = @"INV_erode_dilate.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(Inv_OriginalBW);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	*/
	GaussianBlur(bwimg, bwimg, cv::Size(5, 5), 5);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)15/100 animated:YES];
	});
	/*
		filename = @"INV_GaussianBlur5x5.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(Inv_OriginalBW);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	*/
	threshold(bwimg, bwimg, 127, 255, THRESH_BINARY);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)16/100 animated:YES];
	});
	
	UIImage* tmpuiimage;
	NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
	NSString* documentsDirectory = [paths objectAtIndex:0];

		NSString *filename = @"INV_threshold127.jpg";
		NSString *imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(bwimg);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)17/100 animated:YES];
	});

	Mat inputMat_morph = *new Mat();
	GaussianBlur(inputMat, inputMat_morph, cv::Size(5, 5), 5); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)18/100 animated:YES];
	});
	
	medianBlur(inputMat_morph, inputMat_morph, 5);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)19/100 animated:YES];
	});
/*
		filename = @"1-imageOcr_tresh_blur.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(inputMat_morph);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	*/
	threshold(inputMat_morph, inputMat_morph, 248, 255, THRESH_BINARY);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)20/100 animated:YES];
	});
/*
		filename = @"2-imageOcr_tresh_new.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(inputMat_morph);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	*/
	dilate(inputMat_morph, inputMat_morph, getStructuringElement(MORPH_RECT, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)21/100 animated:YES];
	});
	
		if (self.DEBUG ) {
			filename = @"3-imageOcr_tresh_new_dilate.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			tmpuiimage = MatToUIImage(inputMat_morph);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	
	cv::Rect prodottoTitleRect = *new cv::Rect();
	bool hasProdotto = false;
	cv::Rect articoloTitleRect = *new cv::Rect();
	bool hasArticolo = false;
	cv::Rect lottoTitleRect = *new cv::Rect();
	bool hasLotto = false;
	cv::Rect scadenzaTitleRect = *new cv::Rect();
	bool hasScadenza = false;
	
	Mat sobel = *new Mat();
	Mat thresh = *new Mat();
	
	Sobel(inputMat_morph,sobel,CV_8U, 1,0,3,1,0,BORDER_DEFAULT); // -5MB
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)22/100 animated:YES];
	});
	
	Sobel(sobel,sobel,CV_8U, 0,1,3,1,0,BORDER_DEFAULT);
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)23/100 animated:YES];
	});
	/*
		filename = @"11-sobel.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(sobel);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	*/
	erode(sobel, sobel, getStructuringElement(MORPH_ELLIPSE, cv::Size(3,3)));
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)24/100 animated:YES];
	});
/*
		filename = @"22-sobel_erode_3x3.jpg";
		imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
		if (self.DEBUG ) {
			tmpuiimage = MatToUIImage(sobel);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	*/
	if(sobel.cols>=1536)
	{
		dilate(sobel,thresh,getStructuringElement(MORPH_RECT, cv::Size(7,3)),cv::Point(-1,-1),9);
	}
	else
	{
		dilate(sobel,thresh,getStructuringElement(MORPH_RECT, cv::Size(5,3)),cv::Point(-1,-1),9);
	}
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)25/100 animated:YES];
	});
	
		if (self.DEBUG ) {
			filename = @"33-sobelxy_dilated.jpg";
			imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
			tmpuiimage = MatToUIImage(thresh);
			[UIImagePNGRepresentation(tmpuiimage) writeToFile:imagePath atomically:YES];
			tmpuiimage=nil;
		}
	
	vector<cv::Rect> textBoxesOthers = [self detectTextBoxes:thresh original:OriginalBW_copy2 idx:1 minWidth:self.minWidth minHeight:self.minHeight maxWidth:self.maxWidth maxHeight:self.maxHeight minArea:self.minArea maxArea:self.maxArea];
	
	dispatch_async(dispatch_get_main_queue(), ^{
		[self.progressView setProgress:(CGFloat)30/100 animated:YES];
	});

	double _step = (double)15/(double)textBoxesOthers.size();
	double current_progress=30;
	
	NSLog(@"Inizio la ricerca delle parole descrizione lotto e scadenza.");
	
	bool force_exit = false;
	int seach_count = 0;
	// primo ciclo: cerco le etichette delle colonne della tabella (codice articolo, nome prodotto, lotto e scadenza)
	cv::Rect roi;
    Mat thresh_dilate_cut = *new Mat();
	NSString* recognizedText = @"";
	NSInteger confidence = 0;
	NSString* OrecognizedText;
	NSInteger Oconfidence = 0;
	NSString* Inv_OrecognizedText;
	NSInteger Inv_Oconfidence = 0;
	NSString* stringToFind;
	NSString* OstringToFind;
	NSString* Inv_OstringToFind;
	cv::Rect leftColumn;

	tesseract.image = MatToUIImage(inputMat); // +10MB
	Otesseract.image = MatToUIImage(OriginalBW); //+10MB
	Inv_Otesseract.image = MatToUIImage(bwimg); //+10MB
	for (int i=0; ((i < textBoxesOthers.size()) && !force_exit ); i++)
	{
		if (prodottoTitleRect.area()==0 || scadenzaTitleRect.area()==0 || lottoTitleRect.area()==0 || articoloTitleRect.area()==0)
		{
			roi = textBoxesOthers[i];
			if (prodottoTitleRect.area()>0)
			{
				if(roi.y<(prodottoTitleRect.y-(prodottoTitleRect.height*2)))
				{
					force_exit = true;
				}
			}
			/*cropped = *new Mat(inputMat,roi);
				filename = [NSString stringWithFormat:@"search_%d.jpg",seach_count];
				imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
				UIImage *temp = MatToUIImage(cropped);
				NSData *tempimg = UIImageJPEGRepresentation(temp,100.0f);
				if (self.DEBUG ) [tempimg writeToFile:imagePath atomically:YES];
			
			Ocropped = *new Mat(OriginalBW,roi);
				filename = [NSString stringWithFormat:@"Osearch_%d.jpg",seach_count];
				imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
				if (self.DEBUG ) [UIImageJPEGRepresentation(MatToUIImage(Ocropped),100.0f) writeToFile:imagePath atomically:YES];

			Inv_Ocropped = *new Mat(Inv_OriginalBW,roi);
				filename = [NSString stringWithFormat:@"Inv_Osearch_%d.jpg",seach_count];
				imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
				if (self.DEBUG ) [UIImageJPEGRepresentation(MatToUIImage(Inv_Ocropped),100.0f) writeToFile:imagePath atomically:YES];
			*/
			//tesseract.image = MatToUIImage(cropped);
			
					if (self.DEBUG ) NSLog(@"roi (%d): %d,%d,%d,%d",seach_count,roi.x,roi.y,roi.width,roi.height);
			
			tesseract.rect = CGRectMake(roi.x, roi.y, roi.width, roi.height);
			[tesseract recognize];
			recognizedText = [tesseract recognizedText];
//			characterChoices = [tesseract recognizedBlocksByIteratorLevel:G8PageIteratorLevelTextline];
//			NSLog(@"characterChoices: %lu",(unsigned long)characterChoices.count);
			confidence = [tesseract meanConfidence];
					if (self.DEBUG ) NSLog(@"Only letters ROI confidence: %ld - OCR: %@",(long)confidence,recognizedText);
			
			//tesseract.image = MatToUIImage(Ocropped);
			Otesseract.rect = CGRectMake(roi.x, roi.y, roi.width, roi.height);
			[Otesseract recognize];
			OrecognizedText = [Otesseract recognizedText];
//			characterChoices = [Otesseract recognizedBlocksByIteratorLevel:G8PageIteratorLevelTextline];
//			NSLog(@"OcharacterChoices: %lu",(unsigned long)characterChoices.count);
			Oconfidence = [Otesseract meanConfidence];
					if (self.DEBUG ) NSLog(@"Only letters ROI Oconfidenc	e: %ld - OOCR: %@",(long)Oconfidence,OrecognizedText);
			
			//tesseract.image = MatToUIImage(Inv_Ocropped);
			Inv_Otesseract.rect = CGRectMake(roi.x, roi.y, roi.width, roi.height);
			[Inv_Otesseract recognize];
			Inv_OrecognizedText = [Inv_Otesseract recognizedText];
//			characterChoices = [Inv_Otesseract recognizedBlocksByIteratorLevel:G8PageIteratorLevelTextline];
//			NSLog(@"Inv_OcharacterChoices: %lu",(unsigned long)characterChoices.count);
			Inv_Oconfidence = [Inv_Otesseract meanConfidence];
					if (self.DEBUG ) NSLog(@"Only letters ROI Inv_Oconfidence: %ld - Inv_OOCR: %@",(long)Inv_Oconfidence,Inv_OrecognizedText);
			
//			characterChoices=nil;
			
			stringToFind = [self slugify:recognizedText];
			OstringToFind = [self slugify:OrecognizedText];
			Inv_OstringToFind = [self slugify:Inv_OrecognizedText];
			if (
				[stringToFind rangeOfString:@"descrizione"].location != NSNotFound ||
				[stringToFind rangeOfString:@"desc"].location != NSNotFound ||
				[stringToFind rangeOfString:@"des cri"].location != NSNotFound ||
				[@"descrizione"			similarity:stringToFind]>=0.6 ||
				[@"cod descrizione"		similarity:stringToFind]>=0.6 ||
				[@"descrizione articolo" similarity:stringToFind]>=0.6 ||
				[@"descri"				similarity:stringToFind]>=0.6 ||
				[@"doscriziono"			similarity:stringToFind]>=0.6 ||
				[@"doscrizione dei beni"			similarity:stringToFind]>=0.6 ||
				[@"doscrizione dei"			similarity:stringToFind]>=0.6
				)
			{
				prodottoTitleRect = roi;
				hasProdotto=true;
				rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Descrizione trovata!");
				
			}
			else if(
					[OstringToFind rangeOfString:@"descrizione"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"desc"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"des cri"].location != NSNotFound ||
					[@"descrizione"			similarity:OstringToFind]>=0.6 ||
					[@"cod descrizione"		similarity:OstringToFind]>=0.6 ||
					[@"descrizione articolo" similarity:OstringToFind]>=0.6 ||
					[@"descri"				similarity:OstringToFind]>=0.6 ||
					[@"doscriziono"			similarity:OstringToFind]>=0.6 ||
					[@"doscrizione dei beni"			similarity:OstringToFind]>=0.6 ||
					[@"doscrizione dei"			similarity:OstringToFind]>=0.6
					
				)
			{
				prodottoTitleRect = roi;
				hasProdotto=true;
				rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Descrizione trovata!");
				
			}
			else if(
					[Inv_OstringToFind rangeOfString:@"descrizione"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"desc"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"des cri"].location != NSNotFound ||
					[@"descrizione"			similarity:Inv_OstringToFind]>=0.6 ||
					[@"cod descrizione"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"descrizione articolo" similarity:Inv_OstringToFind]>=0.6 ||
					[@"descri"				similarity:Inv_OstringToFind]>=0.6 ||
					[@"doscriziono"			similarity:Inv_OstringToFind]>=0.6 ||
					[@"doscrizione dei beni"			similarity:Inv_OstringToFind]>=0.6 ||
					[@"doscrizione dei"			similarity:Inv_OstringToFind]>=0.6
					
					)
			{
				prodottoTitleRect = roi;
				hasProdotto=true;
				rectangle(OriginalBW_copy2, prodottoTitleRect.tl(), prodottoTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Descrizione trovata!");
				
			}
			else if(
					[stringToFind rangeOfString:@"codice articolo"].location != NSNotFound ||
					[@"codice articolo" similarity:stringToFind]>=0.6 ||
					[@"codicearticolo"	similarity:stringToFind]>=0.6 ||
					[@"articolo"		similarity:stringToFind]>=0.6 ||
					[@"cod."			similarity:stringToFind]>=0.7 ||
					[@"cod.art."		similarity:stringToFind]>=0.6 ||
					[@"dodi ari"		similarity:stringToFind]>=0.6 ||
					[@"cod--ari"		similarity:stringToFind]>=0.6 ||
					[@"codice"			similarity:stringToFind]>=0.6
					
					)
			{
				articoloTitleRect = roi;
				hasArticolo=true;
				rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Codice Articolo trovata!");
			}
			else if(
					[OstringToFind rangeOfString:@"codice articolo"].location != NSNotFound ||
					[@"codice articolo" similarity:OstringToFind]>=0.6 ||
					[@"codicearticolo"	similarity:OstringToFind]>=0.6 ||
					[@"articolo"		similarity:OstringToFind]>=0.6 ||
					[@"cod."			similarity:OstringToFind]>=0.7 ||
					[@"cod.art."		similarity:OstringToFind]>=0.6 ||
					[@"dodi ari"		similarity:OstringToFind]>=0.6 ||
					[@"cod--ari"		similarity:OstringToFind]>=0.6 ||
					[@"codice"			similarity:OstringToFind]>=0.6
					)
			{
				articoloTitleRect = roi;
				hasArticolo=true;
				rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Codice Articolo trovata!");
			}
			else if(
					[Inv_OstringToFind rangeOfString:@"codice articolo"].location != NSNotFound ||
					[@"codice articolo" similarity:Inv_OstringToFind]>=0.6 ||
					[@"codicearticolo"	similarity:Inv_OstringToFind]>=0.6 ||
					[@"articolo"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"cod."			similarity:Inv_OstringToFind]>=0.7 ||
					[@"cod.art."		similarity:Inv_OstringToFind]>=0.6 ||
					[@"dodi ari"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"cod--ari"		similarity:Inv_OstringToFind]>=0.6 ||
					[@"codice"			similarity:Inv_OstringToFind]>=0.6
					)
			{
				articoloTitleRect = roi;
				hasArticolo=true;
				rectangle(OriginalBW_copy2, articoloTitleRect.tl(), articoloTitleRect.br(), Scalar(100,100,100),3);
				NSLog(@"Colonna Codice Articolo trovata!");
			}
			else if (
					 [stringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:stringToFind]>=0.6 ||
					 [@"lono" similarity:stringToFind]>=0.6 ||
					 [@"loùo" similarity:stringToFind]>=0.6 ||
					 [@"loro" similarity:stringToFind]>=0.6
				)
			{
				if (
					[stringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[stringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[stringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[stringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:stringToFind]>=0.6 ||
					[@"seed" similarity:stringToFind]>=0.6 ||
					[@"sced" similarity:stringToFind]>=0.6 ||
					[@"sead" similarity:stringToFind]>=0.6 ||
					[@"scad." similarity:stringToFind]>=0.6 ||
					[@"scadenza" similarity:stringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					lottoTitleRect = roi;
					hasLotto=true;
					rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Lotto trovata!");
				}
			}
			else if (
					 [OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:OstringToFind]>=0.6 ||
					 [@"lono" similarity:OstringToFind]>=0.6 ||
					 [@"loùo" similarity:OstringToFind]>=0.6 ||
					 [@"loro" similarity:OstringToFind]>=0.6
					 )
			{
				if (
					[OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:OstringToFind]>=0.6 ||
					[@"seed" similarity:OstringToFind]>=0.6 ||
					[@"sced" similarity:OstringToFind]>=0.6 ||
					[@"sead" similarity:OstringToFind]>=0.6 ||
					[@"scad." similarity:OstringToFind]>=0.6 ||
					[@"scadenza" similarity:OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					lottoTitleRect = roi;
					hasLotto=true;
					rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Lotto trovata!");
				}
			}
			else if (
					 [Inv_OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"lono" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loùo" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loro" similarity:Inv_OstringToFind]>=0.6
					 )
			{
				if (
					[Inv_OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:Inv_OstringToFind]>=0.6 ||
					[@"seed" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sced" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sead" similarity:Inv_OstringToFind]>=0.6 ||
					[@"scad." similarity:Inv_OstringToFind]>=0.6 ||
					[@"scadenza" similarity:Inv_OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					lottoTitleRect = roi;
					hasLotto=true;
					rectangle(OriginalBW_copy2, lottoTitleRect.tl(), lottoTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Lotto trovata!");
				}
			}
			else if (
					[stringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[stringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[stringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[stringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[stringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:stringToFind]>=0.6 ||
					[@"seed" similarity:stringToFind]>=0.6 ||
					[@"sced" similarity:stringToFind]>=0.6 ||
					[@"sead" similarity:stringToFind]>=0.6 ||
					[@"scad." similarity:stringToFind]>=0.6 ||
					[@"scadenza" similarity:stringToFind]>=0.6
					 )
			{
				if (
					 [stringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [stringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:stringToFind]>=0.6 ||
					 [@"lono" similarity:stringToFind]>=0.6 ||
					 [@"loùo" similarity:stringToFind]>=0.6 ||
					 [@"loro" similarity:stringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					scadenzaTitleRect = roi;
					hasScadenza=true;
					rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Scadenza trovata!");
				}
			}
			else if (
					[OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:OstringToFind]>=0.6 ||
					[@"seed" similarity:OstringToFind]>=0.6 ||
					[@"sced" similarity:OstringToFind]>=0.6 ||
					[@"sead" similarity:OstringToFind]>=0.6 ||
					[@"scad." similarity:OstringToFind]>=0.6 ||
					[@"scadenza" similarity:OstringToFind]>=0.6
					 )
			{
				if (
					 [OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:OstringToFind]>=0.6 ||
					 [@"lono" similarity:OstringToFind]>=0.6 ||
					 [@"loùo" similarity:OstringToFind]>=0.6 ||
					 [@"loro" similarity:OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					scadenzaTitleRect = roi;
					hasScadenza=true;
					rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Scadenza trovata!");
				}
			}
			else if (
					[Inv_OstringToFind rangeOfString:@"scad"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"seed"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sced"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"sead"].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scad."].location != NSNotFound ||
					[Inv_OstringToFind rangeOfString:@"scadenza"].location != NSNotFound ||
					[@"scad" similarity:Inv_OstringToFind]>=0.6 ||
					[@"seed" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sced" similarity:Inv_OstringToFind]>=0.6 ||
					[@"sead" similarity:Inv_OstringToFind]>=0.6 ||
					[@"scad." similarity:Inv_OstringToFind]>=0.6 ||
					[@"scadenza" similarity:Inv_OstringToFind]>=0.6
					 )
			{
				if (
					 [Inv_OstringToFind rangeOfString:@"lotto"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"lono"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loùo"].location != NSNotFound ||
					 [Inv_OstringToFind rangeOfString:@"loro"].location != NSNotFound ||
					 [@"lotto" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"lono" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loùo" similarity:Inv_OstringToFind]>=0.6 ||
					 [@"loro" similarity:Inv_OstringToFind]>=0.6
					)
				{
					NSLog(@"Campi Lotto e Scadenza trovati!");
				}
				else
				{
					scadenzaTitleRect = roi;
					hasScadenza=true;
					rectangle(OriginalBW_copy2, scadenzaTitleRect.tl(), scadenzaTitleRect.br(), Scalar(100,100,100),3);
					NSLog(@"Colonna Scadenza trovata!");
				}
			}
			current_progress+=_step;
			self.progressView.progress = current_progress/100.0f;
			dispatch_async(dispatch_get_main_queue(), ^{
				[self.progressView setProgress:(CGFloat)current_progress/100 animated:YES];
			});
			
		}
		seach_count++;
	}

	FirstScanData *f = [[FirstScanData alloc] init];
	[f setProdottoTitleTect:prodottoTitleRect];
	[f setArticoloTitleTect:articoloTitleRect];
	[f setLottoTitleTect:lottoTitleRect];
	[f setScadenzaTitleTect:scadenzaTitleRect];
	[f setTextBoxesOthers:textBoxesOthers];
	[f setSobel:sobel];
	[f setThresh:thresh];
	[f setInputMat_morph:inputMat_morph];
	
	return f;

}
@end



/*
-(void) photoCamera:(CvPhotoCamera*)photoCamera capturedImage:(UIImage *)image {

    self.progressView.progress = 1/10.0f;
 
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil);
    });
 
    NSDateComponents *components = [[NSCalendar currentCalendar] components:NSCalendarUnitDay | NSCalendarUnitMonth | NSCalendarUnitYear fromDate:[NSDate date]];
 
    NSInteger day = [components day];
    NSInteger month = [components month];
    NSInteger year = [components year];
    NSInteger hour = [components hour];
    NSInteger minute = [components minute];
    NSInteger second = [components second];
 
    NSString *currentDatetime = [NSString stringWithFormat:@"%ld-%ld-%ld_%ld-%ld-%ld", (long)year, (long)month, (long)day, (long)hour, (long)minute, (long)second];
 
    // Get a file path to save the JPEG
    NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString* documentsDirectory = [paths objectAtIndex:0];
    NSString* filename = [NSString stringWithFormat:@"sample_picture_%@.jpg",currentDatetime];
    
    NSString* imagePath = [documentsDirectory stringByAppendingPathComponent:filename];
    
    // Get the image data (blocking; around 1 second)
    NSData* imageData = UIImageJPEGRepresentation(image, 1.0);
    
    // Write the data to the file
    [imageData writeToFile:imagePath atomically:YES];
    self.progressView.progress = 2/10.0f;


    // 1) devo scalare la posizione dei punti
    for(int i=0; i<corners.size();i++)
    {
        corners[i].x *= image.size.width/320;
        corners[i].y *= image.size.height/480;
    }
    self.progressView.progress = 3/10.0f;
    Mat inputMat = Mat(image.size.height,image.size.width,CV_8UC3);
    Mat quad = Mat(image.size.height,image.size.width,CV_8UC3);
    Mat src_mat = Mat(corners,CV_32FC2);
    self.progressView.progress = 4/10.0f;
    
    std::vector<cv::Point2f> quad_pts;
    quad_pts.push_back(Point2f(0,0));
    quad_pts.push_back(Point2f(quad.cols,0));
    quad_pts.push_back(Point2f(quad.cols,quad.rows));
    quad_pts.push_back(Point2f(0,quad.rows));
    Mat dst_mat = Mat(quad_pts,CV_32FC2);
    self.progressView.progress = 5/10.0f;
    
    Mat transmtx = getPerspectiveTransform(src_mat,dst_mat);
    self.progressView.progress = 6/10.0f;
    warpPerspective(inputMat, quad, transmtx, quad.size());
    self.progressView.progress = 8/10.0f;
    transmtx.release();
    src_mat.release();
    dst_mat.release();
    inputMat.release();
    
    // salvo l'immagine
    filename = [NSString stringWithFormat:@"resultImage_%@.jpg",currentDatetime];
    NSString* imageFile = [documentsDirectory stringByAppendingPathComponent:filename];
    // Get the image data (blocking; around 1 second)
    imageData = UIImageJPEGRepresentation([UIImage imageWithCVMat:quad], 1.0);
    
    // Write the data to the file
    [imageData writeToFile:imageFile atomically:YES];
    self.progressView.progress = 9/10.0f;
    
    // ritaglio l'header
    cv::Rect roi(0,0,quad.cols,(int)quad.rows*0.2);
    Mat cropped(quad,roi);
    
    // salvo l'header
    filename = [NSString stringWithFormat:@"resultHeader_%@.jpg",currentDatetime];
    NSString* headerFile = [documentsDirectory stringByAppendingPathComponent:filename];
    // Get the image data (blocking; around 1 second)
    imageData = UIImageJPEGRepresentation([UIImage imageWithCVMat:cropped], 1.0);
    
    // Write the data to the file
    [imageData writeToFile:headerFile atomically:YES];
    self.progressView.progress = 10/10.0f;
    
    // chiudo
    imageData=nil;
    cropped.release();
    quad.release();
 // devo restituire imageFile e headerFile al plugin
 //    [self.plugin capturedImageWithPath:imagePath imageFile:imageFile headerFile:headerFile ];
 
    
    [self.plugin capturedImageWithPath:imagePath imageFile:@"" headerFile:@"" ];
    self.progressView.progress = 10/10.0f;
    NSLog(@"All done. ");
}
*/


/*
 //_lastFrame = image;
 
 NSLog(@"Resizing to 320x480...");
 resize(image, image, cv::Size(320,480));
 NSLog(@"done.");
 rows = image.rows;
 cols = image.cols;
 
 vector<Vec4i> lines; // = *new Mat();
 int threshold = 70;
 int minLineSize = 30;
 int lineGap = 10;
 int cornerRadius = 100;
 
 Mat gray = *new Mat();
 
 NSLog(@"Convert the image to RGBA... ");
 cvtColor(image, image, CV_BGRA2RGBA);
 
 NSLog(@"Getting the image as grayscale... ");
 cvtColor(image, gray, CV_RGBA2GRAY);
 
 NSLog(@"Apply Gaussian filter to remove small edges... ");
 GaussianBlur(gray, gray, cv::Size(5, 5), 1.2, 1.2);
 
 NSLog(@"Apply Canny filter to detect edges... ");
 Canny(gray, gray, 100, 100, 3);//0, 50);
 
 NSLog(@"Apply HoughLinesP filter to detect border lines... ");
 HoughLinesP(gray, lines, 1 , M_PI /180, threshold, minLineSize, lineGap);
 NSLog(@"done.");
 
 cv::Point2f ULCorner = cv::Point(0,0);
 cv::Point2f URCorner = cv::Point(cols,0);
 cv::Point2f LRCorner = cv::Point(cols,rows);
 cv::Point2f LLCorner = cv::Point(0,rows);
 
 circle(image, ULCorner, cornerRadius, Scalar(0, 127, 127, 0));
 circle(image, LLCorner, cornerRadius, Scalar(0, 127, 127, 0));
 circle(image, URCorner, cornerRadius, Scalar(0, 127, 127, 0));
 circle(image, LRCorner, cornerRadius, Scalar(0, 127, 127, 0));
 
 NSLog(@"Extending lines... ");
 for(int i=0; i < lines.size(); i++)
 {
 Vec4i v = lines[i];
 lines[i][0] = 0;
 lines[i][1] = ((float)v[1] - v[3]) / (v[0] - v[2]) * -v[0] + v[1];
 lines[i][2] = cols;
 lines[i][3] = ((float)v[1] - v[3]) / (v[0] - v[2]) * (cols - v[2]) + v[3];
 v = nil;
 }
 
 cv::Point2f cornerUL(0,0);
 cv::Point2f cornerLL(0,0);
 cv::Point2f cornerUR(0,0);// = *new cv::Point2f();
 cv::Point2f cornerLR(0,0);// = *new cv::Point2f();
 
 int countUL = 0;
 int countLL = 0;
 int countUR = 0;
 int countLR = 0;
 
 NSLog(@"Finding lines intersections... ");
 for (int i = 0; i < lines.size(); i++)
 {
 //cv::line(image,cv::Point(lines[i][0],lines[i][1]),cv::Point(lines[i][2],lines[i][3]), Scalar(255,255,0),3);
 for (int j = i+1; j < lines.size(); j++)
 {
 cv::Point2f pt = computeIntersect(lines[i], lines[j]);
 if (pt.x >= 0 && pt.y >= 0)
 {
 double distanceUL = cv::norm(pt-ULCorner);
 double distanceLL = cv::norm(pt-LLCorner);
 double distanceUR = cv::norm(pt-URCorner);
 double distanceLR = cv::norm(pt-LRCorner);
 
 if(distanceUL<cornerRadius)
 {
 countUL++;
 cornerUL += pt;
 //circle(image, pt, 15, Scalar(0, 0, 255, 0));
 }
 if(distanceLL<cornerRadius)
 {
 countLL++;
 cornerLL += pt;
 //circle(image, pt, 15, Scalar(0, 0, 255, 0));
 }
 if(distanceUR<cornerRadius)
 {
 countUR++;
 cornerUR += pt;
 //circle(image, pt, 15, Scalar(0, 0, 255, 0));
 }
 if(distanceLR<cornerRadius)
 {
 countLR++;
 cornerLR += pt;
 //circle(image, pt, 15, Scalar(0, 0, 255, 0));
 }
 
 
 }
 }
 }
 NSLog(@"Getting corners by finding center of mass in group of points... ");
 corners.clear();
 if (countLR>0)
 {
 cornerUL.x *= (1. / countUL);
 cornerUL.y *= (1. / countUL);
 corners.push_back(cornerUL);
 circle(image, cornerUL, 15, Scalar(255, 255, 0));
 }
 if (countLL>0)
 {
 cornerLL.x *= (1. / countLL);
 cornerLL.y *= (1. / countLL);
 corners.push_back(cornerLL);
 circle(image, cornerLL, 15, Scalar(255, 255, 0));
 }
 if (countLR>0)
 {
 cornerLR.x *= (1. / countLR);
 cornerLR.y *= (1. / countLR);
 corners.push_back(cornerLR);
 circle(image, cornerLR, 15, Scalar(255, 255, 0));
 }
 if (countUR>0)
 {
 cornerUR.x *= (1. / countUR);
 cornerUR.y *= (1. / countUR);
 corners.push_back(cornerUR);
 circle(image, cornerUR, 15, Scalar(255, 255, 0));
 }
 if (corners.size()==4)
 {
 NSLog(@"If we have 4 corners connect them with lines... ");
 
 [self showButton];
 
 cv::line(image, cornerUL, cornerUR, Scalar(255,255,0), 3);
 cv::line(image, cornerUR, cornerLR, Scalar(255,255,0), 3);
 cv::line(image, cornerLR, cornerLL, Scalar(255,255,0), 3);
 cv::line(image, cornerLL, cornerUL, Scalar(255,255,0), 3);
 }
 else
 {
 NSLog(@"Not enough corners... ");
 [self.button performSelectorOnMainThread:@selector(hideButton) withObject:nil waitUntilDone:NO];
 
 }
 
 
 
 @autoreleasepool {
 [self.resultView performSelectorOnMainThread:@selector(setImage:) withObject:[UIImage imageWithCVMat:image] waitUntilDone:YES];
 }
 */
