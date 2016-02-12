package net.xisberto.phonetodesktop.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.services.tasks.model.Task;

import java.util.ArrayList;

public class TaskList implements Parcelable {

    public static final Creator<TaskList> CREATOR = new Creator<TaskList>() {
        @Override
        public TaskList createFromParcel(Parcel in) {
            return new TaskList(in);
        }

        @Override
        public TaskList[] newArray(int size) {
            return new TaskList[size];
        }
    };
    public ArrayList<Task> items;

    public TaskList() {
        items = new ArrayList<>();
    }

    protected TaskList(Parcel in) {
        this();
        in.readList(items, null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(items);
    }
}
