/*
    Copyright (C) 2012 Sinar Project

    This file is part of Malaysian Bill Watcher.

    Malaysian Bill Watcher is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    Malaysian Bill Watcher is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Malaysian Bill Watcher; if not, see <http://www.gnu.org/licenses/>.
*/

package org.sinarproject.malaysianbillwatcher;

import android.app.IntentService;
import android.content.Intent;

public class BillWatcherService extends IntentService
{
	/* minutes between syncs */
	private static final int SYNC_FREQ = 24 * 60;

	public BillWatcherService()
	{
		super("BillWatcherService");
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		while (true) {
			long endTime = SYNC_FREQ * 60 * 1000 + System.currentTimeMillis();
			while (System.currentTimeMillis() < endTime) {
				synchronized (this) {
					try {
						/* note, SYNC_FREQ should be greater than this (1hr) */
						wait(60 * 60 * 1000);
					} catch (Exception e) {
					}
				}
			}

			SyncTask sync = new SyncTask(this, false);
			sync.execute();
		}
	}
}

