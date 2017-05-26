//
//  NSString+Similarity.h
//  Cimino
//
//  Created by Alessandro Randazzo on 30/11/15.
//
//

#import <Foundation/Foundation.h>

@interface NSString (Similarity)

// determinal la similitudine (o vicinnaza) di due stringhe, restituisce un numero tra 0 e 1.
- (CGFloat) similarity: (NSString *) stringB;

@end