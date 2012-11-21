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
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class ViewBillActivity extends Activity {
	private final String DOCS_URL = "http://docs.google.com/viewer?embedded=true&url=";
	private final String CACHE_URL = "http://webcache.googleusercontent.com/search?q=cache:";
	private final String ARCHIVE_URL = "http://liveweb.archive.org/";

	private DbAdapter mDbHelper;
	private Long mRowId;
	private static final String TWITTER_ADDR = "@sinarproject";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);

		Bundle b = getIntent().getExtras();
		if (b == null) {
			b = new Bundle();
		}
		mRowId = b.getLong("row_id");

		try {
			mDbHelper = new DbAdapter();
			mDbHelper.open_readwrite(this);
			mDbHelper.set_read(mRowId, true);
			mDbHelper.close();
		/* database might be locked when trying to open it read/write */
		} catch (SQLiteException e) {
		}

		init_checkbox();

		mDbHelper.open(this);

		Cursor c = mDbHelper.fetch_rev(mRowId);
		if (c.moveToFirst()) {
			String long_name = c.getString(c.getColumnIndex(DbAdapter.KEY_LONG_NAME));
			String name = c.getString(c.getColumnIndex(DbAdapter.KEY_NAME));
			String year = c.getString(c.getColumnIndex(DbAdapter.KEY_YEAR));
			String status = c.getString(c.getColumnIndex(DbAdapter.KEY_STATUS));
			String date_presented = c.getString(c.getColumnIndex(DbAdapter.KEY_DATE_PRESENTED));
			String read_by = c.getString(c.getColumnIndex(DbAdapter.KEY_READ_BY));
			String supported_by = c.getString(c.getColumnIndex(DbAdapter.KEY_SUPPORTED_BY));
			String url = c.getString(c.getColumnIndex(DbAdapter.KEY_URL));
			String sinar_url = c.getString(c.getColumnIndex(DbAdapter.KEY_SINAR_URL));

			print_rev(long_name, name, year, status, date_presented,
					read_by, supported_by, url, sinar_url);
		}
		c.close();
	}

	@Override
	protected void onDestroy() {
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		super.onDestroy();
	}

	private void init_checkbox()
	{
		CheckBox read_checkbox = (CheckBox) findViewById(R.id.mark_as_read);
		read_checkbox.setChecked(true);

		read_checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton button_view, boolean is_checked) {
				try {
					DbAdapter dbHelper = new DbAdapter();
					dbHelper.open_readwrite(ViewBillActivity.this);
					dbHelper.set_read(mRowId, is_checked);
					dbHelper.close();
				/* database might be locked when trying to open it read/write */
				} catch (SQLiteException e) {
				}
			}
		});
	}

	private void print_rev(final String long_name, String name, String year,
			String status, String date_presented, String read_by,
			String supported_by, final String url, final String sinar_url)
	{
		((TextView) findViewById(R.id.long_name)).setText(long_name);
		((TextView) findViewById(R.id.name)).setText(name);
		((TextView) findViewById(R.id.year)).setText(year);
		((TextView) findViewById(R.id.status)).setText(status);
		((TextView) findViewById(R.id.date_presented)).setText(date_presented);
		((TextView) findViewById(R.id.read_by)).setText(read_by.replaceAll(", ", "\n").replace("\\", ""));
		((TextView) findViewById(R.id.supported_by)).setText(supported_by.replaceAll(", ", "\n").replace("\\", ""));
		((TextView) findViewById(R.id.link)).setText(sinar_url.replace(" ", "%20"));

		Button view_bill_button = (Button) findViewById(R.id.view_bill);
		view_bill_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				String wrapper_url = url.indexOf(' ') == -1 ? CACHE_URL : DOCS_URL;
				String encoded_url = Uri.encode(url).replace("+", "%20");
				view_url(wrapper_url + encoded_url);
			}
		});

		Button download_button = (Button) findViewById(R.id.download);
		download_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				String encoded_url = url.replace(" ", "%20");
				view_url(ARCHIVE_URL + encoded_url);
			}
		});

		Button tweet_button = (Button) findViewById(R.id.tweet);
		tweet_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				send_tweet(long_name, sinar_url);
			}
		});
	}

	private void view_url(String url)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(url), "text/html");
		startActivity(Intent.createChooser(intent, null));
	}


	private void send_tweet(String long_name, String url)
	{
		String msg = format_tweet(long_name, url);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, msg);
		startActivity(Intent.createChooser(intent, null));
	}

	private String format_tweet(String long_name, String url)
	{
		return long_name + " " + url + " via " + TWITTER_ADDR;
	}
}

