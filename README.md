# Monitoring GPS and Location Permission checks using LiveData

## Background:

In Android apps, users need to allow specific permissions so that app can use that particular resource. All location tracking apps need “GPS to be enabled” and want users to allow “Location Permission” so that app can start receiving location coordinates. For our usecase, it was critically needed to ensure both of these requirements are fulfilled. If not, user should be alerted and informed on UI level during or after *Onboarding* and also when Location Tracking is started/running in background. 

This is a sample app to demonstrate the usecase. 

## Motivation to publish my solution:

There are already native android ways to implement both of these checks but we tried to do it using LiveData which is a newly introduced API from Android to handle asynchronous operations using Subscribe/Publish mechanism. We faced our fair share of challenges and there was limited help available as not many have tried for this usecase. Solutions which are challenging are the best candidate to be shared with public because it means many others out there will face the same problems and it could save their time. At the end of the day, it’s basically giving back to the community. 

## What is this project about:

1) UI (app is active): Checking for both elements, displaying state and showing dialogs for user to react accordingly

    Medium Article: [Read Here](https://medium.com/ki-labs-engineering/monitoring-gps-and-location-permission-checks-using-livedata-part-1-278907344b77)

2) Background (app is not active): Check for both elements as part of Service whenever it is started by System and inform user via notification so that user can enable them for us. Otherwise, location tracking won’t succeed at all.

    Medium Article: [Read Here](https://medium.com/ki-labs-engineering/monitoring-gps-and-location-permission-checks-using-livedata-part-2-d8822ab951a6)

## How sample app looks like or behaves

<br>

<img src="https://user-images.githubusercontent.com/273389/47611112-c152a500-da5e-11e8-85dd-73dbd1cd1e7c.png" width="200" height="350" align="middle"> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; 
<img src="https://user-images.githubusercontent.com/273389/47611114-cb74a380-da5e-11e8-8a78-18b315703620.png" width="200" height="350" align="middle"> 
&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; 
<img src="https://user-images.githubusercontent.com/273389/47611116-d2031b00-da5e-11e8-8875-3c9fb9d6bfc2.png" width="200" height="350" align="middle">

<br><br>

<img src="https://user-images.githubusercontent.com/273389/47611109-b13ac580-da5e-11e8-8464-930819aa476c.png" width="200" height="200" align="middle"> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; 
<img src="https://media.giphy.com/media/xWfplzYLnWSF3zivrm/giphy.gif" width="200" height="350" align="middle"> 
&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; 
<img src="https://media.giphy.com/media/1ynT3NyWY0daNA8XEk/giphy.gif" width="200" height="350" align="middle">



