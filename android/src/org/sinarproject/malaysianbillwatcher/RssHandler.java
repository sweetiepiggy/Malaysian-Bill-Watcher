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

import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;

public class RssHandler extends DefaultHandler
{
	private static final int EXPECTED_BILL_CNT = 100;

	private SyncTask mSyncTask;
	private String mLastUpdate = "";
	private double mMaxProgress = 100;
	private Boolean mDone = false;
	private Boolean mInItem = false;
	private String mCurChars = "";

	private LinkedList<ContentValues> mBills = new LinkedList<ContentValues>();
	private ContentValues mCurBill = new ContentValues();

	public RssHandler(SyncTask syncTask, String lastUpdate, double maxProgress)
	{
		mSyncTask = syncTask;
		mLastUpdate = lastUpdate;
		mMaxProgress = maxProgress;
	}

	public LinkedList<ContentValues> getBills()
	{
		return mBills;
	}

	@Override
	public void startDocument()
	{
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attr) throws SAXException
	{
		if (!mDone) {
			if (localName.equalsIgnoreCase("item")) {
				mInItem = true;
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException
	{
		if (!mDone) {
			if (localName.equalsIgnoreCase("item")) {
				/* TODO: should use strftime() first? */
				if (mLastUpdate.compareTo(mCurBill.getAsString(DbAdapter.KEY_UPDATE_DATE)) < 0) {
					mBills.addFirst(mCurBill);
					int progress = java.lang.Math.min((int) mMaxProgress,
							(int)(((double) mBills.size() / EXPECTED_BILL_CNT) * mMaxProgress));
					mSyncTask.updateProgress(progress);
				} else {
					mDone = true;
				}

				mInItem = false;
				mCurBill = new ContentValues();
			} else if (mInItem) {
				if (localName.equalsIgnoreCase("title")) {
					mCurBill.put(DbAdapter.KEY_LONG_NAME, strip_ws(new String(mCurChars)));
				} else if (localName.equalsIgnoreCase("description")) {
					parseDescription(mCurChars);
				} else if (localName.equalsIgnoreCase("pubDate")) {
					mCurBill.put(DbAdapter.KEY_UPDATE_DATE, formatDate(strip_ws(new String(mCurChars))));
				} else if (localName.equalsIgnoreCase("link")) {
					mCurBill.put(DbAdapter.KEY_SINAR_URL, strip_ws(new String(mCurChars)));
				}
			}
			mCurChars = "";
		}
	}

	@Override
	public void characters(char[] ch, int start, int len) throws SAXException {
		if (!mDone) {
			mCurChars += new String(ch, start, len);
		}
	}

	private void parseDescription(final String desc)
	{
		for (String line : desc.split("\n")) {
			int idx = line.indexOf(':');
			if (idx != -1 && idx < line.length() - 1) {
				String key = strip_ws(line.substring(0, idx));
				String val = strip_ws(line.substring(idx + 1));
				if (!val.equals("None")) {
					if (key.equalsIgnoreCase("year")) {
						mCurBill.put(DbAdapter.KEY_YEAR, val);
					} else if (key.equalsIgnoreCase("status")) {
						mCurBill.put(DbAdapter.KEY_STATUS, val);
					} else if (key.equalsIgnoreCase("url")) {
						mCurBill.put(DbAdapter.KEY_URL, val);
					} else if (key.equalsIgnoreCase("name")) {
						mCurBill.put(DbAdapter.KEY_NAME, val);
					} else if (key.equalsIgnoreCase("read_by")) {
						mCurBill.put(DbAdapter.KEY_READ_BY, val);
					} else if (key.equalsIgnoreCase("supported_by")) {
						mCurBill.put(DbAdapter.KEY_SUPPORTED_BY, val);
					} else if (key.equalsIgnoreCase("date_presented")) {
						mCurBill.put(DbAdapter.KEY_DATE_PRESENTED, val);
					}
				}
			}
		}
	}

	private String formatDate(final String date)
	{
		String[] fields = date.split("\\s+");

		String ret;

		/* date looks like this: Sun, 04 Mar 2012 11:21:25 GMT */
		if (fields.length == 6) {
			String day = fields[1];
			String month = monthName2Num(fields[2]);
			String year = fields[3];
			String time = fields[4];
			ret = year + "-" + month + "-" + day + " " + time;
		} else {
			ret = date;
		}

		return ret;
	}

	/* TODO: is there a library for this? */
	private String monthName2Num(final String month_name)
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

	private String strip_ws(String s)
	{
		return s.replaceFirst("^\\s+", "").replaceFirst("\\s+$", "");
	}
}

