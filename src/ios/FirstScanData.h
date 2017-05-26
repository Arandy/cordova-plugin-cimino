//
//  FirstScanData.h
//  Cimino
//
//  Created by Alessandro Randazzo on 13/07/16.
//
//

#import <Foundation/Foundation.h>

@interface FirstScanData : NSObject{
	cv::Rect prodottoTitleRect;
	cv::Rect articoloTitleRect;
	cv::Rect lottoTitleRect;
	cv::Rect scadenzaTitleRect;
	cv::vector<cv::Rect> textBoxesOthers;
	cv::Mat sobel;
	cv::Mat thresh;
	cv::Mat inputMat_morph;
}

-(cv::Rect)getProdottoTitleRect;
-(void)setProdottoTitleTect:(cv::Rect)_prodottoTitleRect;
-(cv::Rect)getArticoloTitleRect;
-(void)setArticoloTitleTect:(cv::Rect)_articoloTitleRect;
-(cv::Rect)getLottoTitleRect;
-(void)setLottoTitleTect:(cv::Rect)_lottoTitleRect;
-(cv::Rect)getScadenzaTitleRect;
-(void)setScadenzaTitleTect:(cv::Rect)_scadenzaTitleRect;
-(cv::vector<cv::Rect> )getTextBoxesOthers;
-(void)setTextBoxesOthers:(cv::vector<cv::Rect> )_textBoxesOthers;
-(cv::Mat)getSobel;
-(void)setSobel:(cv::Mat)_sobel;
-(cv::Mat)getThresh;
-(void)setThresh:(cv::Mat)_thresh;
-(cv::Mat)getInputMat_morph;
-(void)setInputMat_morph:(cv::Mat)_inputMat_morph;

@end
