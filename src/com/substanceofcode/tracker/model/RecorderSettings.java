/*
 * RecorderSettings.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.substanceofcode.tracker.model;

import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.midlet.MIDlet;

import com.substanceofcode.tracker.grid.GridPosition;
import com.substanceofcode.tracker.grid.WGS84Position;
import com.substanceofcode.tracker.view.Logger;
import com.substanceofcode.util.StringUtil;
import com.substanceofcode.util.Version;
import com.substanceofcode.localization.LocaleManager;

/**
 * RecorderSettings contains all settings for the Trail Explorer application.
 * Current settings are: - GPS unit connection string - Export folder (default
 * C:/)
 * 
 * @author Tommi Laukkanen
 * @author Vikas Yadav
 */
public class RecorderSettings {

    /**
     * Settings save/load handler
     */
    public static Settings settings;

    // --------------------------------------------------------------------------
    // String Constants
    // --------------------------------------------------------------------------
    private static final String GPS_DEVICE_STRING = "gps-device";
    private static final String PLACES = "waypoints";
    private static final String UNITS = "units";
    private static final String GRID = "grid";
    private static final String BACKLIGHT = "backlight";
    private static final String POSITIONS_TO_DRAW = "number-of-position-to-draw";
    private static final String DRAW_STYLE = "draw-style";
    private static final String DRAW_MAP = "draw-map";
    private static final String LOGGING_LEVEL = "logger-recording-level";
    private static final String VERSION_NUMBER = "version-number";
    private static final String USEJSR179="jsr179";
    private static final String USEFILECACHE="fileCache";
    private static final String USE_BLUETOOTH_FIX = "bluetoothFix";
    private static final String GEOCODE = "geocode";
    private static final String USE_NETWORK_FOR_MAPS="use-network-for-maps";
    private static final String SUNINFO="sun-info";

    /** Importing settings keys */
    private static final String IMPORT_FILE = "import-file";

    /** Exporting setting keys */
    private static final String EXPORT_FOLDER = "export-folder";
    public static final int EXPORT_FORMAT_KML = 0;
    public static final int EXPORT_FORMAT_GPX = 1;
    public static final String EXPORT_PLACEMARKS = "export-placemarks";

    /** Recording setting keys */
    private static final String RECORDING_INTERVAL = "recording-interval";
    private static final String MARKER_INTERVAL = "recording-marker-interval";
    private static final String RECORDING_MAX_SPEED = "recording-max-speed";
    private static final String RECORDING_MAX_ACCELERATION = "recording-max-acceleration";
    private static final String RECORDING_MIN_DISTANCE = "recording-min-distance";
    private static final String FILTER_TRAIL = "filter-trail";
    private static final String UPLOAD_URL = "upload-url";
    private static final String UPLOAD_USE = "upload-use";
    private static final String UPLOAD_INTERVAL = "upload-interval";

    /** Display setting keys */
    public static final String DISPLAY_COORDINATES = "display-coordinates";
    public static final String DISPLAY_SPEED = "display-speed";
    public static final String DISPLAY_TIME = "display-time";
    public static final String DISPLAY_HEADING = "display-heading";
    public static final String DISPLAY_ALTITUDE = "display-altitude";
    public static final String DISPLAY_DISTANCE = "display-distance";
    public static final String DISPLAY_QUALITY = "display-quality";


    /** Trail Saving Keys */
    public static final String EXPORT_TO_KML = "export-to-kml";
    public static final String EXPORT_TO_GPX = "export-to-gpx";
    public static final String EXPORT_AND_CLEANUP = "export-and-cleanup";
    public static final String EXPORT_TO_SAVE = "export-to-save";
    public static final String EXPORT_TO_GPXSTREAM = "export-to-gpxstream";

    /** Default recording intervals */
    private static final int RECORDING_INTERVAL_DEFAULT = 1;
    private static final int MARKER_INTERVAL_DEFAULT = 10;
    private static final int RECORDING_MAX_SPEED_DEFAULT = 310;
    private static final int RECORDING_MAX_ACCELERATION_DEFAULT = 40;
    private static final int RECORDING_MIN_DISTANCE_DEFAULT = 5;
    private static final int UPLOAD_INTERVAL_DEFAULT = 10;
    
    /** Streaming options */
    private static final String STREAMING_FILE = "streaming-file";
    private static final String STREAMING_STARTED = "streaming-started";


