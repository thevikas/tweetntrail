/**
 * Settings.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.substanceofcode.twitter;

import java.io.*;
import java.util.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

import org.json.me.JSONObject;
import org.json.me.JSONException;

import com.substanceofcode.util.Log;

/**
 * A class for storing and retrieving application settings and properties.
 * Class stores all settings into one Hashtable variable. Hashtable is loaded
 * from RecordStore at initialization and it is stored back to the RecordStore
 * with save method.
 *
 * @author  Tommi Laukkanen
 * @version 1.0
 */
public class Settings {

	private static Settings store;

	private MIDlet midlet;

	private boolean valuesChanged = false;

	private Hashtable properties = new Hashtable();
        
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String GATEWAY = "gateway";
	public static final String PICTURE_GATEWAY = "picture_gateway";
	public static final String TIMELINE_LENGTH = "timeline_length";
	public static final String SUFFIX_TEXT = "suffix_text";
	public static final String REFRESH_INTERVAL = "refresh_interval";
	public static final String AUTO_UPDATE_TEXT = "auto_update_text";
	public static final String START_SCREEN = "start_screen";
	public static final String TIME_OFFSET = "time_offset";
	public static final String CAPTURE_DEVICE = "capture_device";
	public static final String SNAPSHOT_ENCODING = "snapshot_encoding";
	public static final String CUSTOM_WORDS = "custom_words";
	public static final String LOCATION_FORMAT = "location_format";
	public static final String CELLID_FORMAT = "cellid_format";

	public static final String SNAPSHOT_FULLSCREEN = "snapshot_fullscreen";
	public static final String ALTERNATE_AUTHEN = "alternate_authen";
	public static final String OPTIMIZE_BANDWIDTH = "optimize_bandwidth";
	public static final String STATUS_LENGTH_MAX = "status_length_max";
	public static final String RESIZE_THUMBNAIL = "resize_thumbnail";
	public static final String WRAP_TIMELINE = "wrap_timeline";
	public static final String ENABLE_SQUEEZE = "enable_squeeze";
	public static final String ENABLE_GPS = "enable_gps";
	public static final String ENABLE_REVERSE_GEOCODER = "enable_reverse_geocoder";
	public static final String ENABLE_CELL_ID = "enable_cell_id";
	public static final String ENABLE_REFRESH = "enable_refresh";
	public static final String ENABLE_REFRESH_ALERT = "enable_refresh_alert";
	public static final String ENABLE_REFRESH_VIBRATE = "enable_refresh_vibrate";
	public static final String ENABLE_REFRESH_COUNTER = "enable_refresh_counter";
	public static final String SWAP_MINIMIZE_REFRESH = "swap_minimize_refresh";
	public static final String ENABLE_AUTO_UPDATE = "enable_auto_update";
	public static final String ENABLE_AUTO_UPDATE_PICTURE = "enable_auto_update";
	public static final String ENABLE_GZIP = "enable_gzip";
	public static final String FORCE_NO_HOST = "force_no_host";
	public static final String HACK = "hack";

	public static final String OAUTH_AUTHORIZED = "oauth_authorized";
	public static final String OAUTH_REQUEST_TOKEN = "oauth_request_token";
	public static final String OAUTH_REQUEST_SECRET = "oauth_request_secret";
	public static final String OAUTH_ACCESS_TOKEN = "oauth_access_token";
	public static final String OAUTH_ACCESS_SECRET = "oauth_access_secret";

	/**
	 * Singleton pattern is used to return 
	 * only one instance of record store
	 */
	public static synchronized Settings getInstance(MIDlet midlet)
			throws IOException, RecordStoreException, JSONException {
		if (store == null) {
			store = new Settings(midlet);
		}
		return store;
	}

	/** Constructor */
	private Settings(MIDlet midlet) throws IOException, RecordStoreException, JSONException {
		this.midlet = midlet;
		load();
	}

	/* Method never called, so comment out.
	 /** Return true if value exists in record store 
	 private boolean exists( String name ) {
	 return getProperty( name ) != null;
	 }
	 */

	/** Get property from Hashtable*/
	private synchronized String getProperty(String name) {
		String value = (String) properties.get(name);
		if (value == null && midlet != null) {
			value = midlet.getAppProperty(name);
			if (value != null) {
				properties.put(name, value);
			}
		}
		return value;
	}

	/** Get boolean property */
	public boolean getBooleanProperty(String name, boolean defaultValue) {
		String value = getProperty(name);
		if (value != null) {
			return value.equals("true") || value.equals("1");
		}
		return defaultValue;
	}

	/** Get integer property */
	public int getIntProperty(String name, int defaultValue) {
		String value = getProperty(name);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
			}
		}
		return defaultValue;
	}

	/** Get string property */
	public String getStringProperty(String name, String defaultValue) {
		Object value = getProperty(name);
		return (value != null) ? value.toString() : defaultValue;
	}

	/** Load properties from record store */
	private synchronized void load() throws IOException, RecordStoreException, JSONException {
		RecordStore rs = null;

		valuesChanged = false;
		properties.clear();

		try {
			rs = RecordStore.openRecordStore("jibjib", true);
			if (rs.getNumRecords() == 0) {
				rs.addRecord(null, 0, 0);
			} else {
				byte[] data = rs.getRecord(1);
				if (data != null) {
					JSONObject o = new JSONObject(new String(data));
					Enumeration e = o.keys();
					while (e.hasMoreElements()) {
						String name = (String) e.nextElement();
						String value = o.getString(name);
						properties.put(name, value);
					}
				}
			}
		} finally {
			if (rs != null) {
				try {
					rs.closeRecordStore();
				} catch (Exception e) {
				}
			}
		}
	}

	/** Save property Hashtable to record store */
	public synchronized void save(boolean force) throws IOException,
			RecordStoreException, JSONException {
		if (!valuesChanged && !force)
			return;

		RecordStore rs = null;

		try {
			JSONObject o = new JSONObject();
			Enumeration e = properties.keys();
			while (e.hasMoreElements()) {
				String name = (String) e.nextElement();
				String value = properties.get(name).toString();
				o.put(name, value);
			}

			byte[] data = o.toString().getBytes();

			rs = RecordStore.openRecordStore("jibjib", true);
			rs.setRecord(1, data, 0, data.length);
		} catch (Exception e) {
			Log.error(e.toString());
		} finally {
			if (rs != null) {
				try {
					rs.closeRecordStore();
				} catch (Exception e) {
				}
			}
		}
	}

	/** Set a boolean property */
	public void setBooleanProperty(String name, boolean value) {
		setStringProperty(name, value ? "true" : "false");
	}

	/** Set an integer property */
	public void setIntProperty(String name, int value) {
		setStringProperty(name, Integer.toString(value));
	}

	/** Set a string property */
	public synchronized boolean setStringProperty(String name, String value) {
		if (name == null && value == null)
			return false;
		properties.put(name, value);
		valuesChanged = true;
		return true;
	}
}
