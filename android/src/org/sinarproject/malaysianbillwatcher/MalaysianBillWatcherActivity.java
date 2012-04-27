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

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MalaysianBillWatcherActivity extends ListActivity {
	private DownloadManager mDm;
	private long mEnqueue;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DbAdapter dbHelper = new DbAdapter();
		dbHelper.open(this);

		fill_data(dbHelper);
		init_click();
		init_receiver();
	}

	private void fill_data(DbAdapter dbHelper)
	{
		Cursor c = dbHelper.fetch_bills();
		startManagingCursor(c);
		SimpleCursorAdapter bills = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1,
				c, new String[] {DbAdapter.KEY_URL},
				new int[] {android.R.id.text1});
		setListAdapter(bills);
	}

	private void init_click()
	{
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int pos, long id) {
				String url = ((TextView) v).getText().toString();

				view_pdf(url);
//				mDm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//				Toast.makeText(getApplicationContext(), "Downloading " + url, Toast.LENGTH_SHORT).show();
//				Request request = new Request(Uri.parse(url));
//				mEnqueue = mDm.enqueue(request);

			}
		});
	}

	private void init_receiver()
	{
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent)
			{
				String action = intent.getAction();
				if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
					long download_id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
					Query query = new Query();
					query.setFilterById(mEnqueue);
					Cursor c = mDm.query(query);
					if (c.moveToFirst()) {
						int col_idx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
						if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(col_idx)) {
							String uri_str = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
							view_pdf(uri_str);
						}
					}
				}
			}
		};

		registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	private void view_pdf(String uri_str)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(uri_str), "application/PDF");
		startActivity(Intent.createChooser(intent, "View PDF"));
	}
}

