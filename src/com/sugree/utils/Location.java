package com.sugree.utils;

import java.io.IOException;

//#ifdef polish.api.locationapi
//# import javax.microedition.location.*;
//#endif

import org.json.me.JSONObject;
import org.json.me.JSONException;
import com.substanceofcode.infrastructure.Device;
import com.substanceofcode.util.Log;
import com.substanceofcode.util.HttpUtil;
import com.substanceofcode.util.StringUtil;
import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.twitter.Settings;

public class Location {
//#if polish.midp2 || polish.midp3
//# 	public static final int MODE_GPS = 0;
//# 	public static final int MODE_REVERSE_GEOCODER = 1;
//# 	public static final int MODE_CELL_ID = 2;
//# 
//# 	private String altitude;
//# 	private String latitude;
//# 	private String longitude;
//# 	private Settings settings;
//# 
//# 	public Location(Settings settings) {
//# 		altitude = "0.0";
//# 		latitude = "0.0";
//# 		longitude = "0.0";
//# 		this.settings = settings;
//# 	}
//# 
//# 	public String getAltitude() {
//# 		return altitude;
//# 	}
//# 
//# 	public String getLatitude() {
//# 		return latitude;
//# 	}
//# 
//# 	public String getLongitude() {
//# 		return longitude;
//# 	}
//# 
//# 	public String toString() {
//# 		String s = settings.getStringProperty(Settings.LOCATION_FORMAT, "l:%lat,%lon http://maps.google.com/maps?q=%lat%2c%lon");
//#ifdef polish.hasFloatingPoint
//# 		s = StringUtil.replace(s, "%lat", latitude);
//# 		s = StringUtil.replace(s, "%lon", longitude);
//#endif
//# 		return s;
//# 	}
//# 
//# 	public String getCellId() {
//# 		int cid = Device.getCellID();
//# 		int lac = Device.getLAC();
//# 
//# 		String s = settings.getStringProperty(Settings.CELLID_FORMAT, "c2l:%cid,%lac");
//# 		s = StringUtil.replace(s, "%cid", ""+cid);
//# 		s = StringUtil.replace(s, "%lac", ""+lac);
//# 		return s;
//# 	}
//# 
//# 	public String refresh(TwitterApi api, int mode) throws Exception {
//# 		if (mode == MODE_CELL_ID) {
//# 			return getCellId();
//# 		} else if (api != null && mode == MODE_REVERSE_GEOCODER) {
//#ifdef polish.hasFloatingPoint
//# 			refreshGLM(api);
//# 		} else {
//#ifdef polish.api.locationapi
//# 			refreshLAPI();
//#endif
//#endif
//# 		}
//# 		return toString();
//# 	}
//# 
//#ifdef polish.hasFloatingPoint
//# 	private void refreshGLM(TwitterApi api) throws IOException, Exception {
//# 		int cid = Device.getCellID();
//# 		int lac = Device.getLAC();
//# 
//# 		String payload = api.requestLocationByCellID(cid, lac);
//# 		if (payload.length() > 0) {
//# 			JSONObject response = new JSONObject(payload);
//# 			altitude = "0.0";
//# 			latitude = ""+round(response.getDouble("lat"));
//# 			longitude = ""+round(response.getDouble("long"));
//# 		}
//# 	}
//# 
//#ifdef polish.api.locationapi
//# 	private void refreshLAPI() throws LocationException, InterruptedException {
//# 		Criteria c = new Criteria();
//# 		c.setHorizontalAccuracy(1000);
//# 		c.setVerticalAccuracy(1000);
//# 		c.setPreferredPowerConsumption(Criteria.POWER_USAGE_LOW);
//# 		LocationProvider lp = LocationProvider.getInstance(c);
//# 		javax.microedition.location.Location loc = lp.getLocation(60);
//# 		QualifiedCoordinates qc = loc.getQualifiedCoordinates();
//# 		altitude = ""+round(qc.getAltitude());
//# 		latitude = ""+round(qc.getLatitude());
//# 		longitude = ""+round(qc.getLongitude());
//# 	}
//#endif
//# 
//# 	private double round(double d) {
//# 		return (double)(int)((d+0.0000005)*1000000.0)/1000000.0;
//# 	}
//#endif
//#endif
}
