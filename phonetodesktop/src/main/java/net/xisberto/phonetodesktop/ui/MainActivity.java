/*******************************************************************************
 * Copyright (c) 2012 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p/>
 * Contributors:
 * Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop.ui;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.network.GoogleTasksSpiceService;
import net.xisberto.phonetodesktop.network.TasksListRequest;

public class MainActivity extends AppCompatActivity implements
        MainFragment.PhoneToDesktopAuthorization {

    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    public static final int REQUEST_AUTHORIZATION = 1;
    public static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int REQUEST_PERMISSION = 3;

    private static final String TAG_MAIN = "mainFragment";

    private GoogleAccountCredential credential;
    public Preferences preferences;

    private MainFragment mainFragment;
    private Fragment currentFragment;

    private boolean showWelcome;

    private SpiceManager mSpiceManager = new SpiceManager(GoogleTasksSpiceService.class);

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Utils.EXTRA_UPDATING)) {
                updateMainLayout(intent.getBooleanExtra(Utils.EXTRA_UPDATING, false));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        preferences = Preferences.getInstance(this);

        credential = GoogleAccountCredential.usingOAuth2(this, Utils.SCOPES);
        credential.setSelectedAccountName(preferences.loadAccountName());

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();
            if (preferences.loadAccountName() == null) {
                showWelcome = true;
                currentFragment = WelcomeFragment.newInstance();
                transaction.replace(R.id.main_frame, currentFragment);
            } else {
                showWelcome = false;
                mainFragment = MainFragment.newInstance();
                transaction.replace(R.id.main_frame, mainFragment, TAG_MAIN);
            }
            transaction.commit();
        } else {
            mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag(
                    TAG_MAIN);
        }

        checkActionAuth(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkActionAuth(intent);
    }

    private void checkActionAuth(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Utils.ACTION_AUTHENTICATE)) {
            startAuthorization();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                Utils.log("Result from Account Picker");
                if (resultCode == RESULT_OK && data != null
                        && data.hasExtra(AccountManager.KEY_ACCOUNT_NAME)) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    Utils.log("Saving account " + accountName);
                    saveAccountName(accountName);
                } else {
                    updateMainLayout(false);
                }
                break;

            case REQUEST_GOOGLE_PLAY_SERVICES:
                Utils.log("Result from Play Services error");
                startAuthorization();
                break;
            case REQUEST_AUTHORIZATION:
                Utils.log("Result from Authorization");
                if (resultCode == RESULT_OK) {
                    Utils.log("starting saveListId");
                    updateMainLayout(true);
                    saveListId();
                } else {
                    updateMainLayout(false);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (REQUEST_PERMISSION == requestCode) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveAccountName(preferences.loadAccountName());
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(Utils.ACTION_AUTHENTICATE));

        if (!showWelcome && currentFragment instanceof WelcomeFragment) {
            mainFragment = (MainFragment) getSupportFragmentManager()
                    .findFragmentByTag(TAG_MAIN);
            if (mainFragment == null) {
                mainFragment = MainFragment.newInstance();
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, mainFragment)
                    .setTransition(FragmentTransaction.TRANSIT_NONE)
                    .commit();
            updateMainLayout(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        if (mSpiceManager.isStarted()) {
            mSpiceManager.shouldStop();
        }
    }

    private void saveAccountName(String accountName) {
        if (accountName != null) {

            preferences.saveAccountName(accountName);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                    == PackageManager.PERMISSION_GRANTED) {
                credential.setSelectedAccountName(accountName);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.GET_ACCOUNTS)) {
                    Toast.makeText(MainActivity.this, "I need your permission to log in",
                            Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        REQUEST_PERMISSION);
                Utils.log("asking for permission");
                updateMainLayout(false);
                return;
            }

            // If PhoneToDesktop hasn't been authorized by the user
            // this will lead to an UserRecoverableAuthIOException
            // that will generate an onActivityResult for
            // REQUEST_ACCOUNT_PICKER
            Utils.log("starting saveListId");
            saveListId();
            updateMainLayout(true);
        }
        // else
        // User cancelled, or any other error during authorization
        // updateMainLayout(false);
    }

    @Override
    public void startAuthorization() {
        if (checkGooglePlayServicesAvailable()) {
            // ask user to choose account
            updateMainLayout(true);
            preferences.saveListId(null);
            startActivityForResult(credential.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    public boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    public void showGooglePlayServicesAvailabilityErrorDialog(int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode, this, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void updateMainLayout(boolean updating) {
        Utils.log("updating layout " + updating);
        if (mainFragment != null) {
            mainFragment.setUpdating(updating);
            if (mainFragment.isVisible()) {
                mainFragment.updateMainLayout();
            }
        }
    }

    /**
     * Retrieves the list id to use. Creates a new list if no list named "PhoneToDesktop" is found.
     */
    private void saveListId() {
        if (!mSpiceManager.isStarted()) {
            mSpiceManager.start(this);
        }

        RequestListener<Void> listRequestListener = new RequestListener<Void>() {
            @Override
            public void onRequestFailure(SpiceException spiceException) {
                if (spiceException.getCause() instanceof UserRecoverableAuthIOException) {
                    UserRecoverableAuthIOException userRecoverableAuthIOException =
                            (UserRecoverableAuthIOException) spiceException.getCause();
                    startActivityForResult(userRecoverableAuthIOException.getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    RetryDialog.newInstance(MainActivity.this)
                            .show(getSupportFragmentManager(), "retry_dialog");
                }
            }

            @Override
            public void onRequestSuccess(Void aVoid) {
                Utils.log("saveListId success");
                updateMainLayout(false);
                if (showWelcome) {
                    startActivity(new Intent(MainActivity.this, TutorialActivity.class));
                    showWelcome = false;
                }
            }
        };

        TasksListRequest request = new TasksListRequest(this);
        mSpiceManager.execute(request, listRequestListener);
    }

    public static class RetryDialog extends DialogFragment implements
            DialogInterface.OnClickListener {

        private MainActivity activity;

        public static RetryDialog newInstance(MainActivity act) {
            RetryDialog dialog = new RetryDialog();
            dialog.activity = act;
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                    .setTitle(R.string.app_name).setMessage(R.string.txt_retry)
                    .setPositiveButton(android.R.string.yes, this)
                    .setNegativeButton(android.R.string.no, this);
            return dialogBuilder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    activity.saveListId();
                case DialogInterface.BUTTON_NEGATIVE:
                    activity.updateMainLayout(false);
            }
            dialog.dismiss();
        }

    }
}
