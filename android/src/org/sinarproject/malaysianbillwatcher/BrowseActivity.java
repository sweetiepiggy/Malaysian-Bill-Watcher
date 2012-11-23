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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BrowseActivity extends ListActivity {
	private DbAdapter mDbHelper;
	private String m_bill_name;
	private String m_status = "";
	private boolean m_favorite = false;
	private Calendar m_after_date;
	private Calendar m_before_date;

	private static final int BG_COLOR_READ = 0x00000000;
	private static final int BG_COLOR_UNREAD = 0x804671D5;

	private class BrowseBillListAdapter extends ResourceCursorAdapter
	{
		public BrowseBillListAdapter(Context context, Cursor c)
		{
			super(context, R.layout.bill_row, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor c)
		{
			TextView name_v = (TextView) view.findViewById(R.id.name);
			TextView status_v = (TextView) view.findViewById(R.id.status);
			ImageView fav_v = (ImageView) view.findViewById(R.id.fav);

			final int row_id = c.getInt(c.getColumnIndex(DbAdapter.KEY_ROWID));
			String long_name = c.getString(c.getColumnIndex(DbAdapter.KEY_LONG_NAME));
			String status = c.getString(c.getColumnIndex(DbAdapter.KEY_STATUS));
			boolean read = c.getInt(c.getColumnIndex(DbAdapter.KEY_READ)) != 0;
			boolean fav = c.getInt(c.getColumnIndex(DbAdapter.KEY_FAV)) != 0;

			name_v.setText(long_name);
			status_v.setText(status);
			fav_v.setImageResource(fav ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
			view.setBackgroundColor(read ? BG_COLOR_READ : BG_COLOR_UNREAD);

			fav_v.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					try {
						DbAdapter dbHelper = new DbAdapter();
						dbHelper.open_readwrite(BrowseActivity.this);
						boolean fav = dbHelper.get_fav(row_id);
						fav = !fav;
						dbHelper.set_fav(row_id, fav);
						dbHelper.close();
						((ImageView)v).setImageResource(fav ? android.R.drawable.star_big_on :
							android.R.drawable.star_big_off);
					/* database might be locked when trying to open it read/write */
					} catch (SQLiteException e) {
					}
				}
			});
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		m_bill_name = (b == null) ? "" : b.getString("bill_name");
		m_status = (b == null) ? "" : b.getString("status");
		m_favorite = (b == null) ? false : b.getBoolean("favorite");

		/* TODO: use status and favorite with SearchManager */
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			m_bill_name = intent.getStringExtra(SearchManager.QUERY);
		}

		final Calendar now = Calendar.getInstance();
		m_after_date = new GregorianCalendar();
		m_before_date = new GregorianCalendar();
		if (b == null || !b.containsKey("after_year") ||
				!b.containsKey("after_month") ||
				!b.containsKey("after_day") ||
				!b.containsKey("before_year") ||
				!b.containsKey("before_month") ||
				!b.containsKey("before_day")) {
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

		fill_data(m_bill_name, m_status, m_favorite, m_after_date,
				m_before_date);
		init_click();
	}

	@Override
	protected void onDestroy()
	{
		if (mDbHelper != null) {
			mDbHelper.close();
		}
		super.onDestroy();
	}

	private void fill_data(String bill_name, String status, boolean fav,
			Calendar after_date, Calendar before_date)
	{
		Cursor c = mDbHelper.fetch_revs(bill_name, status, fav, after_date,
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
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.browse_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		DbAdapter dbHelper;
		int pos;
		try { switch (item.getItemId()) {
		case R.id.mark_all_read:
			dbHelper = new DbAdapter();
			dbHelper.open_readwrite(BrowseActivity.this);
			dbHelper.set_read(true, m_bill_name, m_status,
					m_after_date, m_before_date);
			dbHelper.close();
			pos = getListView().getFirstVisiblePosition();
			fill_data(m_bill_name, m_status, m_favorite,
					m_after_date, m_before_date);
			getListView().setSelection(pos);
			return true;
		case R.id.mark_all_unread:
			dbHelper = new DbAdapter();
			dbHelper.open_readwrite(BrowseActivity.this);
			dbHelper.set_read(false, m_bill_name, m_status,
					m_after_date, m_before_date);
			dbHelper.close();
			pos = getListView().getFirstVisiblePosition();
			fill_data(m_bill_name, m_status, m_favorite,
					m_after_date, m_before_date);
			getListView().setSelection(pos);
			return true;
		default:
		/* database might be locked when trying to open it read/write */
		} } catch (SQLiteException e) {
		}
		return super.onOptionsItemSelected(item);
	}

}

