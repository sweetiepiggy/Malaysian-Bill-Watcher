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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewBillActivity extends Activity {
	private final String GOOGLE_DOCS_URL = "http://docs.google.com/viewer?url=";
	DbAdapter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);

		Bundle b = getIntent().getExtras();
		if (b == null) {
			b = new Bundle();
		}
		Long row_id = b.getLong("row_id");

		mDbHelper = new DbAdapter();
		mDbHelper.open_readwrite(this);
		mDbHelper.set_read(row_id, true);
		mDbHelper.close();

		mDbHelper.open(this);

		Cursor c = mDbHelper.fetch_bill(row_id);
		startManagingCursor(c);
		if (c.moveToFirst()) {
			String long_name = c.getString(c.getColumnIndex(DbAdapter.KEY_LONG_NAME));
			((TextView) findViewById(R.id.long_name)).setText(long_name);

			Cursor c_rev = mDbHelper.fetch_revs(long_name);
			startManagingCursor(c_rev);
			if (c_rev.moveToFirst()) do {
				String name = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_NAME));
				String year = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_YEAR));
				String status = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_STATUS));
				String date_presented = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_DATE_PRESENTED));
				String read_by = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_READ_BY));
				String supported_by = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_SUPPORTED_BY));
				String url = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_URL));

				print_rev(name, year, status, date_presented,
						read_by, supported_by, url);
			} while (c_rev.moveToNext());
		}
	}

	@Override
	protected void onDestroy() {
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		super.onDestroy();
	}

	private void print_rev(String name, String year, String status,
			String date_presented, String read_by,
			String supported_by, final String url)
	{
		TextView name_view = new TextView(getApplicationContext());
		TextView year_view = new TextView(getApplicationContext());
		TextView status_view = new TextView(getApplicationContext());
		TextView date_presented_view = new TextView(getApplicationContext());
		TextView read_by_view = new TextView(getApplicationContext());
		TextView supported_by_view = new TextView(getApplicationContext());
		Button view_bill_button = new Button(getApplicationContext());

		name_view.setText(name);
		year_view.setText(year);
		status_view.setText(status);
		date_presented_view.setText(date_presented);
		read_by_view.setText(read_by);
		supported_by_view.setText(supported_by);
		view_bill_button.setText(getResources().getString(R.string.view_bill));

		view_bill_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				view_pdf(url);
			}
		});

		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(name_view);
		layout.addView(year_view);
		layout.addView(status_view);
		layout.addView(date_presented_view);
		layout.addView(read_by_view);
		layout.addView(supported_by_view);
		layout.addView(view_bill_button);
	}

	private void view_pdf(String uri_str)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		String encoded_uri = Uri.encode(uri_str).replace("+", "%20");
		intent.setDataAndType(Uri.parse(GOOGLE_DOCS_URL + encoded_uri), "text/html");
		startActivity(Intent.createChooser(intent, getResources().getString(R.string.open_browser)));
	}
}

