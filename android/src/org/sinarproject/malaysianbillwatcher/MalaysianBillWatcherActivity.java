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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MalaysianBillWatcherActivity extends ListActivity {
	String GOOGLE_DOCS_URL = "http://docs.google.com/viewer?url=";

	DbAdapter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDbHelper = new DbAdapter();
		/* TODO: should adapter be closed? */
		mDbHelper.open(this);

		fill_data(mDbHelper);
		init_click();
	}

	private void fill_data(DbAdapter dbHelper)
	{
		Cursor c = dbHelper.fetch_bills();
		startManagingCursor(c);
		SimpleCursorAdapter bills = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1,
				c, new String[] {DbAdapter.KEY_LONG_NAME},
				new int[] {android.R.id.text1});
		setListAdapter(bills);
	}

	private void init_click()
	{
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int pos, long id) {
				Cursor c = mDbHelper.fetch_url(id);
				startManagingCursor(c);
				if (c.moveToFirst()) {
					String url = c.getString(c.getColumnIndex(DbAdapter.KEY_URL));
					view_pdf(url);
				}
			}
		});
	}

	private void view_pdf(String uri_str)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse(GOOGLE_DOCS_URL + uri_str), "text/html");
		startActivity(Intent.createChooser(intent, "Open Web Browser"));
	}
}

