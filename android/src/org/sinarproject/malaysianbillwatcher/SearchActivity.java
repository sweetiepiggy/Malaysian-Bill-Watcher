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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class SearchActivity extends Activity
{
	private DbAdapter mDbHelper;
	private String m_status = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);

		mDbHelper = new DbAdapter();
		/* TODO: close() */
		mDbHelper.open(this);

		init_status_spinner();
		init_search_button();
	}

	private void init_status_spinner()
	{
		Cursor c = mDbHelper.fetch_status();
		startManagingCursor(c);
		SimpleCursorAdapter status = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item,
				c, new String[] {DbAdapter.KEY_STATUS},
				new int[] {android.R.id.text1});
		Spinner status_spinner = (Spinner) findViewById(R.id.status_spinner);
		status_spinner.setAdapter(status);

		status_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent,
					View selected_item, int pos,
					long id)
			{
				m_status = ((Cursor)parent.getItemAtPosition(pos)).getString(1);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView)
			{
			}
		});
	}

	private void init_search_button()
	{
		Button search_button = (Button) findViewById(R.id.search_button);
		search_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				Intent intent = new Intent(getApplicationContext(), BrowseActivity.class);
				Bundle b = new Bundle();
				b.putString("bill_name", ((EditText) findViewById(R.id.bill_name_entry)).getText().toString());
				b.putString("status", m_status);
				intent.putExtras(b);
				startActivity(intent);
			}
		});
	}
}

