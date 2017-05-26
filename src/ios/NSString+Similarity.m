	//
//  NSString+Similarity.m
//  Cimino
//
//  Created by Alessandro Randazzo on 30/11/15.
//
//

#import "NSString+Similarity.h"

@implementation NSString (Similarity)

- (CGFloat) similarity: (NSString *) stringB
{
	NSString* longer = self;
	NSString* shorter = stringB;
	
	if(self.length<stringB.length)
	{
		longer = stringB;
		shorter = self;
	}
	NSInteger longerLength = longer.length;
	
	if(longerLength==0 || shorter.length==0)
	{
		return 0.0;//1.0; /* both strings are zero length */
	}
	NSInteger distance = [longer editDistance:shorter];
	
	CGFloat result = ((longerLength - distance )/ (CGFloat)longerLength);

	//NSLog(@"Score of '%@' against '%@': %f",self, stringB, result);

	longer=nil;
	shorter=nil;
	longerLength=nil;

	return result;
//    return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
}

- (NSInteger) editDistance:(NSString*)s2
{
	NSString * s1 = [NSString stringWithString: self];
	s1 = [s1 lowercaseString];
	s2 = [s2 lowercaseString];

	NSInteger i, j, *costs, distance = 0, lastValue;
	
	NSUInteger n = [s1 length];
	NSUInteger m = [s2 length];
	
	if( n > 0 && m > 0 )
	{
		costs = (NSInteger*) malloc( sizeof(NSInteger) * (m+1) );
		for(i=0; i<=n; i++)
		{
			lastValue = i;
			for (j=0;j<=m;j++)
			{
				if(i==0)
					costs[j] = j;
				else {
					if(j>0)
					{
						NSInteger newValue = costs[j-1];
						NSString *s1char = [s1 substringWithRange:NSMakeRange(i-1,1)] ;
						NSString *s2char = [s2 substringWithRange:NSMakeRange(j-1,1)] ;
						if( ![s1char isEqualToString:s2char] )
						{
							newValue = MIN(MIN(newValue, lastValue),costs[j])+1;
						}
						costs[j-1] = lastValue;
						lastValue = newValue;
						newValue=nil;
					}
				}
			}
			if(i>0)
				costs[m]=lastValue;
			
		}
		distance = costs[m];
	}
	i=nil;
	j=nil;
	n=nil;
	m=nil;
	costs=nil;
	lastValue=nil;
	return distance;
}
@end