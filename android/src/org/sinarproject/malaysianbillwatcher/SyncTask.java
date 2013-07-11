/*
    Copyright (C) 2012,2013 Sinar Project

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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
	//private final String BILLWATCHER_URL = "http://billwatcher.sinarproject.org/feeds/";
	private final String PARLIMEN_URL = "http://www.parlimen.gov.my/bills-dewan-rakyat.html?uweb=dr&arkib=yes&lang=en#";

	private Context mCtx;
	private int mAddedBills = 0;
	private String mAlertMsg = null;
	private ProgressDialog mProgressDialog = null;

	public SyncTask(Context ctx)
	{
		this(ctx, true);
	}

	public SyncTask(Context ctx, boolean showProgress)
	{
		mCtx = ctx;
		if (ctx != null && showProgress) {
			mProgressDialog = new ProgressDialog(mCtx);
		}
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		try {
			//SAXParserFactory spf = SAXParserFactory.newInstance();
			//SAXParser sp = spf.newSAXParser();
			//XMLReader xr = sp.getXMLReader();

			//URL url = new URL(BILLWATCHER_URL);

			//DbAdapter dbHelper = new DbAdapter();
			//dbHelper.open(mCtx);
			//String lastUpdate = dbHelper.get_last_update();
			//dbHelper.close();

			//RssHandler rss_handler = new RssHandler(this, lastUpdate, 25);
			//xr.setContentHandler(handler);
			//xr.parse(new InputSource(url.openStream()));

			ParlimenHandler handler = new ParlimenHandler(this, 25);
			LinkedList<ContentValues> bills = handler.parseBills(PARLIMEN_URL);
			publishProgress(25);
			//LinkedList<ContentValues> bills = rss_handler.getBills();

			mAddedBills = updateDb(bills, 25, 100);
			send_notifications(bills);

			publishProgress(100);

		/* probably no internet connection */
		} catch (UnknownHostException e) {
			mAlertMsg = mCtx.getResources().getString(R.string.unknown_host);
		} catch (java.io.FileNotFoundException e) {
			mAlertMsg = mCtx.getResources().getString(R.string.file_not_found) + ":\n" + e.getMessage();
		} catch (MalformedURLException e) {
			throw new Error(e);
		//} catch (SAXException e) {
		//	throw new Error(e);
		//} catch (ParserConfigurationException e) {
		//	throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}

		return null;
	}

	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		if (mProgressDialog != null && mCtx != null) {
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
		if (mProgressDialog != null && mCtx != null) {
			try {
				mProgressDialog.dismiss();
			/* view might no longer be attached to window manager */
			} catch (IllegalArgumentException e) {
			}
		}
		if (mAlertMsg != null && mCtx != null && mProgressDialog != null) {
			alert(mAlertMsg);
		} else if (mProgressDialog != null && mCtx != null) {
			Toast.makeText(mCtx,
					Integer.toString(mAddedBills) + " " +
					mCtx.getResources().getString(R.string.updates_found),
					Toast.LENGTH_SHORT).show();
		}
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
						cv.getAsString(DbAdapter.KEY_STATUS),
						cv.getAsLong(DbAdapter.KEY_ROWID));
			}
		} else {
			send_notification(billCnt);
		}
	}

	private void send_notification(String long_name, String status, long row_id)
	{
		NotificationCompat.Builder builder =
			new NotificationCompat.Builder(mCtx)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(long_name)
				.setContentText(status);

		Intent intent = new Intent(mCtx, ViewBillActivity.class);

		Bundle b = new Bundle();
		b.putLong("row_id", row_id);
		intent.putExtras(b);

		/* set action to make intent unique so Extras aren't
			duplicated among notifications */
		intent.setAction(Long.toString(row_id));

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

	private void alert(String msg)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(mCtx);
		alert.setTitle(mCtx.getResources().getString(android.R.string.dialog_alert_title));
		alert.setMessage(msg);
		alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		alert.show();
	}
}

