/*******************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop.database;

import java.util.ArrayList;
import java.util.List;

import net.xisberto.phonetodesktop.model.LocalTask;
import net.xisberto.phonetodesktop.model.LocalTask.Status;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	public static final String DATABASE_NAME = "phonetodesktop";
	public static final int DATABASE_VERSION = 1;

	private static DatabaseHelper instance;
	
	private Context context;

	private DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	public static synchronized DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseHelper(context.getApplicationContext());
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TableTasks.CREATE_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	private ContentValues contentValuesFromTask(LocalTask task) {
		ContentValues cv = new ContentValues();
		cv.put(TableTasks.COLUMN_GOOGLE_ID, task.getGoogleId());
		cv.put(TableTasks.COLUMN_TITLE, task.getTitle());
		cv.put(TableTasks.COLUMN_DESCRIPTION, task.getDescription());
		cv.put(TableTasks.COLUMN_OPTIONS, task.getOptionsAsInt());
		cv.put(TableTasks.COLUMN_STATUS, task.getStatus().name());
		return cv;
	}

	private LocalTask taskFromCursor(Cursor c) {
		LocalTask result = new LocalTask(context);
		result
				.setLocalId(c.getLong(0))
				.setGoogleId(c.getString(1))
				.setTitle(c.getString(2))
				.setDescription(c.getString(3))
				.setOptions(c.getInt(4))
				.setStatus(Status.valueOf(c.getString(5)));
		return result;
	}
	
	private String whereColumnIn(String column, long[] array) {
		Long[] Larray = new Long[array.length];
		for (int i = 0; i < Larray.length; i++) {
			Larray[i] = array[i];
		}
		return whereColumnIn(column, Larray);
	}
	
	private String whereColumnIn(String column, Long[] array) {
		
		String whereClause = column + " IN (";
		for (int i = 0; i < array.length; i ++) {
			if (i != array.length-1) {
				whereClause += array[i] + ", ";
			} else {
				whereClause += array[i] + ")";
			}
		}
		return whereClause;
	}

	public long insert(LocalTask task) throws SQLException {
		final ContentValues cv = contentValuesFromTask(task);
		final SQLiteDatabase db = getWritableDatabase();
		try {
			long result = db.insertOrThrow(TableTasks.TABLE_NAME, null, cv);
			return result;
		} finally {
		}
	}

	public void update(LocalTask task) {
		final SQLiteDatabase db = getWritableDatabase();
		try {
			final ContentValues cv = contentValuesFromTask(task);
			db.update(TableTasks.TABLE_NAME, cv, TableTasks.COLUMN_LOCAL_ID + " = ?",
					new String[] { Long.toString(task.getLocalId()) });
		} finally {
		}
	}
	
	public void delete(LocalTask task) {
		final SQLiteDatabase db = getWritableDatabase();
		try {
			db.delete(TableTasks.TABLE_NAME, TableTasks.COLUMN_LOCAL_ID + " = ?",
					new String[] { Long.toString(task.getLocalId())});
		} finally {
		}
	}
	
	public void delete(Long[] ids) {
		final SQLiteDatabase db = getWritableDatabase();
		try {
			db.delete(TableTasks.TABLE_NAME, whereColumnIn(TableTasks.COLUMN_LOCAL_ID, ids),
					null);
		} finally {
		}
	}
	
	public void setStatus(Status status, long... ids) {
		if (ids != null && ids.length > 0) {
			final SQLiteDatabase db = getWritableDatabase();
			try {
				ContentValues cv = new ContentValues();
				String whereClause = whereColumnIn(TableTasks.COLUMN_LOCAL_ID, ids);
				cv.put(TableTasks.COLUMN_STATUS, status.name());
				db.update(TableTasks.TABLE_NAME, cv,
						whereClause, null);
			} finally {
			}
		}
	}

	public int getTasksCount() {
		final SQLiteDatabase db = getReadableDatabase();
		try {
			final Cursor cursor = db.query(TableTasks.TABLE_NAME,
					TableTasks.COLUMNS, null, null, null, null, TableTasks.COLUMN_LOCAL_ID);
			return cursor.getCount();
		} finally {
		}
	}

	public LocalTask getTask(long local_id) {
		final SQLiteDatabase db = getReadableDatabase();
		try {
			final Cursor cursor = db.query(TableTasks.TABLE_NAME,
					TableTasks.COLUMNS, TableTasks.COLUMN_LOCAL_ID + " = ?",
					new String[] { Long.toString(local_id) }, null, null, null);
			if (cursor.getCount() == 1) {
				cursor.moveToFirst();
				final LocalTask task = taskFromCursor(cursor);
				return task;
			} else {
				return null;
			}
		} finally {
		}
	}
	
	public Cursor listTasksAsCursor() {
		final SQLiteDatabase db = getReadableDatabase();
		try {
			return db.query(TableTasks.TABLE_NAME,
					TableTasks.COLUMNS,
					null, null, null, null,
					TableTasks.COLUMN_LOCAL_ID + " DESC");
		} finally {
		}
	}
	
	public Cursor listTasksAsCursor(Status status) {
		SQLiteDatabase db = getReadableDatabase();
		try {
			return db.query(TableTasks.TABLE_NAME, TableTasks.COLUMNS,
					TableTasks.COLUMN_STATUS + " = ?",
					new String[] { status.name() },
					null, null, null);
		} finally {
		}
	}
	
	public Cursor listTaskQueueAsCursor() {
		SQLiteDatabase db = getReadableDatabase();
		return db.query(TableTasks.TABLE_NAME, TableTasks.COLUMNS,
				TableTasks.COLUMN_STATUS + " != ?",
				new String[] { Status.SENT.name() },
				null, null, null);
	}
	
	public Cursor listTasksAsCursorById(Long[] ids) {
		SQLiteDatabase db = getReadableDatabase();
		try {
			return db.query(TableTasks.TABLE_NAME, TableTasks.COLUMNS,
					whereColumnIn(TableTasks.COLUMN_LOCAL_ID, ids),
					null, null, null, null);
		} finally {
		}
	}

	public List<LocalTask> listTasks() {
		List<LocalTask> tasks = new ArrayList<LocalTask>();

		Cursor cursor = listTasksAsCursor();
		try {
			while (cursor.moveToNext()) {
				LocalTask task = taskFromCursor(cursor);
				tasks.add(task);
			}
		} finally {
			cursor.close();
		}
		return tasks;
	}
	
	public List<LocalTask> listTasks(Status status) {
		List<LocalTask> tasks = new ArrayList<LocalTask>();
		Cursor cursor = listTasksAsCursor(status);
		try {
			while (cursor.moveToNext()) {
				LocalTask task = taskFromCursor(cursor);
				tasks.add(task);
			}
		} finally {
			cursor.close();
		}
		return tasks;
	}
}
