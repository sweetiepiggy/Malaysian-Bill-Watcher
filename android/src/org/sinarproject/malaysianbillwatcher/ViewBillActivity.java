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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewBillActivity extends Activity {
	private final String GOOGLE_DOCS_URL = "http://docs.google.com/viewer?url=";
	DbAdapter mDbHelper;
	Long mRowId;

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

		mDbHelper = new DbAdapter();
		mDbHelper.open_readwrite(this);
		mDbHelper.set_read(mRowId, true);
		mDbHelper.close();

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

			print_rev(long_name, name, year, status, date_presented,
					read_by, supported_by, url);
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
				boolean need_reopen = false;
				if (mDbHelper != null) {
					mDbHelper.close();
					need_reopen = true;
				}
				mDbHelper = new DbAdapter();
				mDbHelper.open_readwrite(ViewBillActivity.this);
				mDbHelper.set_read(mRowId, is_checked);
				mDbHelper.close();

				if (need_reopen) {
					mDbHelper.open(ViewBillActivity.this);
				}
			}
		});
	}

	private void print_rev(String long_name, String name, String year,
			String status, String date_presented, String read_by,
			String supported_by, final String url)
	{
		((TextView) findViewById(R.id.long_name)).setText(long_name);
		((TextView) findViewById(R.id.name)).setText(name);
		((TextView) findViewById(R.id.year)).setText(year);
		((TextView) findViewById(R.id.status)).setText(status);
		((TextView) findViewById(R.id.date_presented)).setText(date_presented);
		((TextView) findViewById(R.id.read_by)).setText(read_by.replaceAll(", ", "\n").replace("\\", ""));
		((TextView) findViewById(R.id.supported_by)).setText(supported_by.replaceAll(", ", "\n").replace("\\", ""));
		((TextView) findViewById(R.id.link)).setText(url.replace(" ", "%20"));

		Button view_bill_button = (Button) findViewById(R.id.view_bill);
		view_bill_button.setText(getResources().getString(R.string.view_bill));
		view_bill_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				view_pdf(url);
			}
		});
	}

	private void view_pdf(String uri_str)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		String encoded_uri = Uri.encode(uri_str).replace("+", "%20");
		intent.setDataAndType(Uri.parse(GOOGLE_DOCS_URL + encoded_uri), "text/html");
		startActivity(Intent.createChooser(intent, getResources().getString(R.string.open_browser)));
	}
}

