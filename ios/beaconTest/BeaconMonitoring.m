//
//  BeaconMonitoring.m
//  beaconTest
//
//  Created by Alex Zhukov on 2/13/20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "React/RCTBridgeModule.h"

#import "React/RCTEventEmitter.h"

@interface RCT_EXTERN_REMAP_MODULE(RNBeaconMonitoring, BeaconMonitoring, RCTEventEmitter)
RCT_EXTERN_METHOD(startMonitoring: (NSDictionary *) dict)
RCT_EXTERN_METHOD(stopMonitoring)
@end