    /** Shortcut key options */
    private static final String SHORTCUT_STAR = "shortcut-asterisk";
    private static final String SHORTCUT_POUND = "shortcut-crosshatch";

    /** Logging */
    public static final String WRITE_LOG = "write-log";

    /** Locale */
    private static final String MTE_LOCALE = "locale";

    /** Upload services */
    private static final String OSM_USERNAME = "osm-username";
    private static final String OSM_PASSWORD = "osm-password";

    /** Multimedia */
    /** Audio */
    private static final String MM_AUDIO_ENCODING = "mm-audio-encoding";
    private static final String MM_AUDIO_INDEX = "mm-audio-index";
    private static final String MM_AUDIO_ENCODING_DEFAULT = "";
    private static final int MM_AUDIO_INDEX_DEFAULT = 0;
    private static final String MM_AUDIO_SUFFIX = "mm-audio-suffix";
    private static final String MM_AUDIO_SUFFIX_DEFAULT = "wav";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String GATEWAY = "gateway";

    public static final String ENABLE_GZIP = "enable_gzip";
    public static final String FORCE_NO_HOST = "force_no_host";
    public static final String HACK = "hack";

    public static final String OAUTH_AUTHORIZED = "oauth_authorized";
    public static final String OAUTH_REQUEST_TOKEN = "oauth_request_token";
    public static final String OAUTH_REQUEST_SECRET = "oauth_request_secret";
    public static final String OAUTH_ACCESS_TOKEN = "oauth_access_token";
    public static final String OAUTH_ACCESS_SECRET = "oauth_access_secret";


    
    private MIDlet member_midlet;

    /** 
     * Creates a new instance of RecorderSettings
     * @param midlet 
     */
    public RecorderSettings(MIDlet midlet) {
        try {
            settings = Settings.getInstance(midlet);
            member_midlet = midlet;
        } catch (Exception ex) {
            Logger.error("Error occured while creating an instance "
                    + "of Settings class: " + ex.toString());
        }
    }

    /**
     * Do we need to filter the recorded trail?
     * @return true if filtering is active
     */
    public boolean getFilterTrail() {
        return settings.getBooleanProperty(FILTER_TRAIL, true);
    }

    /**
     * Define if we need to filter the recorded trail on the fly.
     * @param useFilter
     */
    public void setFilterTrail(boolean useFilter) {
        settings.setBooleanProperty(FILTER_TRAIL, useFilter);
    }

    /**
     * 
     * @return the current geocodeProvider identifier
     */
    public String getGeocode()
    {
        return settings.getStringProperty(GEOCODE, "");
    }

    /**
     * @return Username for OpenStreetMap service
     */
    public String getOpenStreetMapUsername() {
        return settings.getStringProperty(OSM_USERNAME, "");
    }

    /**
     * @return Password for OpenStreetMap service
     */
    public String getOpenStreetMapPassword() {
        return settings.getStringProperty(OSM_PASSWORD, "");
    }

    /**
     * Set OSM username.
     */
    public void setOpenStreetMapUsername(String username) {
        settings.setStringProperty(OSM_USERNAME, username);
        saveSettings();
    }

    /**
     * Set OSM password
     */
    public void setOpenStreetMapPassword(String password) {
        settings.setStringProperty(OSM_PASSWORD, password);
        saveSettings();
    }
    /**
     * set the geocode identifier
     * @param identifier
     */
    public void setGeocode(String identifier)
    {
        settings.setStringProperty(GEOCODE, identifier);
        saveSettings();
    }

    /**
     * 
     * @return the identifier of the selected grid
     */
    public String getGrid()
    {
        return settings.getStringProperty(GRID, "");  
    }
    /**
     * set the selected grid
     * @param identifier
     */
    public void setGrid(String identifier)
    {
        settings.setStringProperty(GRID, identifier);
        saveSettings();
    }

    /**
     * @return Max acceleration between GPS positions.
     */
    public int getMaxAcceleration() {
        int maxAcc = settings.getIntProperty(
                RECORDING_MAX_ACCELERATION, 
                RECORDING_MAX_ACCELERATION_DEFAULT);
        return maxAcc;
    }

    /**
     * Get web recording usage.
     * @return
     */
    public boolean getWebRecordingUsage() {
        return settings.getBooleanProperty(UPLOAD_USE, false);
    }

    /**
     * Set web recording usage.
     * @param useWebRecording   true if we are uploading position to web.
     */
    public void setWebRecordingUsage(boolean useWebRecording) {
        settings.setBooleanProperty(UPLOAD_USE, useWebRecording);
        saveSettings();
    }

