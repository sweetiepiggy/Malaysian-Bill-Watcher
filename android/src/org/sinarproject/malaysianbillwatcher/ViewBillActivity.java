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

		Button sync_button = new Button(getApplicationContext());
		sync_button.setText("sync");
		sync_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				SyncTask sync = new SyncTask(getApplicationContext());
				sync.execute();
			}
		});
		Button main_button = new Button(getApplicationContext());
		main_button.setText("main");
		main_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				Intent intent = new Intent(getApplicationContext(), MalaysianBillWatcherActivity.class);
				startActivity(intent);
			}
		});
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(sync_button);
		layout.addView(main_button);


		DbAdapter dbHelper = new DbAdapter();
		/* TODO: should adapter be closed? */
		dbHelper.open(this);

		Cursor c = dbHelper.fetch_bill(row_id);
		startManagingCursor(c);
		if (c.moveToFirst()) {
			String long_name = c.getString(c.getColumnIndex(DbAdapter.KEY_LONG_NAME));
			((TextView) findViewById(R.id.long_name)).setText(long_name);

			Cursor c_rev = dbHelper.fetch_revs(long_name);
			startManagingCursor(c_rev);
			if (c_rev.moveToFirst()) do {
				String code = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_NAME));
				String rev = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_YEAR));
				String status = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_STATUS));
				String url = c_rev.getString(c_rev.getColumnIndex(DbAdapter.KEY_URL));

				print_rev(code, rev, status, url);
			} while (c_rev.moveToNext());
		}
	}

	private void print_rev(String code, String rev, String status, final String url)
	{
		TextView code_view = new TextView(getApplicationContext());
		TextView rev_view = new TextView(getApplicationContext());
		TextView status_view = new TextView(getApplicationContext());
		Button view_bill_button = new Button(getApplicationContext());

		code_view.setText(code);
		rev_view.setText(rev);
		status_view.setText(status);
		view_bill_button.setText(getResources().getString(R.string.view_bill));

		view_bill_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				view_pdf(url);
			}
		});

		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(code_view);
		layout.addView(rev_view);
		layout.addView(status_view);
		layout.addView(view_bill_button);
	}

	private void view_pdf(String uri_str)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(GOOGLE_DOCS_URL + uri_str), "text/html");
		startActivity(Intent.createChooser(intent, "Open Web Browser"));
	}
}

