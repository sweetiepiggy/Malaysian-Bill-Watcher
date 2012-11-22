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

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

public class SyncTask extends AsyncTask<Void, Integer, Void>
{
	private final int MAX_NOTIFICATIONS = 3;

	/* TODO: move to Constants.java */
	private final String BILLWATCHER_URL = "http://billwatcher.sinarproject.org/feeds/";

	private Context mCtx;
	private int mAddedBills = 0;
	private ProgressDialog mProgressDialog;

	public SyncTask(Context ctx)
	{
		mCtx = ctx;
		mProgressDialog = new ProgressDialog(mCtx);
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			URL url = new URL(BILLWATCHER_URL);

			DbAdapter dbHelper = new DbAdapter();
			dbHelper.open(mCtx);
			String lastUpdate = dbHelper.get_last_update();
			dbHelper.close();

			RssHandler rss_handler = new RssHandler(this, lastUpdate, 25);
			xr.setContentHandler(rss_handler);
			xr.parse(new InputSource(url.openStream()));

			LinkedList<ContentValues> bills = rss_handler.getBills();
			mAddedBills = updateDb(bills, 25, 100);
			send_notifications(bills);

			publishProgress(100);
		/* TODO: properly handle exceptions */
		} catch (Exception e) {
			throw new Error(e);
		}

		return null;
	}

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		if (mProgressDialog != null) {
			mProgressDialog.setMessage(mCtx.getResources().getString(R.string.syncing));
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(100);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setProgress(0);
			mProgressDialog.show();
		}
	}

	@Override
	protected void onPostExecute(Void result)
	{
		if (mProgressDialog != null) {
			try {
				mProgressDialog.dismiss();
			/* view might no longer be attached to window manager */
			} catch (IllegalArgumentException e) {
			}
		}
		Toast.makeText(mCtx,
				Integer.toString(mAddedBills) + " " +
				mCtx.getResources().getString(R.string.updates_found),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onProgressUpdate(Integer... values)
	{
		super.onProgressUpdate(values);
		if (mProgressDialog != null) {
			mProgressDialog.setProgress(values[0]);
		}
	}

	public void updateProgress(int progress)
	{
		publishProgress(progress);
	}

	private int updateDb(LinkedList<ContentValues> bills, int progressOffset,
			int maxProgress)
	{
		int billCnt = bills.size();
		int addedBills = 0;

		Iterator<ContentValues> itr = bills.listIterator();
		DbAdapter dbHelper = new DbAdapter();
		dbHelper.open_readwrite(mCtx, false);
		while (itr.hasNext()) {
			ContentValues cv = itr.next();
			long row_id = dbHelper.create_bill_rev(cv);
			cv.put(DbAdapter.KEY_ROWID, row_id);
			++addedBills;
			publishProgress(java.lang.Math.min(maxProgress,
						progressOffset +
						(int)((maxProgress - progressOffset) *
							(double) addedBills / billCnt)));
		}
		dbHelper.close();
		return addedBills;
	}

	private void send_notifications(LinkedList<ContentValues> bills)
	{
		int billCnt = bills.size();
		if (billCnt <= MAX_NOTIFICATIONS) {
			Iterator<ContentValues> itr = bills.listIterator();
			while (itr.hasNext()) {
				ContentValues cv = itr.next();
				send_notification(cv.getAsString(DbAdapter.KEY_LONG_NAME),
						cv.getAsLong(DbAdapter.KEY_ROWID));
			}
		} else {
			send_notification(billCnt);
		}
	}

	private void send_notification(String long_name, long row_id)
	{
		NotificationCompat.Builder builder =
			new NotificationCompat.Builder(mCtx)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(mCtx.getResources().getString(R.string.app_name))
				.setContentText(long_name);

		Intent intent = new Intent(mCtx, ViewBillActivity.class);

		Bundle b = new Bundle();
		b.putLong("row_id", row_id);
		intent.putExtras(b);

		TaskStackBuilder sb = TaskStackBuilder.create(mCtx);

		sb.addParentStack(ViewBillActivity.class);

		/* adds the Intent that starts the Activity to the top of the stack */
		sb.addNextIntent(intent);
		PendingIntent pi = sb.getPendingIntent(
			0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);
		NotificationManager nm = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

		nm.notify((int) row_id, builder.build());
	}

	private void send_notification(int billCnt)
	{
		String msg = Integer.toString(mAddedBills) + " " +
			mCtx.getResources().getString(R.string.updates_found);

		NotificationCompat.Builder builder =
			new NotificationCompat.Builder(mCtx)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(mCtx.getResources().getString(R.string.app_name))
				.setContentText(msg);

		Intent intent = new Intent(mCtx, BrowseActivity.class);

		TaskStackBuilder sb = TaskStackBuilder.create(mCtx);

		sb.addParentStack(BrowseActivity.class);

		/* adds the Intent that starts the Activity to the top of the stack */
		sb.addNextIntent(intent);
		PendingIntent pi = sb.getPendingIntent(
			0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);
		NotificationManager nm = (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

		nm.notify(0, builder.build());
	}
}