    /** Get web recording interval */
    public int getWebRecordingInterval() {
        int defaultInterval = UPLOAD_INTERVAL_DEFAULT;
        int recordingInterval = settings.getIntProperty(UPLOAD_INTERVAL,
                defaultInterval);
        return recordingInterval;
    }

    /** Set web recording interval in seconds */
    public void setWebRecordingInterval(int interval) {
        settings.setIntProperty(UPLOAD_INTERVAL, interval);
        saveSettings();
    }

    
    public void setMaxAcceleration(int maxAcceleration) {
        settings.setIntProperty(RECORDING_MAX_ACCELERATION, maxAcceleration);
        saveSettings();
    }
    
    /** 
     * @return Max speed for recorded position.
     */
    public int getMaxRecordedSpeed() {
        int maxSpeed = settings.getIntProperty(
                RECORDING_MAX_SPEED,
                RECORDING_MAX_SPEED_DEFAULT);
        return maxSpeed;        
    }
    
    public void setMaxRecordedSpeed(int maxSpeed) {
        settings.setIntProperty(RECORDING_MAX_SPEED, maxSpeed);
        saveSettings();
    }
    
    /** @return Min distance for recorded position since last position */
    public int getMinRecordedDistance() {
        int minDistance = settings.getIntProperty(
                RECORDING_MIN_DISTANCE, 
                RECORDING_MIN_DISTANCE_DEFAULT);
        return minDistance;
    }

    public void setMinDistance(int minDistance) {
        settings.setIntProperty(RECORDING_MIN_DISTANCE, minDistance);
        saveSettings();
    }
    
    public String getUploadURL(){
        return settings.getStringProperty(UPLOAD_URL,"");
    }
    
    public void setUploadURL(String url){
        settings.setStringProperty(UPLOAD_URL, url);
        saveSettings();
    }
    
    /**
     * @return True if streaming trail was unfinished.
     */
    public boolean getStreamingStarted()
    {
        return settings.getBooleanProperty(STREAMING_STARTED, false);
    }
    
    /**
     * @return Filename of currently unfinished streaming file or "" if no such
     * filename was saved.
     */
    public String getStreamingFile()
    {
        return settings.getStringProperty(STREAMING_FILE, "");
    }
    
    /**
     * This will set a flag that the streaming has started and will set the
     * full path of the file being streamed to.
     * @param fullPath Full path of file which is being written to.
     */
    public void setStreamingStarted(String fullPath)
    {
        settings.setBooleanProperty(STREAMING_STARTED, true);
        settings.setStringProperty(STREAMING_FILE, fullPath);
        saveSettings();
    }
    
    /**
     * Flags that no stream is active and clears the stored path.
     */
    public void setStreamingStopped()
    {
        settings.setBooleanProperty(STREAMING_STARTED, false);
        settings.setStringProperty(STREAMING_FILE, "");
        saveSettings();
    }
    
    /** Get export folder. Default is E:/ */
    public String getExportFolder() {
        return settings.getStringProperty(EXPORT_FOLDER, "E:/");
    }

    /** Set export folder. */
    public void setExportFolder(String exportFolder) {
        settings.setStringProperty(EXPORT_FOLDER, exportFolder);
        saveSettings();
    }

    /** Get export default value for placemarks */
    public boolean getPlacemarkExport() {
        return settings.getBooleanProperty(EXPORT_PLACEMARKS, false);
    }

    /** Set export default value for placemarks */
    public void setPlacemarkExport(boolean include) {
        settings.setBooleanProperty(EXPORT_PLACEMARKS, include);
        saveSettings();
    }

    /** Get import file. Default is C:/import.gpx */
    public String getImportFile() {
        return settings.getStringProperty(IMPORT_FILE, "C:/import.gpx");
    }

    public void setImportFile(String value) {
        settings.setStringProperty(IMPORT_FILE, value);
        saveSettings();
    }

    /** Get a GPS device connection string */
    public String getGpsDeviceConnectionString() {
        String result = settings.getStringProperty(GPS_DEVICE_STRING, "");
        return result;
    }

    /** Set a GPS device connection string */
    public void setGpsDeviceConnectionString(String connectionString) {
        settings.setStringProperty(GPS_DEVICE_STRING, connectionString);
        saveSettings();
    }

