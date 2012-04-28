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
import android.widget.TextView;

public class ViewBillActivity extends Activity {
	String GOOGLE_DOCS_URL = "http://docs.google.com/viewer?url=";

	private String m_url = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);

		Bundle b = getIntent().getExtras();
		if (b == null) {
			b = new Bundle();
		}
		Long row_id = b.getLong("row_id");

		DbAdapter dbHelper = new DbAdapter();
		/* TODO: should adapter be closed? */
		dbHelper.open(this);

		Cursor c = dbHelper.fetch_bill(row_id);
		startManagingCursor(c);
		if (c.moveToFirst()) {
			String long_name = c.getString(c.getColumnIndex(DbAdapter.KEY_LONG_NAME));
			String code = c.getString(c.getColumnIndex(DbAdapter.KEY_NAME));
			String rev = c.getString(c.getColumnIndex(DbAdapter.KEY_YEAR));
			String status = c.getString(c.getColumnIndex(DbAdapter.KEY_STATUS));
			m_url = c.getString(c.getColumnIndex(DbAdapter.KEY_URL));

			((TextView) findViewById(R.id.long_name)).setText(long_name);
			((TextView) findViewById(R.id.code)).setText(code);
			((TextView) findViewById(R.id.rev)).setText(rev);
			((TextView) findViewById(R.id.status)).setText(status);

			init_submit_button();
		}
	}

	private void init_submit_button()
	{
		Button view_bill_button = (Button) findViewById(R.id.view_bill);
		view_bill_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				view_pdf(m_url);
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

