# NoTrustIssues

Status: barely working (under the right conditions)

This Android application allows making internal phone sensors available to other devices via Bluetooth Low Energy.
While running this application, your phone acts like Bluetooth sensor:
1. Announcing it's presence _continuously_ via Bluetooth to all other devices in range (incl. device name) 
2. If another device connects and subscribes for data changes, it will receive updates as long (a) NoTrustIssues is running and (b) the devices remain in Bluetooth range. 

ATTENTION: Enabling NoTrustIssues may compromise _your privacy_ as it literally turns your phone into a Bluetooth beacon. 

Support sensors:
* Barometer: exposes the measured air pressure as Environmental Sensing service