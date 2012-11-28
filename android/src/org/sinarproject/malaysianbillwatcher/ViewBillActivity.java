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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewBillActivity extends Activity {
	private final String DOCS_URL = "http://docs.google.com/viewer?embedded=true&url=";
	private final String CACHE_URL = "http://webcache.googleusercontent.com/search?q=cache:";
	private final String ARCHIVE_URL = "http://liveweb.archive.org/";

	private DbAdapter mDbHelper;
	private Long mRowId;
	private boolean mFav = false;
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
			mFav = c.getInt(c.getColumnIndex(DbAdapter.KEY_FAV)) != 0;

			print_rev(long_name, name, year, status, date_presented,
					read_by, supported_by, url, sinar_url, mFav);
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
					((CheckBox) findViewById(R.id.mark_as_read)).setChecked(!is_checked);
				}
			}
		});
	}

	private void print_rev(final String long_name, String name, String year,
			String status, String date_presented, String read_by,
			String supported_by, final String url, final String sinar_url,
			boolean fav)
	{
		TextView long_name_view = (TextView) findViewById(R.id.long_name);
		TextView name_view = (TextView) findViewById(R.id.name);
		TextView year_view = (TextView) findViewById(R.id.year);
		TextView status_view = (TextView) findViewById(R.id.status);
		TextView date_presented_view = (TextView) findViewById(R.id.date_presented);
		TextView read_by_view = (TextView) findViewById(R.id.read_by);
		TextView supported_by_view = (TextView) findViewById(R.id.supported_by);
		TextView link_view = (TextView) findViewById(R.id.link);
		Button view_bill_button = (Button) findViewById(R.id.view_bill);
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);

		long_name_view.setText(long_name);
		name_view.setText(name);
		year_view.setText(year);
		status_view.setText(status);
		date_presented_view.setText(date_presented);

		if (read_by != null) {
			read_by_view.setText(read_by.replaceAll(", ", "\n").replace("\\", ""));
		}
		if (supported_by != null) {
			supported_by_view.setText(supported_by.replaceAll(", ", "\n").replace("\\", ""));
		}

		ImageView fav_v = (ImageView) findViewById(R.id.fav);
		fav_v.setImageResource(fav ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
		fav_v.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					boolean fav = !mFav;
					DbAdapter dbHelper = new DbAdapter();
					dbHelper.open_readwrite(ViewBillActivity.this);
					dbHelper.set_fav(mRowId, !fav);
					dbHelper.close();
					((ImageView)v).setImageResource(fav ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
					mFav = fav;
				/* database might be locked when trying to open it read/write */
				} catch (SQLiteException e) {
				}
			}
		});

		if (sinar_url != null) {
			((TextView) findViewById(R.id.link)).setText(sinar_url.replace(" ", "%20"));

			Button tweet_button = (Button) findViewById(R.id.tweet);
			tweet_button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					send_tweet(long_name, sinar_url);
				}
			});
		}

		if (url == null) {
			layout.removeView(view_bill_button);
		} else {
			view_bill_button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					//String wrapper_url = url.indexOf(' ') == -1 ? CACHE_URL : DOCS_URL;
					String wrapper_url = DOCS_URL;
					String encoded_url = Uri.encode(url).replace("+", "%20");
					view_url(wrapper_url + encoded_url);
					view_url(encoded_url);
				}
			});

			Button download_button = (Button) findViewById(R.id.download);
			download_button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v)
				{
					String encoded_url = url.replace(" ", "%20");
					//view_url(ARCHIVE_URL + encoded_url);
					view_url(encoded_url);
				}
			});
		}

		if (name == null) {
			layout.removeView(findViewById(R.id.name_label));
			layout.removeView(name_view);
		}
		if (year == null) {
			layout.removeView(findViewById(R.id.year_label));
			layout.removeView(year_view);
		}
		if (status == null) {
			layout.removeView(findViewById(R.id.status_label));
			layout.removeView(status_view);
		}
		if (date_presented == null) {
			layout.removeView(findViewById(R.id.date_presented_label));
			layout.removeView(date_presented_view);
		}
		if (read_by == null) {
			layout.removeView(findViewById(R.id.read_by_label));
			layout.removeView(read_by_view);
		}
		if (supported_by == null) {
			layout.removeView(findViewById(R.id.supported_by_label));
			layout.removeView(supported_by_view);
		}
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

