//
//  MyTesseractOCR.m
//  Cimino
//
//  Created by Alessandro Randazzo on 29/11/15.
//
//

#import "G8Tesseract+meanConfidence.h"
#import <G8RecognizedBlock.h>

@implementation G8Tesseract (MyTesseractOCR)

-(NSInteger)meanConfidence
{
	CGFloat final_confidence = 0.0f;
	NSArray *choices;
	G8RecognizedBlock *choiceBlock;
	int i,j;
	for (i=0; i<[self characterChoices].count;i++)
	{
		if ([[self characterChoices][i] class] == [NSArray class])
		{
			choices = (NSArray*)[self characterChoices][i];
			for (j=0; j<choices.count;j++)
			{
				choiceBlock =(G8RecognizedBlock*)choices[j];
				final_confidence+=choiceBlock.confidence;
			}
		}
		else if ([[self characterChoices][i] class] == [G8RecognizedBlock class])
		{
			final_confidence+=((G8RecognizedBlock*)[self characterChoices][i]).confidence;
		}
	}
	return (NSInteger)(final_confidence/[self characterChoices].count);
}

/*
-(NSInteger) meanConfidence
{
	return _tesseract->MeanTextConf();
}
*/

@end
