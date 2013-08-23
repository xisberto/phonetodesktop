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
package net.xisberto.phonetodesktop.model;

import net.xisberto.phonetodesktop.database.DatabaseHelper;
import android.content.Context;

public class LocalTask {

	public enum Status {
		ADDED, PROCESSING_UNSHORTEN, PROCESSING_TITLE, WAITING, SENDING, SENT;
	}
	
	private String description, title, google_id;
	private Status status;
	
	public LocalTask() {
		this.google_id = "";
		this.title = "";
		this.description = "";
		this.status = Status.ADDED;
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

	public Status getStatus() {
		return status;
	}

	public LocalTask setStatus(Status status) {
		this.status = status;
		return this;
	}
	
	public void persist(Context context, PersistCallback callback) {
		new PersistThread(DatabaseHelper.getInstance(context), this, callback)
				.start();
	}
	
	public void persist(Context context) {
		this.persist(context, null);
	}
	
	private class PersistThread extends Thread {
		private DatabaseHelper helper;
		private LocalTask task;
		private PersistCallback callback;
		
		public PersistThread(DatabaseHelper helper, LocalTask task, PersistCallback callback) {
			this.helper = helper;
			this.task = task;
			this.callback = callback;
		}

		@Override
		public void run() {
			helper.insert(task);
			if (callback != null) {
				callback.done();
			}
		}
		
	}
	
	public interface PersistCallback {
		public void done();
	}
}
