/*
 * Copyright (c) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.services.samples.tasks.android;

import java.io.IOException;

import net.xisberto.phonetodesktop.GoogleTasksActivity;
import net.xisberto.phonetodesktop.SyncActivity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

/**
 * Asynchronous task that also takes care of common needs, such as displaying progress,
 * authorization, exception handling, and notifying UI when operation succeeded.
 * 
 * @author Yaniv Inbar
 */
public abstract class CommonAsyncTask extends AsyncTask<Void, Void, Boolean> {

  final protected SyncActivity activity;
  final protected int request;

  protected CommonAsyncTask(SyncActivity activity, int request) {
    this.activity = activity;
    this.request = request;
  }
  
  public int getRequest() {
	  return request;
  }

  @Override
  protected final Boolean doInBackground(Void... params) {
	  //TODO use this params
    try {
      doInBackground();
      return true;
    } catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
      activity.showGooglePlayServicesAvailabilityErrorDialog(
          availabilityException.getConnectionStatusCode());
    } catch (UserRecoverableAuthIOException userRecoverableException) {
      activity.startActivityForResult(
          userRecoverableException.getIntent(), GoogleTasksActivity.REQUEST_AUTHORIZATION);
    } catch (IOException e) {
      Log.e(GoogleTasksActivity.TAG, e.getLocalizedMessage());
    }
    return false;
  }

  @Override
  protected final void onPostExecute(Boolean success) {
    super.onPostExecute(success);
    if (success) {
      activity.refreshView();
    }
  }

  abstract protected void doInBackground() throws IOException;
}
