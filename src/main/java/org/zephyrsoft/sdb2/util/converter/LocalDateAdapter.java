/*
 * This file is part of the Song Database (SDB).
 *
 * SDB is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * SDB is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with SDB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.sdb2.util.converter;

import java.time.LocalDate;

import org.zephyrsoft.sdb2.util.DateTools;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * XML adapter for {@link LocalDate}.
 */
public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
	
	@Override
	public LocalDate unmarshal(String v) throws Exception {
		return DateTools.parseDate(v);
	}
	
	@Override
	public String marshal(LocalDate v) throws Exception {
		return DateTools.formatDate(v);
	}
}
