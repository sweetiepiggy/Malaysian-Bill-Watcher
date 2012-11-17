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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter
{
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LONG_NAME = "long_name";
	public static final String KEY_STATUS = "status";
	public static final String KEY_UPDATE_DATE = "update_date";
	public static final String KEY_DATE_PRESENTED = "date_presented";
	public static final String KEY_READ_BY = "read_by";
	public static final String KEY_URL = "url";
	public static final String KEY_SUPPORTED_BY = "supported_by";
	public static final String KEY_YEAR = "year";
	public static final String KEY_NAME = "name";
	public static final String KEY_READ = "read";

	private static final String TAG = "DbAdapter";

	private DatabaseHelper mDbHelper;

	private static final String DATABASE_NAME = "data.db";
	private static final String DATABASE_TABLE = "data";
	private static final int DATABASE_VERSION = 3;

	private static final String DATABASE_CREATE =
		"CREATE TABLE " + DATABASE_TABLE + " (" +
		KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		KEY_NAME + " TEXT, " +
		KEY_LONG_NAME + " TEXT, " +
		KEY_URL + " TEXT, " +
		KEY_STATUS + " TEXT, " +
		KEY_YEAR + " TEXT, " +
		KEY_READ_BY + " TEXT, " +
		KEY_SUPPORTED_BY + " TEXT, " +
		KEY_DATE_PRESENTED + " TEXT, " +
		KEY_UPDATE_DATE + " TEXT," +
		KEY_READ + " INTEGER DEFAULT 0);";

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		private final Context mCtx;
		private boolean mAllowSync;
		public SQLiteDatabase mDb;

		DatabaseHelper(Context context, boolean allow_sync)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mCtx = context;
			mAllowSync = allow_sync;
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			db.execSQL(DATABASE_CREATE);
			if (mAllowSync) {
				SyncTask sync = new SyncTask(mCtx);
				sync.execute();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int old_ver, int new_ver)
		{
			switch (old_ver) {
			/* ver 2 added KEY_READ */
			case 1:
				db.execSQL("ALTER TABLE " + DATABASE_TABLE +
						" ADD COLUMN " + KEY_READ +
						" INTEGER DEFAULT 0");
			/* ver 3 removed KEY_CREATE_DATE */
			case 2:
				break;
			default:
				onCreate(db);
				break;
			}
		}

		public void open_database(int perm) throws SQLException
		{
			mDb = (perm == SQLiteDatabase.OPEN_READWRITE) ?
				getWritableDatabase() : getReadableDatabase();
		}

		@Override
		public synchronized void close()
		{
			if (mDb != null) {
				mDb.close();
			}
			super.close();
		}
	}

	public DbAdapter()
	{
	}

	public DbAdapter open(Context ctx) throws SQLException
	{
		return open(ctx, SQLiteDatabase.OPEN_READONLY, true);
	}

	public DbAdapter open_no_sync(Context ctx) throws SQLException
	{
		return open(ctx, SQLiteDatabase.OPEN_READONLY, false);
	}

	public DbAdapter open_readwrite(Context ctx) throws SQLException
	{
		return open(ctx, SQLiteDatabase.OPEN_READWRITE, true);
	}

	private DbAdapter open(Context ctx, int perm, boolean allow_sync) throws SQLException
	{
		mDbHelper = new DatabaseHelper(ctx, allow_sync);
		mDbHelper.open_database(perm);

		return this;
	}

	public void close()
	{
		mDbHelper.close();
	}

	/** @return row_id or -1 if failed */
	public long create_bill(String long_name, String year, String status,
			String url, String name, String read_by,
			String supported_by, String date_presented,
			String update_date)
	{
		long ret = -1;

		ContentValues initial_values = new ContentValues();
		initial_values.put(KEY_LONG_NAME, long_name);
		initial_values.put(KEY_YEAR, year);
		initial_values.put(KEY_STATUS, status);
		initial_values.put(KEY_URL, url);
		initial_values.put(KEY_NAME, name);
		initial_values.put(KEY_READ_BY, read_by);
		initial_values.put(KEY_SUPPORTED_BY, supported_by);
		initial_values.put(KEY_DATE_PRESENTED, date_presented);
		initial_values.put(KEY_UPDATE_DATE, update_date);
		initial_values.put(KEY_READ, 0);

		Cursor c = fetch_bill(long_name, name);
		/* bill already exists, just update it */
		if (c.moveToFirst()) {
			long row_id = c.getLong(c.getColumnIndex(KEY_ROWID));
			mDbHelper.mDb.update(DATABASE_TABLE, initial_values,
				KEY_ROWID + " = ?",
				new String[] {Long.toString(row_id)});

		/* create new bill */
		} else {
			ret =  mDbHelper.mDb.insert(DATABASE_TABLE, null,
				initial_values);
		}

		return ret;
	}

	/** @return true on success, false on failure */
	public boolean set_read(long row_id, boolean read)
	{
		ContentValues values = new ContentValues();
		values.put(KEY_READ, read ? 1 : 0);

		int rows_affected = mDbHelper.mDb.update(DATABASE_TABLE, values,
			KEY_ROWID + " = ?",
			new String[] {Long.toString(row_id)});

		return rows_affected == 1;
	}

	public Cursor fetch_bills()
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_ROWID, KEY_LONG_NAME, KEY_STATUS},
				null, null, null, null,
				KEY_UPDATE_DATE + " DESC ", null);
	}

	public Cursor fetch_bills(String bill_name, String status,
			Calendar after_date, Calendar before_date)
	{
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String before = df.format(before_date.getTime());
		String after = df.format(after_date.getTime());

		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_ROWID, KEY_LONG_NAME, KEY_STATUS, KEY_READ},
				KEY_LONG_NAME + " LIKE ? AND " +
					"(" + KEY_STATUS + " = ? OR \"\" = ?) AND " +
					KEY_UPDATE_DATE + " < " + "? AND " +
					KEY_UPDATE_DATE + " > " + "?",
				new String[] {"%" + bill_name + "%", status,
					status, before, after},
				null, null,
				KEY_UPDATE_DATE + " DESC", null);
	}

	public Cursor fetch_bill(String long_name, String name)
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_ROWID},
				KEY_LONG_NAME + " = ? AND " + KEY_NAME + " = ?",
				new String[] {long_name, name},
				null, null, null);
	}


	public Cursor fetch_revs(String long_name)
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_URL, KEY_STATUS, KEY_YEAR,
					KEY_NAME, KEY_DATE_PRESENTED,
					KEY_READ_BY, KEY_SUPPORTED_BY},
				KEY_LONG_NAME + " = ?", new String[] {long_name},
				null, null,
				KEY_UPDATE_DATE + " DESC ", null);
	}

	public Cursor fetch_bill(long id)
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_URL, KEY_LONG_NAME, KEY_STATUS, KEY_YEAR, KEY_NAME},
				KEY_ROWID + " = ?", new String[] {Long.toString(id)},
				null, null, null, null);
	}

	public Cursor fetch_last_update()
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_ROWID, KEY_UPDATE_DATE},
				null, null, null, null,
				KEY_UPDATE_DATE + " DESC LIMIT 1", null);
	}

	public Cursor fetch_first_update()
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_ROWID,
					"strftime(\"%Y\", " + KEY_UPDATE_DATE + ")",
					"strftime(\"%m\", " + KEY_UPDATE_DATE + ")",
					"strftime(\"%d\", " + KEY_UPDATE_DATE + ")",
				},
				null, null, null, null,
				KEY_UPDATE_DATE + " ASC LIMIT 1", null);
	}

	public Cursor fetch_status()
	{
		return mDbHelper.mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_STATUS},
				"length(" + KEY_STATUS + ") != 0", null,
				KEY_STATUS, null, KEY_STATUS + " ASC", null);
	}
}

