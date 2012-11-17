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
import java.util.GregorianCalendar;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class BrowseActivity extends ListActivity {
	DbAdapter mDbHelper;
	String m_bill_name;
	String m_status;
	Calendar m_after_date;
	Calendar m_before_date;

	private class BrowseBillListAdapter extends ResourceCursorAdapter {
		public BrowseBillListAdapter(Context context, Cursor c) {
			super(context, android.R.layout.two_line_list_item, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor c) {
			TextView text1 = (TextView) view.findViewById(android.R.id.text1);
			TextView text2 = (TextView) view.findViewById(android.R.id.text2);

			String long_name = c.getString(c.getColumnIndex(DbAdapter.KEY_LONG_NAME));
			String status = c.getString(c.getColumnIndex(DbAdapter.KEY_STATUS));
			boolean read = c.getInt(c.getColumnIndex(DbAdapter.KEY_READ)) != 0;

			text1.setText(long_name);
			text2.setText(status);

			text1.setTextAppearance(context, android.R.style.TextAppearance_Large);
			view.setBackgroundColor(read ? 0x00000000 : 0x804671D5);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle b = getIntent().getExtras();
		m_bill_name = (b == null) ? "" : b.getString("bill_name");
		m_status = (b == null) ? "" : b.getString("status");

		final Calendar now = Calendar.getInstance();
		m_after_date = new GregorianCalendar();
		m_before_date = new GregorianCalendar();
		if (b == null) {
			m_after_date.set(1970, 0, 1, 0, 0);
			m_before_date.set(now.get(Calendar.YEAR),
					now.get(Calendar.MONTH),
					now.get(Calendar.DAY_OF_MONTH),
					23, 59);
		} else {
			m_after_date.set(b.getInt("after_year"),
					b.getInt("after_month"),
					b.getInt("after_day"),
					0, 0);
			m_before_date.set(b.getInt("before_year"),
					b.getInt("before_month"),
					b.getInt("before_day"),
					23, 59);
		}

		mDbHelper = new DbAdapter();
		mDbHelper.open(this);

		fill_data(m_bill_name, m_status, m_after_date, m_before_date);
		init_click();
	}

	@Override
	protected void onDestroy() {
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		super.onDestroy();
	}

	private void fill_data(String bill_name, String status,
			Calendar after_date, Calendar before_date)
	{
		Cursor c = mDbHelper.fetch_revs(bill_name, status, after_date,
				before_date);
		startManagingCursor(c);
		BrowseBillListAdapter revs = new BrowseBillListAdapter(this, c);
		setListAdapter(revs);
	}

	private void init_click()
	{
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int pos, long id) {
				Intent intent = new Intent(getApplicationContext(),
					ViewBillActivity.class);
				Bundle b = new Bundle();
				b.putLong("row_id", id);
				intent.putExtras(b);
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.browse_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		DbAdapter dbHelper;
		switch (item.getItemId()) {
		/* TODO: refresh row colors */
		case R.id.mark_all_read:
			dbHelper = new DbAdapter();
			dbHelper.open_readwrite(BrowseActivity.this);
			dbHelper.set_read(true, m_bill_name, m_status,
					m_after_date, m_before_date);
			dbHelper.close();
			return true;
		case R.id.mark_all_unread:
			dbHelper = new DbAdapter();
			dbHelper.open_readwrite(BrowseActivity.this);
			dbHelper.set_read(false, m_bill_name, m_status,
					m_after_date, m_before_date);
			dbHelper.close();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}

