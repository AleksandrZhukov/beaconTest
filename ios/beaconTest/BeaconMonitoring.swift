//
//  BeaconMonitoring.swift
//  beaconTest
//
//  Created by Alex Zhukov on 2/13/20.
//  Copyright © 2020 Facebook. All rights reserved.
//

import Foundation
import CoreLocation
import UserNotifications

@objc(BeaconMonitoring)
class BeaconMonitoring: RCTEventEmitter, CLLocationManagerDelegate {
  let locationManager = CLLocationManager()
  var beaconRegion: CLBeaconRegion!
  
  @objc
  func startMonitoring(_ params: [String: Any]) {
    print("startMonitoring", params )
    locationManager.allowsBackgroundLocationUpdates = true
    locationManager.delegate = self
    locationManager.requestAlwaysAuthorization()
    
    let uuid = UUID(uuidString: params["uuid"] as! String)!
    beaconRegion = CLBeaconRegion(proximityUUID: uuid, major: params["major"] as! CLBeaconMajorValue, minor: params["minor"] as! CLBeaconMinorValue, identifier: uuid.uuidString)
    
    locationManager.startMonitoring(for: beaconRegion)
  }
  
  @objc
  func stopMonitoring() {
    print("stopMonitoring" )
    locationManager.stopMonitoring(for: beaconRegion)
  }
  
  func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
    switch status {
    case .authorizedAlways:
      if !locationManager.monitoredRegions.contains(beaconRegion) {
        locationManager.startMonitoring(for: beaconRegion)
      }
    case .authorizedWhenInUse:
      if !locationManager.monitoredRegions.contains(beaconRegion) {
        locationManager.startMonitoring(for: beaconRegion)
      }
    default:
      print("authorisation not granted")
    }
  }
  
  func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for region: CLRegion) {
    print("Did determine state for region \(region) \(state)")
    sendEvent(withName: "didDetermineState", body:  keyForBeacon(region: region as! CLBeaconRegion))
  }
  
  func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
    print("Did start monitoring region: \(region)\n")
    
  }
  func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
    locationManager.startRangingBeacons(in: beaconRegion)
    print("didEnter \(region)\n")
    sendEvent(withName: "didEnter", body:  keyForBeacon(region: region as! CLBeaconRegion))
  }
  
  func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
    locationManager.stopRangingBeacons(in: beaconRegion)
    print("didExit \(region)\n")
    sendEvent(withName: "didExit", body:  keyForBeacon(region: region as! CLBeaconRegion ))
  }
  
  func keyForBeacon(region: CLBeaconRegion) -> [String: Any] {
    if #available(iOS 13.0, *) {
      return ["uuid": region.uuid.uuidString, "major": region.major!, "minor": region.minor!]
    } else {
      return ["uuid": region.proximityUUID.uuidString, "major": region.major!, "minor": region.minor!]
    }
  }
  
  override func supportedEvents() -> [String]! {
    return ["didDetermineState", "didEnter", "didExit"]
  }
}
