# location-service-live-data

## Background:

In Android apps, users need to allow specific permissions so that app can use that particular resource. All location tracking apps need “GPS to be enabled” and want users to allow “Location Permission” so that app can start receiving location coordinates. 

## Motivation to publish my solution:

There are already native android ways to implement both of these checks but I tried to do it using LiveData which is a newly introduced API from Android to handle asynchronous operations using Subscribe/Publish mechanism. I found it quite challenging and there was limited help available as not many have tried doing it. Solutions which are challenging are the best candidate to be shared with public because it means many others out there will face the same problems and it could save their time. At the end of the day, it’s basically giving back to the community. 

## What is this project about:

I tried to show implementation of GPS and Location Permission checks using LiveData. 

1) UI (app is active): Checking for both elements, displaying state and showing dialogs for user to react accordingly

    Medium Article: [Read Here](https://medium.com/ki-labs-engineering/monitoring-gps-and-location-permission-checks-using-livedata-part-1-278907344b77)

2) Background (app is not active): Check for both elements as part of Service whenever it is started by System and inform user via notification so that user can enable them for us. Otherwise, location tracking won’t succeed at all.

    Medium Article: [Read Here](https://medium.com/ki-labs-engineering/monitoring-gps-and-location-permission-checks-using-livedata-part-2-d8822ab951a6)
