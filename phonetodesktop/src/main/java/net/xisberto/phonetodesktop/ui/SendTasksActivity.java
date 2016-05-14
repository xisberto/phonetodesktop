/**
 * ****************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p/>
 * Contributors:
 * Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 * ****************************************************************************
 */
package net.xisberto.phonetodesktop.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.rampo.updatechecker.UpdateChecker;
import com.rampo.updatechecker.notice.Notice;

import net.xisberto.phonetodesktop.BuildConfig;
import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.model.LocalTask;
import net.xisberto.phonetodesktop.model.LocalTask.Options;
import net.xisberto.phonetodesktop.model.LocalTask.PersistCallback;
import net.xisberto.phonetodesktop.network.GoogleTasksSpiceService;
import net.xisberto.phonetodesktop.network.SendTasksService;
import net.xisberto.phonetodesktop.network.TaskOptionsRequest;

public class SendTasksActivity extends AppCompatActivity implements
        android.content.DialogInterface.OnClickListener {

    private static final String SAVE_CACHE_UNSHORTEN = "cache_unshorten",
            SAVE_CACHE_TITLES = "cache_titles",
            SAVE_LOCAL_TASK_ID = "local_task_id",
            SAVE_IS_WAITING = "is_waiting";
    protected SpiceManager spiceManager = new SpiceManager(GoogleTasksSpiceService.class);
    private String text_from_extra;
    private SendFragment send_fragment;
    private boolean restoreFromPreferences, isWaiting = false;
    private Preferences prefs;
    private LocalTask localTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.log("onCreate " + this.toString());

        prefs = Preferences.getInstance(this);
        if (prefs.loadShowPreview()) {
            // If we will show the activity, change the theme
            setTheme(R.style.Theme_PhoneToDesktop_Dialog);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UpdateChecker checker = new UpdateChecker(this);
        checker.setNotice(Notice.NOTIFICATION);
        checker.start();

        if (getIntent().getAction().equals(Intent.ACTION_SEND)
                && getIntent().hasExtra(Intent.EXTRA_TEXT)) {
            text_from_extra = getIntent().getStringExtra(Intent.EXTRA_TEXT);

            DatabaseHelper databaseHelper = DatabaseHelper
                    .getInstance(getApplicationContext());

            if (savedInstanceState != null) {
                long local_id = savedInstanceState.getLong(SAVE_LOCAL_TASK_ID);
                localTask = databaseHelper.getTask(local_id);
                //The caches aren't saved to the databse
                localTask.cache_titles = savedInstanceState
                        .getStringArray(SAVE_CACHE_TITLES);
                localTask.cache_unshorten = savedInstanceState
                        .getStringArray(SAVE_CACHE_UNSHORTEN);
                isWaiting = savedInstanceState.getBoolean(SAVE_IS_WAITING);
                restoreFromPreferences = false;
            } else {
                localTask = new LocalTask(this);
                localTask.setTitle(text_from_extra);
                if (!prefs.loadShowPreview()) {
                    // User has chosen to not see the preview, so we process and
                    // send the task without showing the activity.
                    // processPreferences calls sentText on localTask's persist
                    // callback
                    spiceManager.start(this);
                    processPreferences();
                    finish();
                    return;
                }
                restoreFromPreferences = true;
            }

            send_fragment = (SendFragment) getSupportFragmentManager()
                    .findFragmentByTag("send_fragment");
            if (send_fragment == null) {
                send_fragment = SendFragment.newInstance(text_from_extra);
            }
            if (!send_fragment.isAdded()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_frame, send_fragment,
                                "send_fragment").commit();
            }
        } else {
            finish();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        spiceManager.start(this);
    }

    @Override
    protected void onStop() {
        spiceManager.shouldStop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (restoreFromPreferences) {
            send_fragment.cb_only_links.setChecked(prefs.loadOnlyLinks());
            send_fragment.cb_unshorten.setChecked(prefs.loadUnshorten());
            send_fragment.cb_get_titles.setChecked(prefs.loadGetTitles());
            send_fragment.cb_show_preview.setChecked(prefs.loadShowPreview());
        }
        if (!isWaiting) {
            Utils.log("not waiting, processCheckBoxes");
            processCheckBoxes();
        } else {
            Utils.log("waiting, setWaiting");
            setWaiting();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //We save the caches because they don't go to the database
        outState.putStringArray(SAVE_CACHE_UNSHORTEN, localTask.cache_unshorten);
        outState.putStringArray(SAVE_CACHE_TITLES, localTask.cache_titles);
        outState.putBoolean(SAVE_IS_WAITING, isWaiting);
        outState.putLong(SAVE_LOCAL_TASK_ID, localTask.getLocalId());
    }

    @Override
    public void onBackPressed() {
        if (!BuildConfig.DEBUG) {
            localTask.delete();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_send, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                localTask.delete();
                finish();
                break;
            case R.id.item_send:
                sendText();
                saveCheckBoxes();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int whichButton) {
        switch (whichButton) {
            case DialogInterface.BUTTON_POSITIVE:
                sendText();
                saveCheckBoxes();
                finish();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                localTask.delete();
                finish();
                break;
        }
    }

    private void sendText() {
        SendTasksService.sendTasks(this);
    }

    private void saveCheckBoxes() {
        prefs.saveOnlyLinks(send_fragment.cb_only_links.isChecked());
        prefs.saveUnshorten(send_fragment.cb_unshorten.isChecked());
        prefs.saveGetTitles(send_fragment.cb_get_titles.isChecked());
        prefs.saveShowPreview(send_fragment.cb_show_preview.isChecked());
    }

    private void processPreferences() {
        processOptions(prefs.loadOnlyLinks(), prefs.loadUnshorten(),
                prefs.loadGetTitles(), true);
    }

    private void processCheckBoxes() {
        processOptions(send_fragment.cb_only_links.isChecked(),
                send_fragment.cb_unshorten.isChecked(),
                send_fragment.cb_get_titles.isChecked(), false);
    }

    private void processOptions(boolean only_links, boolean unshorten,
                                boolean get_titles, final boolean send_immediately) {
        String links = Utils.filterLinks(text_from_extra).trim();
        PersistCallback callback = new PersistCallback() {
            @Override
            public void run() {
                if (localTask.hasOption(Options.OPTION_UNSHORTEN)
                        || localTask.hasOption(Options.OPTION_GETTITLES)) {
                    // Only start service if there's some option to process
                    startProcessingTask();
                    setWaiting();
                }
                if (send_immediately) {
                    sendText();
                }
            }
        };

        localTask.setOptions(0);

        if (links.equals("")) {
            Toast.makeText(this, R.string.txt_no_links, Toast.LENGTH_SHORT)
                    .show();
            localTask.persist(callback);
            return;
        }

        if (only_links) {
            localTask.setTitle(links);
        } else {
            localTask.setTitle(text_from_extra);
        }

        if (unshorten) {
            if (localTask.cache_unshorten != null) {
                localTask.setTitle(Utils.replace(localTask.getTitle(),
                        localTask.cache_unshorten));
            } else {
                localTask.addOption(Options.OPTION_UNSHORTEN);
            }
        } else {
            localTask.removeOption(Options.OPTION_UNSHORTEN);
        }

        if (get_titles) {
            if (localTask.cache_titles != null) {
                localTask.setTitle(Utils.appendInBrackets(localTask.getTitle(),
                        localTask.cache_titles));
            } else {
                localTask.addOption(Options.OPTION_GETTITLES);
            }
        } else {
            localTask.removeOption(Options.OPTION_GETTITLES);
        }

        localTask.persist(callback);

        if (send_fragment != null) {
            send_fragment.setPreview(localTask.getTitle());
        }
    }

    private void startProcessingTask() {
        TaskOptionsRequest taskOptionsRequest = new TaskOptionsRequest(localTask);
        spiceManager.execute(taskOptionsRequest, new RequestListener<LocalTask>() {
            @Override
            public void onRequestFailure(SpiceException spiceException) {
                setDone();
            }

            @Override
            public void onRequestSuccess(LocalTask task) {
                Log.w("TaskOptionsRequest", String.format("localTask %s: %s", localTask.getLocalId(), localTask.getTitle()));
                Log.w("TaskOptionsRequest", String.format("task %s: %s", task.getLocalId(), task.getTitle()));
                if (send_fragment != null) {
                    send_fragment.setPreview(localTask.getTitle());
                }
                setDone();
            }
        });

    }

    public void setWaiting() {
        Utils.log("Waiting " + this.toString());
        isWaiting = true;
        if (send_fragment != null) {
            send_fragment.setWaiting(true);
        }
    }

    public void setDone() {
        Utils.log("Done " + this.toString());
        isWaiting = false;
        if (send_fragment != null) {
            send_fragment.setWaiting(false);
        }
    }

    public static class SendFragment extends DialogFragment implements
            OnClickListener {
        private CheckBox cb_only_links, cb_unshorten, cb_get_titles,
                cb_show_preview;
        private View v;

        public static SendFragment newInstance(String text) {
            SendFragment fragment = new SendFragment();
            Bundle args = new Bundle();
            args.putString(Intent.EXTRA_TEXT, text);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onAttach(Activity activity) {
            if (activity instanceof SendTasksActivity) {
                super.onAttach(activity);
            } else {
                throw new ClassCastException(
                        "Activity must be SendTasksActivity");
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setView(createView())
                            //.setIcon(R.drawable.ic_launcher)
                            //.setTitle(R.string.filter_title)
                    .setPositiveButton(R.string.send,
                            (SendTasksActivity) getActivity())
                    .setNegativeButton(android.R.string.cancel,
                            (SendTasksActivity) getActivity()).create();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (getDialog() == null) {
                return createView(inflater, container);
            } else {
                return super.onCreateView(inflater, container,
                        savedInstanceState);
            }
        }

        private View createView() {
            return createView(getActivity().getLayoutInflater(), null);
        }

        private View createView(LayoutInflater inflater, ViewGroup container) {
            v = inflater.inflate(R.layout.layout_send_task, container, false);
            ((TextView) v.findViewById(R.id.text_preview))
                    .setText(getArguments().getString(Intent.EXTRA_TEXT));

            cb_only_links = ((CheckBox) v.findViewById(R.id.cb_only_links));
            cb_only_links.setOnClickListener(this);
            cb_unshorten = ((CheckBox) v.findViewById(R.id.cb_unshorten));
            cb_unshorten.setOnClickListener(this);
            cb_get_titles = ((CheckBox) v.findViewById(R.id.cb_get_titles));
            cb_get_titles.setOnClickListener(this);
            cb_show_preview = (CheckBox) v.findViewById(R.id.cb_show_preview);

            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (v != null) {
                Toolbar toolbar = (Toolbar) v.findViewById(R.id.toolbar);
                toolbar.setNavigationIcon(R.drawable.abc_ic_clear_mtrl_alpha);
                ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            getActivity().finish();
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cb_only_links:
                case R.id.cb_unshorten:
                case R.id.cb_get_titles:
                    ((SendTasksActivity) getActivity()).processCheckBoxes();
                    break;

                default:
                    break;
            }
        }

        private void setPreview(String text) {
            ((TextView) v.findViewById(R.id.text_preview)).setText(text);
        }

        private void setWaiting(boolean is_waiting) {
            if (v == null) {
                return;
            }
            if (is_waiting) {
                v.findViewById(R.id.progress).setVisibility(View.VISIBLE);
                v.findViewById(R.id.text_preview).setEnabled(false);
                cb_only_links.setEnabled(false);
                cb_unshorten.setEnabled(false);
                cb_get_titles.setEnabled(false);
            } else {
                v.findViewById(R.id.progress).setVisibility(View.GONE);
                v.findViewById(R.id.text_preview).setEnabled(true);
                cb_only_links.setEnabled(true);
                cb_unshorten.setEnabled(true);
                cb_get_titles.setEnabled(true);
            }
        }
    }

}
