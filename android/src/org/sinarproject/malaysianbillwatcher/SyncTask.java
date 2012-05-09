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

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class SyncTask extends AsyncTask<Void, Void, Void>
{
	private final String TAG = "AsyncTask";
	private final String BILLWATCHER_URL = "http://billwatcher.sinarproject.org/feeds/";
	private Context mCtx;
	private int bill_cnt = 0;

	public SyncTask(Context ctx)
	{
		mCtx = ctx;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			URL url = new URL(BILLWATCHER_URL);

			RssHandler rss_handler = new RssHandler();
			xr.setContentHandler(rss_handler);
			xr.parse(new InputSource(url.openStream()));
		} catch (Exception e) {
			throw new Error(e);
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result)
	{
		Toast.makeText(mCtx,
				Integer.toString(bill_cnt) + " update(s) found",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onProgressUpdate(Void... values)
	{
	}

	private class RssHandler extends DefaultHandler
	{
		private Boolean done = false;
		private Boolean in_item = false;
		private String cur_chars = "";
		private String last_update = "1970-01-01 00:00:00";

		private String long_name = "";
		private String year = "";
		private String status = "";
		private String url = "";
		private String name = "";
		private String read_by = "";
		private String supported_by = "";
		private String date_presented = "";
		private String update_date = "";

		@Override
		public void startDocument()
		{
			DbAdapter dbHelper = new DbAdapter();
			dbHelper.open(mCtx);
			Cursor c = dbHelper.fetch_last_update();
			if (c.moveToFirst()) {
				last_update = c.getString(c.getColumnIndex(DbAdapter.KEY_UPDATE_DATE));
			}
			c.close();
			dbHelper.close();
			Log.i(TAG, "last_update:[" + last_update + "]");
		}

		@Override
		public void startElement(String uri, String local_name, String q_name,
				Attributes attr) throws SAXException
		{
			if (!done) {
				if (local_name.equalsIgnoreCase("item")) {
					in_item = true;
				}
			}
		}

		@Override
		public void endElement(String uri, String local_name, String q_name)
				throws SAXException
		{
			if (!done) {
				if (local_name.equalsIgnoreCase("item")) {
					/* TODO: should use strftime() first? */
					Log.i(TAG, "last_update:[" + last_update + "]");
					Log.i(TAG, "update_date:[" + update_date + "]");
					if (last_update.compareTo(update_date) < 0) {
						update_db();
					} else {
						done = true;
						Log.i(TAG, "done updating");
					}
					in_item = false;
					long_name = "";
					year = "";
					status = "";
					url = "";
					name = "";
					read_by = "";
					supported_by = "";
					date_presented = "";
					update_date = "";
				} else if (in_item) {
					//Log.i(TAG, "local_name:[" + local_name + "]");
					//Log.i(TAG, "cur_chars:[" + cur_chars + "]");

					if (local_name.equalsIgnoreCase("title")) {
						long_name = strip_ws(new String(cur_chars));
					} else if (local_name.equalsIgnoreCase("description")) {
						parse_description(cur_chars);
					} else if (local_name.equalsIgnoreCase("pubDate")) {
						update_date = format_date(strip_ws(new String(cur_chars)));
					}
				}
				cur_chars = "";
			}
		}

		@Override
		public void characters(char[] ch, int start, int len) throws SAXException {
			if (!done) {
				cur_chars += new String(ch, start, len);
			}
		}

		private void parse_description(String desc)
		{
			for (String line : desc.split("\n")) {
				int idx = line.indexOf(':');
				if (idx != -1 && idx < line.length() - 1) {
					String key = line.substring(0, idx);
					String val = line.substring(idx + 1);
					key = strip_ws(key);
					val = strip_ws(val);
					if (!val.equals("None")) {
						if (key.equalsIgnoreCase("year")) {
							year = val;
						} else if (key.equalsIgnoreCase("status")) {
							status = val;
						} else if (key.equalsIgnoreCase("url")) {
							url = val;
						} else if (key.equalsIgnoreCase("name")) {
							name = val;
						} else if (key.equalsIgnoreCase("read_by")) {
							read_by = val;
						} else if (key.equalsIgnoreCase("supported_by")) {
							supported_by = val;
						} else if (key.equalsIgnoreCase("date_presented")) {
							date_presented = val;
						}
					}
				}
			}
		}

		private String format_date(String date)
		{
			//Log.i(TAG, "in_date:[" + date + "]");

			String[] fields = date.split("\\s+");

			String ret;

			/* date looks like this: Sun, 04 Mar 2012 11:21:25 GMT */
			if (fields.length == 6) {
				String day = fields[1];
				String month = month_name2num(fields[2]);
				String year = fields[3];
				String time = fields[4];
				ret = year + "-" + month + "-" + day + " " + time;
			} else {
				//Log.i(TAG, "length:[" + fields.length + "]");
				ret = date;
			}

			//Log.i(TAG, "out_date:[" + ret + "]");

			return ret;
		}

		/* TODO: is there a library for this */
		private String month_name2num(String month_name)
		{
			if (month_name.equals("Jan")) {
				return "01";
			} else if (month_name.equals("Feb")) {
				return "02";
			} else if (month_name.equals("Mar")) {
				return "03";
			} else if (month_name.equals("Apr")) {
				return "04";
			} else if (month_name.equals("May")) {
				return "05";
			} else if (month_name.equals("Jun")) {
				return "06";
			} else if (month_name.equals("Jul")) {
				return "07";
			} else if (month_name.equals("Aug")) {
				return "08";
			} else if (month_name.equals("Sep")) {
				return "09";
			} else if (month_name.equals("Oct")) {
				return "10";
			} else if (month_name.equals("Nov")) {
				return "11";
			} else if (month_name.equals("Dec")) {
				return "12";
			}
			return month_name;
		}

		private void update_db()
		{
			++bill_cnt;
			Log.i(TAG, "inserting new bill");
			Log.i(TAG, "long_name:[" + long_name + "]");
			Log.i(TAG, "year:[" + year + "]");
			Log.i(TAG, "status:[" + status + "]");
			Log.i(TAG, "url:[" + url + "]");
			Log.i(TAG, "name:[" + name + "]");
			Log.i(TAG, "read_by:[" + read_by + "]");
			Log.i(TAG, "supported_by:[" + supported_by + "]");
			Log.i(TAG, "date_presented:[" + date_presented + "]");
			Log.i(TAG, "update_date:[" + update_date + "]");

			DbAdapter dbHelper = new DbAdapter();
			dbHelper.open_readwrite(mCtx);

			dbHelper.create_bill(long_name, year, status, url, name, read_by, supported_by, date_presented, update_date);

			dbHelper.close();
		}

		private String strip_ws(String s)
		{
			return s.replaceFirst("^\\s+", "").replaceFirst("\\s+$", "");
		}
	}

}

