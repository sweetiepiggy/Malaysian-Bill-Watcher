/*
    Copyright (C) 2013 Sinar Project

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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;

public class ParlimenHandler extends DefaultHandler
{
	private final String BILL_BASE_URL = "http://www.parlimen.gov.my";

	private SyncTask mSyncTask;
	private double mMaxProgress = 100;

	private LinkedList<ContentValues> mBills = new LinkedList<ContentValues>();
	private ContentValues mCurBill = new ContentValues();

	public ParlimenHandler(SyncTask syncTask, double maxProgress)
	{
		mSyncTask = syncTask;
		mMaxProgress = maxProgress;
	}

	public LinkedList<ContentValues> parseBills(String url) throws IOException
	{
		LinkedList<ContentValues> ret = new LinkedList<ContentValues>();

		Document doc = Jsoup.connect(url).get();
		Element table = doc.getElementById("mytable");

		Iterator<Element> tr_itr = table.select("tr").iterator();
		if (tr_itr.hasNext()) {
			Elements trs = tr_itr.next().siblingElements();
			int rows = trs.size();
			int i = 0;
			for (Element tr : trs) {
				mSyncTask.updateProgress((int)(((double) i / rows) * mMaxProgress));
				Elements tds = table.select("td");
				if (tds.size() > 3) {
					ContentValues bill = new ContentValues();

					String name = tds.get(0).text();
					bill.put(DbAdapter.KEY_NAME, name);

					String year = tds.get(1).text();
					bill.put(DbAdapter.KEY_YEAR, year);

					String long_name = tds.get(2).text();
					bill.put(DbAdapter.KEY_LONG_NAME, long_name);

					Iterator<Element> a_itr = tds.get(0).select("a").iterator();
					if (a_itr.hasNext()) {
						String bill_url = a_itr.next().attr("onclick");
						int start = bill_url.indexOf('\'') + 1;
						int end = bill_url.indexOf('\'', start);
						bill_url = BILL_BASE_URL + bill_url.substring(start, end);
						bill.put(DbAdapter.KEY_URL, bill_url);
					}

					Iterator<Element> div_itr = tds.get(3).select("div").iterator();
					if (div_itr.hasNext()) {
						String status = div_itr.next().text();
						bill.put(DbAdapter.KEY_STATUS, status);
					}

					ret.addFirst(bill);
				}
				++i;
			}
		}

		return ret;
	}

}

