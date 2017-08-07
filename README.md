# phonetodesktop

This project is licensed under the Apache 2.0 license.

This is the open source project for Phone To Desktop android app (https://play.google.com/store/apps/details?id=net.xisberto.phonetodesktop).

The app creates and manages a task list on [Google Tasks](https://developers.google.com/google-apps/tasks/v1/reference/) to insert on it texts shared to the app.
 
## Google API access

In order to run the app, you must create your own project on the [Google API Console](https://console.developers.google.com/apis/dashboard) and set the name of your created project on [the Utils class](https://github.com/xisberto/phonetodesktop/blob/master/phonetodesktop/src/main/java/net/xisberto/phonetodesktop/Utils.java#L125).

You will have to create a new Android app credential and associate it to your signed APK. Follow the instructions provided on the Google API Console to obtain this credential.