    /** Get places */
    public Vector getPlaces() {
        String encodedPlaces = settings.getStringProperty(PLACES, "");

        // Return empty Vector if we don't have any places
        if (encodedPlaces.length() == 0) {
            return new Vector();
        }

        // Parse places
        Vector places = new Vector();
        String[] placeLines = StringUtil.split(encodedPlaces, "\n");
        int placeCount = placeLines.length;
        for (int placeIndex = 0; placeIndex < placeCount; placeIndex++) {

            String[] values = StringUtil.split(placeLines[placeIndex],
                    "|");
            if (values.length >= 3) {
                String lat = values[0];
                String lon = values[1];
                String name = values[2];

                double latValue = Double.parseDouble(lat);
                double lonValue = Double.parseDouble(lon);
                GridPosition position = null;

                //check, if we can get a GridPosition, if not, just create it with the available data (lat,lon)
                if(values.length > 3)
                {
                    String [] arr = new String[values.length-3];
                    for(int i=0; i<values.length-3;i++)
                    {
                        arr[i] = values[i+3];
                    }
                    position = GridPosition.unserializeGridPosition(arr);
                }
                if(position == null)
                {
                    position = new WGS84Position(latValue, lonValue);
                }

                Place newPlace = new Place(name, position);
                places.addElement( newPlace );
            }
        }
        return places;
    }

    /** 
     * Set places
     * @param places 
     */
    public void setPlaces(Vector places) 
    {
        String placeString = "";
        Enumeration wpEnum = places.elements();
        
        while (wpEnum.hasMoreElements() == true) {
            
            Place wp = (Place) wpEnum.nextElement();
           
            GridPosition pos = wp.getPosition();
        
            WGS84Position pos84 = pos.getAsWGS84Position();
            

            //store the wgs84 anyway, so that if a grid would be removed, the place is not lost
            String latString = String.valueOf(pos84.getLatitude());
        
            String lonString = String.valueOf(pos84.getLongitude());
        
            
            placeString += latString + "|" + lonString + "|" + wp.getName();
        

            //append all the serialized data from the position
            String[] posData = pos.serialize();
            
            for(int i=0; i<posData.length; i++)
            {
                placeString += "|" + posData[i];
            }
            //append end of record
            placeString += "\n";
        }
        
        settings.setStringProperty(PLACES, placeString);
        
        saveSettings();
    }

    /** Get recording interval */
    public int getRecordingInterval() {
        int defaultInterval = RECORDING_INTERVAL_DEFAULT; // Mark default as
                                                            // 10
        // seconds
        int recordingInterval = settings.getIntProperty(RECORDING_INTERVAL,
                defaultInterval);
        return recordingInterval;
    }

    /** Set recording interval in seconds */
    public void setRecordingInterval(int interval) {
        settings.setIntProperty(RECORDING_INTERVAL, interval);
        saveSettings();
    }

    /** Get recording interval for markers */
    public int getMarkerInterval() {
        return settings.getIntProperty(MARKER_INTERVAL,
                MARKER_INTERVAL_DEFAULT);
    }

    /** Set recording interval for markers */
    public void setMarkerInterval(int interval) {
        settings.setIntProperty(MARKER_INTERVAL, interval);
        saveSettings();
    }

    /** Get display setting */
    public boolean getDisplayValue(String displayItem) {
        return settings.getBooleanProperty(displayItem, true);
    }

    /** Set display setting */
    public void setDisplayValue(String displayItem, boolean value) {
        settings.setBooleanProperty(displayItem, value);
        saveSettings();
    }


    /**
     * Get the Logger level. Default is Logger.ERROR.
     * 
     * @return currently set Logging Level.
     */
    public byte getLoggingLevel() {
        return (byte) settings.getIntProperty(LOGGING_LEVEL, Logger.ERROR);
    }

    /**
     * Set the logging level
     */
    public void setLoggingLevel(byte level) {
        settings.setIntProperty(LOGGING_LEVEL, level);
        saveSettings();
    }

    /**
     * Do we use kilometers as units? Default is true!
     */
    public int getDistanceUnitType() {
        return settings.getIntProperty(UNITS, UnitConverter.UNITS_KILOMETERS);
    }

    /** Set units */
    public void setDistanceUnitType(int value) {
        settings.setIntProperty(UNITS, value);
        saveSettings();
    }

    /**
     * Get number of positions to draw
     */
    public int getNumberOfPositionToDraw() {
        return settings.getIntProperty(POSITIONS_TO_DRAW, 150);
    }

