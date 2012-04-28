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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;


public class DbAdapter
{
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LONG_NAME = "long_name";
	public static final String KEY_STATUS = "status";
	public static final String KEY_UPDATE_DATE = "update_date";
	public static final String KEY_CREATE_DATE = "create_date";
	public static final String KEY_DATE_PRESENTED = "date_presented";
	public static final String KEY_READ_BY = "read_by";
	public static final String KEY_URL = "url";
	public static final String KEY_SUPPORTED_BY = "supported_by";
	public static final String KEY_YEAR = "year";

	private DatabaseHelper mDbHelper;

	private static final String DATABASE_PATH = "/data/data/org.sinarproject.malaysianbillwatcher/databases/";
	private static final String DATABASE_NAME = "data.db";
	private static final String DATABASE_TABLE = "data";
	private static final int DATABASE_VERSION = 1;

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		private final Context mCtx;
		public SQLiteDatabase mDb;

		DatabaseHelper(Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mCtx = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
		}

		public void create_database() throws IOException
		{
			if (!database_exists()) {
				this.getReadableDatabase();
				try {
					copy_database();
				} catch (IOException e) {
					throw new Error(e);
				}
			}
		}

		private boolean database_exists()
		{
			SQLiteDatabase db = null;
			try {
				String out_filename = DATABASE_PATH + DATABASE_NAME;
				db = SQLiteDatabase.openDatabase(out_filename, null, SQLiteDatabase.OPEN_READONLY);
			} catch (SQLiteException e) {
				/* database does not exist yet */
			}
			if (db != null) {
				db.close();
			}
			return db != null;
		}

		private void copy_database() throws IOException
		{
			InputStream input = mCtx.getAssets().open(DATABASE_NAME);

			String full_path = DATABASE_PATH + DATABASE_NAME;

			OutputStream output = new FileOutputStream(full_path);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = input.read(buffer))>0){
				output.write(buffer, 0, length);
			}

			output.flush();
			output.close();
			input.close();
		}

		public void open_database(int perm) throws SQLException
		{
			String full_path = DATABASE_PATH + DATABASE_NAME;
			mDb = SQLiteDatabase.openDatabase(full_path, null, perm);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
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
		return open(ctx, SQLiteDatabase.OPEN_READONLY);
	}

	public DbAdapter open_readwrite(Context ctx) throws SQLException
	{
		return open(ctx, SQLiteDatabase.OPEN_READWRITE);
	}

	private DbAdapter open(Context ctx, int perm) throws SQLException
	{
		mDbHelper = new DatabaseHelper(ctx);

		try {
			mDbHelper.create_database();
		} catch (IOException e) {
			throw new Error(e);
		}

		mDbHelper.open_database(perm);

		return this;
	}

	public void close()
	{
		mDbHelper.close();
	}

	public Cursor fetch_bills()
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_ROWID, KEY_LONG_NAME},
				null, null, null, null,
				"strftime(" + KEY_UPDATE_DATE + ") DESC LIMIT 10", null);
	}

	public Cursor fetch_url(long id)
	{
		return mDbHelper.mDb.query(DATABASE_TABLE,
				new String[] {KEY_URL},
				KEY_ROWID + " = ?", new String[] {Long.toString(id)},
				null, null, null, null);
	}

}

