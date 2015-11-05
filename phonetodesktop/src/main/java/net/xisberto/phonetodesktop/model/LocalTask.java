/*******************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p/>
 * Contributors:
 * Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop.model;

import android.content.Context;
import android.os.Handler;
import android.os.Process;

import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.database.DatabaseHelper;

import java.util.EnumSet;

public class LocalTask {

    public enum Options {
        OPTION_ONLY_LINKS(1),
        OPTION_UNSHORTEN(2),
        OPTION_GETTITLES(4);
        private int value;

        private Options(int val) {
            value = val;
        }

        private static Options fromValue(int i) {
            for (Options opt : values()) {
                if (opt.value == i) {
                    return opt;
                }
            }
            return null;
        }
    }

    public enum Status {
        ADDED, PROCESSING_UNSHORTEN, PROCESSING_TITLE, READY, SENDING, SENT;
    }

    private long local_id;
    private String description, title, google_id;
    private EnumSet<Options> options;
    private Status status;
    private Context context;
    public String[] cache_unshorten = null;
    public String[] cache_titles = null;


    public LocalTask(Context context) {
        this.local_id = -1;
        this.google_id = "";
        this.title = "";
        this.description = "";
        this.options = EnumSet.noneOf(Options.class);
        this.status = Status.ADDED;
        this.context = context;
    }

    public long getLocalId() {
        return local_id;
    }

    public LocalTask setLocalId(long local_id) {
        this.local_id = local_id;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public LocalTask setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public LocalTask setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getGoogleId() {
        return google_id;
    }

    public LocalTask setGoogleId(String id) {
        this.google_id = id;
        return this;
    }

    public boolean hasOption(Options option) {
        return options.contains(option);
    }

    public EnumSet<Options> getOptions() {
        return options;
    }

    public int getOptionsAsInt() {
        if (options.size() == 0) {
            return 0;
        }
        int result = 0;
        for (Options opt : options) {
            result |= opt.value;
        }
        return result;
    }

    public LocalTask setOptions(int opts) {
        for (Options option : Options.values()) {
            if ((opts & option.value) == option.value) {
                addOption(option);
            } else {
                removeOption(option);
            }
        }
        return this;
    }

    public LocalTask addOption(Options option) {
        options.add(option);
        return this;
    }

    public LocalTask addOption(int opt) {
        Options new_opt = Options.fromValue(opt);
        if (new_opt != null) {
            options.add(new_opt);
        }
        return this;
    }

    public LocalTask removeOption(Options option) {
        options.remove(option);
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public LocalTask setStatus(Status status) {
        this.status = status;
        return this;
    }

    /**
     * Persists this LocalTask blocking the current thread.
     * This is useful for GoogleTasksService.processOptions, since it's
     * important that the task is stored before sending it.
     *
     * @param callback this callback will be posted to the main thread
     */
    public void persistBlocking(PersistCallback callback) {
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        int action = (local_id == -1 ? PersistThread.ACTION_INSERT : PersistThread.ACTION_UPDATE);
        switch (action) {
            case PersistThread.ACTION_INSERT:
                Utils.log("ACTION_INSERT");
                long id = helper.insert(this);
                this.setLocalId(id);
                break;
            case PersistThread.ACTION_UPDATE:
                Utils.log("ACTION_UPDATE " + this.local_id);
                helper.update(this);
                break;
        }
        if (callback != null) {
            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(callback);
        }
    }

    public void persist(PersistCallback callback) {
        int action = (local_id == -1 ? PersistThread.ACTION_INSERT : PersistThread.ACTION_UPDATE);
        new PersistThread(
                this,
                action, callback)
                .start();
    }

    public void persist() {
        this.persist(null);
    }

    public void delete() {
        new PersistThread(
                this,
                PersistThread.ACTION_DELETE, null)
                .start();
    }

    private class PersistThread extends Thread {
        public static final int ACTION_INSERT = 1, ACTION_UPDATE = 2, ACTION_DELETE = 3;
        private LocalTask task;
        private int action;
        private PersistCallback callback;

        public PersistThread(LocalTask task, int action, PersistCallback callback) {
            this.task = task;
            this.action = action;
            this.callback = callback;
            setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        }

        @Override
        public void run() {
            DatabaseHelper helper = DatabaseHelper.getInstance(task.context);
            switch (action) {
                case ACTION_INSERT:
                    Utils.log("ACTION_INSERT");
                    long id = helper.insert(task);
                    task.setLocalId(id);
                    break;
                case ACTION_UPDATE:
                    Utils.log("ACTION_UPDATE " + task.local_id);
                    helper.update(task);
                    break;
                case ACTION_DELETE:
                    Utils.log("ACTION_DELETE " + task.local_id);
                    helper.delete(task);
                    break;
            }
            if (callback != null) {
                Handler mainHandler = new Handler(task.context.getMainLooper());
                mainHandler.post(callback);
            }
        }

    }

    public interface PersistCallback extends Runnable {
    }
}
