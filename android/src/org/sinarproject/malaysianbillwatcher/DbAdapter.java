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
	private static final String ACCEPTED_EN = "Accepted";
	private static final String ACCEPTED_MS = "Diterima";
	private static final String ACCEPTED_ZH = "被接纳";
	private static final String SECOND_AND_THIRD_EN = "Second And Third Reading";
	private static final String SECOND_AND_THIRD_MS = "Bacaan Kedua dan Ketiga";
	private static final String SECOND_AND_THIRD_ZH = "第二次和第三次宣读";
	private static final String WITHDRAWN_EN = "Withdrawn";
	private static final String WITHDRAWN_MS = "Ditarik Balik";
	private static final String WITHDRAWN_ZH = "撤回";

	public static final String KEY_ROWID = "_id";
	public static final String KEY_LONG_NAME = "long_name";
	public static final String KEY_STATUS_ID = "status_id";
	public static final String KEY_STATUS = "status";
	public static final String KEY_STATUS_EN = "status_en";
	public static final String KEY_STATUS_MS = "status_ms";
	public static final String KEY_STATUS_ZH = "status_zh";
	public static final String KEY_UPDATE_DATE = "update_date";
	public static final String KEY_DATE_PRESENTED = "date_presented";
	public static final String KEY_READ_BY = "read_by";
	public static final String KEY_URL = "url";
	public static final String KEY_SINAR_URL = "sinar_url";
	public static final String KEY_SUPPORTED_BY = "supported_by";
	public static final String KEY_YEAR = "year";
	public static final String KEY_NAME = "name";
	public static final String KEY_READ = "read";
	public static final String KEY_FAV = "favorite";
	public static final String KEY_BILL_ID = "bill_id";

	private static final String TAG = "DbAdapter";

	private DatabaseHelper mDbHelper;

	private static final String DATABASE_NAME = "data.db";
	private static final String TABLE_BILLS = "bills";
	private static final String TABLE_REVS = "bill_revs";
	private static final String TABLE_STATUS = "status";
	private static final int DATABASE_VERSION = 5;

	private static final String DATABASE_CREATE_BILLS =
		"CREATE TABLE " + TABLE_BILLS + " (" +
		KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		KEY_NAME + " TEXT UNIQUE, " +
		KEY_LONG_NAME + " TEXT);";

	private static final String DATABASE_CREATE_REVS =
		"CREATE TABLE " + TABLE_REVS + " (" +
		KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		KEY_BILL_ID + " INTEGER, " +
		KEY_URL + " TEXT, " +
		KEY_SINAR_URL + " TEXT, " +
		KEY_STATUS_ID + " TEXT, " +
		KEY_YEAR + " TEXT, " +
		KEY_READ_BY + " TEXT, " +
		KEY_SUPPORTED_BY + " TEXT, " +
		KEY_DATE_PRESENTED + " TEXT, " +
		KEY_UPDATE_DATE + " TEXT," +
		KEY_READ + " INTEGER DEFAULT 0," +
		KEY_FAV + " INTEGER DEFAULT 0," +
		"FOREIGN KEY(" + KEY_BILL_ID + ") REFERENCES " + TABLE_BILLS + "(" + KEY_ROWID + "), " +
		"FOREIGN KEY(" + KEY_STATUS_ID + ") REFERENCES " + TABLE_STATUS + "(" + KEY_ROWID + "));";

	private static final String DATABASE_CREATE_STATUS =
		"CREATE TABLE " + TABLE_STATUS + " (" +
		KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		KEY_STATUS_EN + " TEXT, " +
		KEY_STATUS_MS + " TEXT, " +
		KEY_STATUS_ZH + " TEXT);";

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

		private void init_status_table(SQLiteDatabase db)
		{
			create_status(db, ACCEPTED_EN, ACCEPTED_MS,
					ACCEPTED_ZH);
			create_status(db, SECOND_AND_THIRD_EN,
					SECOND_AND_THIRD_MS,
					SECOND_AND_THIRD_ZH);
			create_status(db, WITHDRAWN_EN, WITHDRAWN_MS,
					WITHDRAWN_ZH);
		}

		/** @return row_id or -1 if failed */
		private long create_status(SQLiteDatabase db, String status_en,
				String status_ms, String status_zh)
		{
			ContentValues cv = new ContentValues();
			cv.put(KEY_STATUS_EN, status_en);
			cv.put(KEY_STATUS_MS, status_ms);
			cv.put(KEY_STATUS_ZH, status_zh);

			return db.insert(TABLE_STATUS, null, cv);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_REVS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATUS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILLS);
			db.execSQL(DATABASE_CREATE_BILLS);
			db.execSQL(DATABASE_CREATE_STATUS);
			init_status_table(db);
			db.execSQL(DATABASE_CREATE_REVS);
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
			/* ver 3 removed KEY_CREATE_DATE */
			/* ver 4 renamed split data table into bills, bill_revs, and status tables */
			/* ver 5 added KEY_SINAR_URL */
			/* drop tables and recreate from scratch */
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

	/** @return TABLE_REVS row_id or -1 if failed */
	public long create_bill_rev(String long_name, String year,
			String status, String url, String sinar_url,
			String name, String read_by, String supported_by,
			String date_presented, String update_date)
	{
		long bill_id = create_bill(long_name, name);
		if (bill_id == -1) {
			return -1;
		}

		long status_id = fetch_or_create_status(status);
		if (status_id == -1) {
			return -1;
		}

		return create_rev(year, bill_id, status_id, url, sinar_url,
				read_by, supported_by, date_presented,
				update_date);
	}

	/** @return row_id or -1 if failed */
	private long create_bill(String long_name, String name)
	{
		ContentValues cv = new ContentValues();
		cv.put(KEY_LONG_NAME, long_name);
		cv.put(KEY_NAME, name);
		return mDbHelper.mDb.replace(TABLE_BILLS, null, cv);
	}

	/** @return row_id or -1 if failed */
	private long create_rev(String year, long bill_id, long status_id,
			String url, String sinar_url, String read_by,
			String supported_by, String date_presented,
			String update_date)
	{
		ContentValues cv = new ContentValues();
		cv.put(KEY_YEAR, year);
		cv.put(KEY_BILL_ID, Long.toString(bill_id));
		cv.put(KEY_STATUS_ID, Long.toString(status_id));
		cv.put(KEY_URL, url);
		cv.put(KEY_SINAR_URL, sinar_url);
		cv.put(KEY_READ_BY, read_by);
		cv.put(KEY_SUPPORTED_BY, supported_by);
		cv.put(KEY_DATE_PRESENTED, date_presented);
		cv.put(KEY_UPDATE_DATE, update_date);
		cv.put(KEY_READ, 0);

		return mDbHelper.mDb.insert(TABLE_REVS, null, cv);
	}

	/** @return row_id or -1 if failed */
	private long fetch_or_create_status(String status_en)
	{
		long ret = -1;

		Cursor c = mDbHelper.mDb.query(TABLE_STATUS,
				new String[] {KEY_ROWID},
				KEY_STATUS_EN + " = ?", new String[] {status_en},
				null, null, null, "1");
		if (c.moveToFirst()) {
			ret = c.getInt(c.getColumnIndex(KEY_ROWID));
		} else {
			ContentValues cv = new ContentValues();
			cv.put(KEY_STATUS_EN, status_en);
			cv.put(KEY_STATUS_MS, status_en);
			cv.put(KEY_STATUS_ZH, status_en);

			ret = mDbHelper.mDb.insert(TABLE_STATUS, null, cv);
		}
		c.close();

		return ret;
	}

	/** @return true on success, false on failure */
	public boolean set_read(long row_id, boolean read)
	{
		ContentValues values = new ContentValues();
		values.put(KEY_READ, read ? 1 : 0);

		int rows_affected = mDbHelper.mDb.update(TABLE_REVS, values,
			KEY_ROWID + " = ?",
			new String[] {Long.toString(row_id)});

		return rows_affected == 1;
	}

	public void set_read(boolean read, String bill_name, String status,
			Calendar after_date, Calendar before_date)
	{
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String before = df.format(before_date.getTime());
		String after = df.format(after_date.getTime());
		String key_status = KEY_STATUS + "_" +
			mDbHelper.mCtx.getResources().getString(R.string.lang_code);
		String any = mDbHelper.mCtx.getResources().getString(R.string.any);

		Cursor c = mDbHelper.mDb.rawQuery("UPDATE " + TABLE_REVS + " SET " +
				KEY_READ + " = " + (read ? "1" : "0") +
				" WHERE " + KEY_ROWID + " IN " +
				"(SELECT " + TABLE_REVS +
				"." + KEY_ROWID + " AS " + KEY_ROWID +
				" FROM " + TABLE_REVS + " JOIN " + TABLE_BILLS +
				" ON " + TABLE_REVS + "." + KEY_BILL_ID + " == " +
				TABLE_BILLS + "." + KEY_ROWID + " JOIN " +
				TABLE_STATUS + " ON " + TABLE_REVS + "." +
				KEY_STATUS_ID + " == " + TABLE_STATUS + "." +
				KEY_ROWID +
				" WHERE " + KEY_LONG_NAME + " LIKE ? AND " +
					"(" + key_status + " = ? OR \"\" = ? OR \"" + any + "\" = ?) AND " +
					KEY_UPDATE_DATE + " < " + "? AND " +
					KEY_UPDATE_DATE + " > " + "?)",
			new String[] {"%" + bill_name + "%", status, status,
				status, before, after});

		/* need to interact and close cursor in order for UPDATE to
			take effect */
		c.moveToFirst();
		c.close();
	}

	public Cursor fetch_revs(String bill_name, String status,
			Calendar after_date, Calendar before_date)
	{
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String before = df.format(before_date.getTime());
		String after = df.format(after_date.getTime());
		String key_status = KEY_STATUS + "_" +
			mDbHelper.mCtx.getResources().getString(R.string.lang_code);
		String any = mDbHelper.mCtx.getResources().getString(R.string.any);

		return mDbHelper.mDb.rawQuery("SELECT " + TABLE_REVS +
				"." + KEY_ROWID + " AS " + KEY_ROWID + ", " +
				KEY_LONG_NAME + ", " + key_status + " AS " +
				KEY_STATUS + ", " + KEY_READ +
				" FROM " + TABLE_REVS + " JOIN " + TABLE_BILLS +
				" ON " + TABLE_REVS + "." + KEY_BILL_ID + " == " +
				TABLE_BILLS + "." + KEY_ROWID + " JOIN " +
				TABLE_STATUS + " ON " + TABLE_REVS + "." +
				KEY_STATUS_ID + " == " + TABLE_STATUS + "." +
				KEY_ROWID +
				" WHERE " + KEY_LONG_NAME + " LIKE ? AND " +
					"(" + key_status + " = ? OR \"\" = ? OR \"" + any + "\" = ?) AND " +
					KEY_UPDATE_DATE + " < " + "? AND " +
					KEY_UPDATE_DATE + " > " + "? " +
				"ORDER BY strftime('%s', " + KEY_UPDATE_DATE + ") DESC",
			new String[] {"%" + bill_name + "%", status, status,
				status, before, after});
	}

	public Cursor fetch_rev(long id)
	{
		String key_status = KEY_STATUS + "_" +
			mDbHelper.mCtx.getResources().getString(R.string.lang_code);
		return mDbHelper.mDb.rawQuery("SELECT " + TABLE_REVS +
				"." + KEY_ROWID + " AS " + KEY_ROWID + ", " +
				KEY_LONG_NAME + ", " + key_status + " AS " +
				KEY_STATUS + ", " + KEY_YEAR + ", " + KEY_URL + ", " +
				KEY_SINAR_URL + ", " + KEY_NAME + ", " +
				KEY_DATE_PRESENTED + ", " + KEY_READ_BY + ", " + KEY_SUPPORTED_BY +
				" FROM " + TABLE_REVS + " JOIN " + TABLE_BILLS +
				" ON " + TABLE_REVS + "." + KEY_BILL_ID + " == " +
				TABLE_BILLS + "." + KEY_ROWID + " JOIN " +
				TABLE_STATUS + " ON " + TABLE_REVS + "." +
				KEY_STATUS_ID + " == " + TABLE_STATUS + "." +
				KEY_ROWID +
				" WHERE " + TABLE_REVS + "." + KEY_ROWID + " = ?",
			new String[] {Long.toString(id)});
	}

	public String get_last_update()
	{
		Cursor c = mDbHelper.mDb.query(TABLE_REVS,
				new String[] {KEY_ROWID, KEY_UPDATE_DATE},
				null, null, null, null,
				"strftime('%s', " + KEY_UPDATE_DATE + ") DESC LIMIT 1",
				null);
		String ret = "1970-01-01 00:00:00";
		if (c.moveToFirst()) {
			ret = c.getString(c.getColumnIndex(KEY_UPDATE_DATE));
		}
		c.close();
		return ret;
	}

	public Cursor fetch_first_update()
	{
		return mDbHelper.mDb.query(TABLE_REVS,
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
		String key_status = KEY_STATUS + "_" +
			mDbHelper.mCtx.getResources().getString(R.string.lang_code);
		return mDbHelper.mDb.rawQuery("SELECT DISTINCT " + TABLE_REVS +
				"." + KEY_ROWID + " AS " + KEY_ROWID + ", " + key_status + " AS " + KEY_STATUS +
				" FROM " + TABLE_REVS + " JOIN " + TABLE_STATUS +
				" ON " + TABLE_REVS + "." + KEY_STATUS_ID + " == " +
				TABLE_STATUS + "." + KEY_ROWID +
				" GROUP BY " + key_status +
				" ORDER BY " + key_status + " ASC",
			null);
	}
}

