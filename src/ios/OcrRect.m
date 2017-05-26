//
//  OcrRect.m
//  Cimino
//
//  Created by Alessandro Randazzo on 01/12/15.
//
//
#import "OcrRect.h"

@implementation OcrRect

-(cv::Rect)getArea{
	return area;
}

-(void)setArea:(cv::Rect)_area{
	area = _area;
}

-(NSString*)getText{
	return text;
}

-(void)setText:(NSString*)_text{
	text = _text;
}

-(NSInteger)getConfidence{
	return confidence;
}

-(void)setConfidence:(NSInteger)_confidence{
	confidence = _confidence;
}



@end
