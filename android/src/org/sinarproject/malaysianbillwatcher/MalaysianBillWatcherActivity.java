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
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

public class MalaysianBillWatcherActivity extends ListActivity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DbAdapter dbHelper = new DbAdapter();
		dbHelper.open(this);

		fill_data(dbHelper);
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
}

