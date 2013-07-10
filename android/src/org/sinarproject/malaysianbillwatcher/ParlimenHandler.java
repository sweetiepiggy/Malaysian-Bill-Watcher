/*
    Copyright (C) 2012,2013 Sinar Project

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
	private SyncTask mSyncTask;
	private String mLastUpdate = "";
	private double mMaxProgress = 100;

	private LinkedList<ContentValues> mBills = new LinkedList<ContentValues>();
	private ContentValues mCurBill = new ContentValues();

	public ParlimenHandler(SyncTask syncTask, String lastUpdate, double maxProgress)
	{
		mSyncTask = syncTask;
		mLastUpdate = lastUpdate;
		mMaxProgress = maxProgress;
	}

	public LinkedList<ContentValues> parseBills(String url) throws IOException
	{
		LinkedList<ContentValues> ret = new LinkedList<ContentValues>();

		Document doc = Jsoup.connect(url).get();
		Element table = doc.getElementById("mytable");

		Iterator<Element> tr_itr = table.select("tr").iterator();
		if (tr_itr.hasNext()) {
			for (Element tr : tr_itr.next().siblingElements()) {
				Elements tds = table.select("td");
				if (tds.size() > 3) {
					String name = tds.get(0).text();
					String year = tds.get(1).text();
					String long_name = tds.get(2).text();
					android.util.Log.i("SyncTask", "name: " + name);
					android.util.Log.i("SyncTask", "year: " + year);
					android.util.Log.i("SyncTask", "long_name: " + long_name);

					Iterator<Element> div_itr = tds.get(3).select("div").iterator();
					if (div_itr.hasNext()) {
						String status = div_itr.next().text();
						android.util.Log.i("SyncTask", "status: " + status);

						ContentValues bill = new ContentValues();
						bill.put(DbAdapter.KEY_NAME, name);
						bill.put(DbAdapter.KEY_YEAR, year);
						bill.put(DbAdapter.KEY_LONG_NAME, long_name);
						bill.put(DbAdapter.KEY_STATUS, status);
						ret.addFirst(bill);
					}
				}
			}
		}

		return ret;
	}

}

