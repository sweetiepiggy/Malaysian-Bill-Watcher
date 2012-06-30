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

import java.util.Calendar;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class BrowseActivity extends ListActivity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DbAdapter dbHelper = new DbAdapter();

		Bundle b = getIntent().getExtras();
		String bill_name = (b == null) ? "" : b.getString("bill_name");
		String status = (b == null) ? "" : b.getString("status");

		final Calendar cal = Calendar.getInstance();
		int before_year = (b == null) ? cal.get(Calendar.YEAR) : b.getInt("before_year");
		int before_month = (b == null) ? cal.get(Calendar.MONTH) : b.getInt("before_month");
		int before_day = (b == null) ? cal.get(Calendar.DAY_OF_MONTH) : b.getInt("before_day");
		int after_year = (b == null) ? 1970 : b.getInt("after_year");
		int after_month = (b == null) ? 0 : b.getInt("after_month");
		int after_day = (b == null) ? 1 : b.getInt("after_day");

		/* TODO: should adapter be closed? */
		dbHelper.open(this);

		fill_data(dbHelper, bill_name, status, before_year,
				before_month, before_day, after_year,
				after_month, after_day);
		init_click();
	}

	private void fill_data(DbAdapter dbHelper, String bill_name,
			String status, int before_year, int before_month,
			int before_day, int after_year, int after_month,
			int after_day)
	{
		Cursor c = dbHelper.fetch_bills(bill_name, status, before_year,
				before_month, before_day, after_year,
				after_month, after_day);
		startManagingCursor(c);
		SimpleCursorAdapter bills = new SimpleCursorAdapter(this,
				android.R.layout.two_line_list_item,
				c, new String[] {DbAdapter.KEY_LONG_NAME, DbAdapter.KEY_STATUS},
				new int[] {android.R.id.text1, android.R.id.text2});
		setListAdapter(bills);
	}

	private void init_click()
	{
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int pos, long id) {
				Intent intent = new Intent(getApplicationContext(), ViewBillActivity.class);
				Bundle b = new Bundle();
				b.putLong("row_id", id);
				intent.putExtras(b);
				startActivity(intent);
			}
		});
	}
}