    /**
     * Set number of positions to draw
     */
    public void setNumberOfPositionToDraw(int value) {
        if (value < 1) {
            throw new IllegalArgumentException(
                    LocaleManager.getMessage("recorder_settings_setnumberofpositiontodraw")
                    + ": " + value);
        }
        settings.setIntProperty(POSITIONS_TO_DRAW, value);
        saveSettings();
    }

    /** Is the whole trail drawn */
    public boolean getDrawWholeTrail() {
        return settings.getBooleanProperty(DRAW_STYLE, false);
    }

    /** Set the drawing style */
    public void setDrawWholeTrail(boolean value) {
        settings.setBooleanProperty(DRAW_STYLE, value);
        saveSettings();
    }

    /** Are we going to display background maps? */
    public String getDrawMap() {
        return settings.getStringProperty(DRAW_MAP,"");
    }

    /** Set the drawing style */
    public void setDrawMap(String mapIdentifier) {       
        settings.setStringProperty(DRAW_MAP, mapIdentifier);
        saveSettings();
    }
    
    /**
     * Should the sun rise and set times be shown in Information Screen? Default is false;
     */
    public boolean getShowSunInfo()
    {
        return settings.getBooleanProperty(SUNINFO, false);
    }
    /**
     * Set wheather or not to show sun rise and set times.
     */
    public void setShowSunInfo(boolean value) {
        settings.setBooleanProperty(SUNINFO, value);
        saveSettings();
    }
    
    /**
     * Should the backlight always be on? Default is false;
     */
    public boolean getBacklightOn() {
        return settings.getBooleanProperty(BACKLIGHT, false);
    }

    /**
     * Set wheather or not the backlight should always be ON.
     */
    public void setBacklightOn(boolean value) {
        settings.setBooleanProperty(BACKLIGHT, value);
        saveSettings();
    }

    /**
     * Should we default the save screen to check the export to kml box
     */
    public boolean getExportToKML() {
        return settings.getBooleanProperty(EXPORT_TO_KML, true);
    }

    /**
     * Set whether we default the save screen to check the export to kml box
     */
    public void setExportToKML(boolean value) {
        settings.setBooleanProperty(EXPORT_TO_KML, value);
        saveSettings();
    }

    /**
     * Should we default the save screen to check the export to gpx box
     */
    public boolean getExportToGPX() {
        return settings.getBooleanProperty(EXPORT_TO_GPX, true);
    }

    /**
     * Set whether we default the save screen to check the export to gpx box
     */
    public void setExportToGPX(boolean value) {
        settings.setBooleanProperty(EXPORT_TO_GPX, value);
        saveSettings();
    }
    
    /**
     * Should we default the save screen to check the export and remove all places
     */
    public boolean getExportAndCleanup() {
        return settings.getBooleanProperty(EXPORT_AND_CLEANUP, false);
    }

    /**
     * Set whether we default the save screen to check the export and remove all places
     */
    public void setExportAndCleanup(boolean value) {
        settings.setBooleanProperty(EXPORT_AND_CLEANUP, value);
        saveSettings();
    }

    /**
     * Flag to set gpxstream as default recording setting
     */
    public boolean getExportToGPXStream() {
        return settings.getBooleanProperty(EXPORT_TO_GPXSTREAM, false);
    }

    /**
     * Set gpxstream as default export
     */
    public void setExportToGPXStream(boolean value) {
        settings.setBooleanProperty(EXPORT_TO_GPXSTREAM, value);
        saveSettings();
    }

    /**
     * Should we default the save screen to check the export to rms box
     */
    public boolean getExportToSave() {
        return settings.getBooleanProperty(EXPORT_TO_SAVE, false);
    }

    /**
     * Set whether we default the save screen to check the export to xml box
     */
    public void setExportToSave(boolean value) {
        settings.setBooleanProperty(EXPORT_TO_SAVE, value);
        saveSettings();
    }

    /**
     * @since Version 1.7
     * @return The <b>saved</b> Version number (i.e. the Version of the
     *         software that ran last)
     */
    public Version getVersionNumber() {
        String versionString = settings.getStringProperty(VERSION_NUMBER, null);
        if (versionString == null) {
            return null;
        } else {
            return new Version(versionString);
        }
    }

    /**
     * XXX : mchr : What does this mean? How can we set the Version number?
     * @param version 
     */
    public void setVersionNumber(Version version) {
        settings.setStringProperty(VERSION_NUMBER, version.toString());
        saveSettings();
    }

