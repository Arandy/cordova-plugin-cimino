//
//  OcrRect.h
//  Cimino
//
//  Created by Alessandro Randazzo on 01/12/15.
//
//

#import <Foundation/Foundation.h>

@interface OcrRect : NSObject{
	cv::Rect area;
	NSString *text;
	NSInteger confidence;
}

-(cv::Rect)getArea;
-(void)setArea:(cv::Rect)_area;
-(NSString*)getText;
-(void)setText:(NSString*)_text;
-(NSInteger)getConfidence;
-(void)setConfidence:(NSInteger)_confidence;

@end
