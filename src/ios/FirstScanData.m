//
//  FirstScanData.m
//  Cimino
//
//  Created by Alessandro Randazzo on 13/07/16.
//
//

#import "FirstScanData.h"

@implementation FirstScanData

-(cv::Rect)getProdottoTitleRect{
	return prodottoTitleRect;
}

-(void)setProdottoTitleTect:(cv::Rect)_prodottoTitleRect{
	prodottoTitleRect = _prodottoTitleRect;
}

-(cv::Rect)getArticoloTitleRect{
	return articoloTitleRect;
}

-(void)setArticoloTitleTect:(cv::Rect)_articoloTitleRect{
	articoloTitleRect = _articoloTitleRect;
}

-(cv::Rect)getLottoTitleRect{
	return lottoTitleRect;
}

-(void)setLottoTitleTect:(cv::Rect)_lottoTitleRect{
	lottoTitleRect = _lottoTitleRect;
}

-(cv::Rect)getScadenzaTitleRect{
	return scadenzaTitleRect;
}

-(void)setScadenzaTitleTect:(cv::Rect)_scadenzaTitleRect{
	scadenzaTitleRect = _scadenzaTitleRect;
}

-(cv::vector<cv::Rect> )getTextBoxesOthers{
	return textBoxesOthers;
}

-(void)setTextBoxesOthers:(cv::vector<cv::Rect> )_textBoxesOthers{
	textBoxesOthers = _textBoxesOthers;
}

-(cv::Mat)getSobel{
	return sobel;
}

-(void)setSobel:(cv::Mat)_sobel{
	sobel = _sobel;
}

-(cv::Mat)getThresh{
	return thresh;
}

-(void)setThresh:(cv::Mat)_thresh{
	thresh = _thresh;
}

-(cv::Mat)getInputMat_morph{
	return inputMat_morph;
}

-(void)setInputMat_morph:(cv::Mat)_inputMat_morph{
	inputMat_morph = _inputMat_morph;
}


@end
