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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RssHandler extends DefaultHandler
{
	private Boolean in_item = false;
	private String cur_name = "";
	private String cur_chars = "";
	private String long_name = "";
	private String year = "";
	private String status = "";
	private String update_date = "";

	@Override
	public void startElement(String uri, String local_name, String q_name,
			Attributes attr) throws SAXException
	{
		cur_name = new String(local_name);
		if (cur_name.equalsIgnoreCase("item")) {
			in_item = true;
		}
	}

	@Override
	public void endElement(String uri, String local_name, String q_name)
			throws SAXException
	{
		if (cur_name.equalsIgnoreCase("item")) {
			update_db(long_name, update_date);
			in_item = false;
			long_name = "";
			year = "";
			status = "";
			update_date = "";
		} else if (in_item) {
			if (cur_name.equalsIgnoreCase("title")) {
				long_name = new String(cur_chars);
			} else if (cur_name.equalsIgnoreCase("description")) {
				parse_description(cur_chars);
			} else if (cur_name.equalsIgnoreCase("pubDate")) {
				update_date = new String(cur_chars);
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int len) throws SAXException {
		cur_chars = new String(ch, start, len);
	}

	private void parse_description(String desc)
	{
	}

	private void update_db(String long_name, String update_date)
	{
	}
}