    /** Save settings */
    private void saveSettings() {
        try {
            settings.save(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public boolean getJsr179() {
        return settings.getBooleanProperty(USEJSR179, false);
    }

    /** Set jsr179 support */
    public void setJsr179(boolean value) {
        settings.setBooleanProperty(USEJSR179, value);
        saveSettings();
    }
    
    public boolean getFileCache() {
        return settings.getBooleanProperty(USEFILECACHE, true);
    }

    /** Set filecache support */
    public void setFileCache(boolean value) {
        settings.setBooleanProperty(USEFILECACHE, value);
        saveSettings();
    }
    
    /** Get bluetooth reading fix */
    public boolean getUseBTFix() {
	    return settings.getBooleanProperty(USE_BLUETOOTH_FIX, false);
    }

    /** Set bluetooth reading fix */
    public void setUseBTFix(boolean value) {
	    settings.setBooleanProperty(USE_BLUETOOTH_FIX, value);
	    saveSettings();
    }
    
       /** Get bluetooth reading fix */
    public boolean getUseNetworkForMaps() {
	    return settings.getBooleanProperty(USE_NETWORK_FOR_MAPS, true);
    }

    /** Set bluetooth reading fix */
    public void setUseNetworkForMaps(boolean value) {
	    settings.setBooleanProperty(USE_NETWORK_FOR_MAPS, value);
	    saveSettings();
    }
    
    public boolean getWriteLog() {
        return settings.getBooleanProperty(WRITE_LOG, false);
    }

    /** Set Logging to filesystem*/
    public void setWriteLog(boolean value) {
        settings.setBooleanProperty(WRITE_LOG, value);
        saveSettings();
    }

    /** Get star shortcut */
    public short getStarShortcut() {
        return (short)settings.getIntProperty(SHORTCUT_STAR, 3);
    }

    /** Set star shortcut */
    public void setStarShortcut(int shortcut) {
        settings.setIntProperty(SHORTCUT_STAR, shortcut);
    }

    /** Get cross hatch shortcut */
    public short getPoundShortcut() {
        return (short)settings.getIntProperty(SHORTCUT_POUND, 1);
    }

    /** Set cross hatch shortcut */
    public void setPoundShortcut(int shortcut) {
        settings.setIntProperty(SHORTCUT_POUND, shortcut);
    }

    /** Get saved locale */
    public String getMteLocale() {
        String phoneLocale = System.getProperty("microedition.locale");

        if(phoneLocale == null) {
            phoneLocale = "en";
        }

        return settings.getStringProperty(MTE_LOCALE, phoneLocale);
    }

    /** Set locale */
    public void setMteLocale(String locale) {
        settings.setStringProperty(MTE_LOCALE, locale);
        saveSettings();
    }

    /**
     * get previous used audio encoding string
     */
    public String getAudioEncoding() {
        return settings.getStringProperty(MM_AUDIO_ENCODING, MM_AUDIO_ENCODING_DEFAULT);
    }

    /**
     * Set the audio encoding string
     */
    public void setAudioEncoding(String audioEncoding) {
        settings.setStringProperty(MM_AUDIO_ENCODING, audioEncoding);
    }

    /*
     * get index number to mark previous used codec in audio choice group
     */
    public int getAudioIndex() {
        return settings.getIntProperty(MM_AUDIO_INDEX, MM_AUDIO_INDEX_DEFAULT);
    }

    /*
     * Remember the position in audio encoding choicegroup
     * (easier than comparing a lot of strings)
     */
    public void setAudioIndex(int index) {
        settings.setIntProperty(MM_AUDIO_INDEX, index);
    }

    /**
     * get file suffix used when saving audio notes
     */
    public String getAudioSuffix() {
        return settings.getStringProperty(MM_AUDIO_SUFFIX, MM_AUDIO_SUFFIX_DEFAULT);
    }

    /**
     * Set the file suffix used when saving audio notes
     */
    public void setAudioSuffix(String audioSuffix) {
        settings.setStringProperty(MM_AUDIO_SUFFIX, audioSuffix);
    }

    /** Get string property */
    public String getStringProperty(String name, String defaultValue) {
            Object value = getProperty(name);
            return (value != null) ? value.toString() : defaultValue;
    }

    /** Get property from Hashtable*/
    private synchronized String getProperty(String name) {
            String value = (String) settings.properties.get(name);
            if (value == null && member_midlet != null) {
                    value = member_midlet.getAppProperty(name);
                    if (value != null) {
                            settings.properties.put(name, value);
                    }
            }
            return value;
    }
}
