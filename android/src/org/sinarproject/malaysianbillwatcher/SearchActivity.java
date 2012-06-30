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
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class SearchActivity extends Activity
{
	private DbAdapter mDbHelper;
	private String m_status = "";

	private int m_before_year;
	private int m_before_month;
	private int m_before_day;
	private int m_after_year;
	private int m_after_month;
	private int m_after_day;

	static final int BEFORE_DATE_DIALOG_ID = 0;
	static final int AFTER_DATE_DIALOG_ID = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);

		mDbHelper = new DbAdapter();
		/* TODO: close() */
		mDbHelper.open(this);

		init_date_buttons();
		init_status_spinner();
		init_search_button();
	}

	private void init_date_buttons()
	{
		init_date_button(R.id.after_date_button, AFTER_DATE_DIALOG_ID);
		init_date_button(R.id.before_date_button, BEFORE_DATE_DIALOG_ID);

		final Calendar cal = Calendar.getInstance();
		m_before_year = cal.get(Calendar.YEAR);
		m_before_month = cal.get(Calendar.MONTH);
		m_before_day = cal.get(Calendar.DAY_OF_MONTH);

		Cursor c = mDbHelper.fetch_first_update();
		startManagingCursor(c);
		if (c.moveToFirst()) {
			/* TODO: check that correct number of columns are returned */
			m_after_year = c.getInt(1);
			m_after_month = c.getInt(2);
			m_after_day = c.getInt(3);
		} else {
			m_after_year = m_before_year;
			m_after_month = m_before_month;
			m_after_day = m_before_day;
		}

		update_date_label(R.id.before_date_button, m_before_year, m_before_month,
				m_before_day);
		update_date_label(R.id.after_date_button, m_after_year, m_after_month,
				m_after_day);
	}

	private void init_date_button(int button_id, final int dialog_id)
	{
		Button date_button = (Button)findViewById(button_id);
		date_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(dialog_id);
			}
		});

	}

	private void update_date_label(int button_id, int year, int month, int day)
	{
		Button date_button = (Button) findViewById(button_id);
		Date d = new Date(year - 1900, month, day);

		String date = DateFormat.getLongDateFormat(getApplicationContext()).format(d);
		date_button.setText(date);
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
				b.putInt("before_year", m_before_year);
				b.putInt("before_month", m_before_month);
				b.putInt("before_day", m_before_day);
				b.putInt("after_year", m_after_year);
				b.putInt("after_month", m_after_month);
				b.putInt("after_day", m_after_day);
				intent.putExtras(b);
				startActivity(intent);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		DatePickerDialog.OnDateSetListener before_date_listener =
			new DatePickerDialog.OnDateSetListener() {
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					m_before_year = year;
					m_before_month = monthOfYear;
					m_before_day = dayOfMonth;
					update_date_label(R.id.before_date_button,
							m_before_year, m_before_month,
							m_before_day);
				}
		};
		DatePickerDialog.OnDateSetListener after_date_listener =
			new DatePickerDialog.OnDateSetListener() {
				public void onDateSet(DatePicker view, int year,
						int monthOfYear, int dayOfMonth) {
					m_after_year = year;
					m_after_month = monthOfYear;
					m_after_day = dayOfMonth;
					update_date_label(R.id.after_date_button,
							m_after_year, m_after_month,
							m_after_day);
				}
		};

		switch (id) {
		case BEFORE_DATE_DIALOG_ID:
			return new DatePickerDialog(this, before_date_listener,
					m_before_year, m_before_month, m_before_day);
		case AFTER_DATE_DIALOG_ID:
			return new DatePickerDialog(this, after_date_listener,
					m_after_year, m_after_month, m_after_day);
		}
		return null;
	}
}

