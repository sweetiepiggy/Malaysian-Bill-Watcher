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
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MalaysianBillWatcherActivity extends Activity {
	private static final String SOURCE_URL = "https://github.com/sweetiepiggy/Malaysian-Bill-Watcher/tree/sp/android";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		init();
		try {
			/* open database only to sync if it has not been created yet */
			DbAdapter dbHelper = new DbAdapter();
			dbHelper.open_readwrite(this);
			dbHelper.close();
		/* database might be locked when trying to open it read/write */
		} catch (SQLiteException e) {
		}

		Intent intent = new Intent(this, BillWatcherService.class);
		startService(intent);
	}

	private void init()
	{
		setContentView(R.layout.main);
		TextView browse = (TextView) findViewById(R.id.browse);
		browse.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				Intent intent = new Intent(getApplicationContext(), BrowseActivity.class);
				startActivity(intent);
			}
		});

		TextView search = (TextView) findViewById(R.id.search);
		search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.sync:
			SyncTask sync = new SyncTask(MalaysianBillWatcherActivity.this);
			sync.execute();
			return true;
		case R.id.about:
			Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
			startActivity(intent);
			return true;
		case R.id.source:
			Intent source_intent = new Intent(Intent.ACTION_VIEW);
			source_intent.setDataAndType(Uri.parse(SOURCE_URL), "text/html");
			startActivity(Intent.createChooser(source_intent, null));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}

