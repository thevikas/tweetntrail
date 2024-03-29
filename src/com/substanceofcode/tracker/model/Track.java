/*
 * Track.java
 *
 * Copyright (C) 2005-2009 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package com.substanceofcode.tracker.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.io.EOFException;
import java.util.Date;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import com.substanceofcode.data.FileIOException;
import com.substanceofcode.data.FileSystem;
import com.substanceofcode.data.Serializable;
import com.substanceofcode.gps.GpsPosition;
import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.view.Logger;
import com.substanceofcode.util.DateTimeUtil;
import com.substanceofcode.util.StringUtil;
import com.substanceofcode.localization.LocaleManager;

/**
 * <p>
 * A Track is an ordered list of {@link GpsPosition}s which represents the
 * movement of a GPS enabled device over time.
 * 
 * <p>
 * A Track has two main elements
 * <ul>
 * <li>The Track: An ordered list of {@link GpsPosition}s which is <b>the
 * Track</b>
 * <li>The Markers: An ordered list of Markers (or Places). Markers should,
 * to be of use, be relavent to the Track, but this is not a strict requirement.
 * </ul>
 * 
 * <p>
 * A Track also has a distance. This is the sum of the distances between the
 * points on the track.<br>
 * <small>i.e. if a track consists of 5 points, a, b, c, d, and e, and |ab| is
 * the distance between point a and point b, then Tracks 'Distance' would be
 * (|ab| + |bc| + |cd| + |de|)</small>
 * 
 * @author Tommi
 * @author Barry Redmond
 */
public class Track implements Serializable {

    /**
     * The MIME type for all Tracks stored XXX : mchr : What is this for?
     */
    public static final String MIME_TYPE = "Mobile Trail Trail";

    /** A Vector of {@link GpsPosition}s representing this 'Trails' route. */
    private Vector trackPoints;

    /**
     * A Vector of {@link GpsPosition}s representing this 'Trails' Markers or
     * Places.
     */
    private Vector trackMarkers;

    /** The Track statistics */
    private double distance;
    private GpsPosition maxSpeedPosition;
    private GpsPosition maxAltitude;
    private GpsPosition minAltitude;

    private double maxLatitude= -1;
    private double minLatitude= 99;
    private double maxLongitude=-400;
    private double minLongitude=400;

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public double getMaxAltitude() {
        return maxAltitude.altitude;
    }
    
    public double getMinAltitude() {
        return minAltitude.altitude;
    }

    public double getMaxSpeed() {
        return maxSpeedPosition.speed;
    }

    public double getMinSpeed() {
        return 0;
    }

    /** The Tracks name */
    private String name = null;

    /** Constant:Pause file name */
    public static final String PAUSEFILENAME = "pause";

    // --------------------------------------------------------------------------
    // Optional elements associated with a Track which is being streamed to
    // disk
    // --------------------------------------------------------------------------
    private FileConnection streamConnection = null;
    private OutputStream streamOut = null;
    private PrintStream streamPrint = null;

    /**
     * State variable : True - This track should be streamed to disk, False -
     * This track should be saved right at the end
     */
    private boolean isStreaming = false;

    
    /** Unique identifer of the track */
    private long id;
    
    /**
     * the datetime of starting the trail
     */
    private long trailStartTime = 0;
    
    /**
     * Creates a new instance of Track which will be saved at the end
     */
    public Track() {
        trackPoints = new Vector();
        trackMarkers = new Vector();
        distance = 0.0;
        name = "";
        /** Create unique identifier for the track */
        String dateStamp = DateTimeUtil.getCurrentDateStamp();
        dateStamp = StringUtil.replace(dateStamp, "_", "");
        id = Long.parseLong(dateStamp);
        
        trailStartTime = System.currentTimeMillis();
    }

    /**
     * Construct a Track which streams all points directly to a GPX file.
     * 
     * @param fullPath
     *                Full path of GPX stream
     * @param newStream
     *                True : Creates a new GPX stream, False : Reconnects to an
     *                existing GPX stream
     * @throws IOException
     */
    public Track(String fullPath, boolean newStream) throws IOException {
        this();
        isStreaming = true;
        
        trailStartTime = System.currentTimeMillis();
        
        try {
            // ------------------------------------------------------------------
            // Create a FileConnection and if this is a new stream create the
            // file
            // ------------------------------------------------------------------
            streamConnection = (FileConnection) Connector.open(fullPath,
                    Connector.READ_WRITE);
            if (newStream) {
                streamConnection.create();
            }

            // ------------------------------------------------------------------
            // Open outputStream positioned at the end of the file
            // For a new file this will be the same as positioning at the start
            // For an existing file this allows us to append data
            // ------------------------------------------------------------------
            streamOut = streamConnection.openOutputStream(streamConnection
                    .fileSize() + 1);
            streamPrint = new PrintStream(streamOut);

            // ------------------------------------------------------------------
            // If this is a new stream we must add headers
            // ------------------------------------------------------------------
            if (newStream) {
                StringBuffer gpxHead = new StringBuffer();
                GpxConverter.addHeader(gpxHead);
                GpxConverter.addTrailStart(gpxHead);
                streamPrint.print(gpxHead.toString());
                streamPrint.flush();
                streamOut.flush();
            }
        } catch (IOException e) {
            closeStreams();
            throw e;
        }catch(SecurityException e){
            closeStreams();
            Logger.error("Track: Security Exception: "+e.getMessage());
        }
    }

    /** Remove the latest position from trail. */
    void removeLastPosition() {
        final int lastIndex = trackPoints.size()-1;
        if(trackPoints.size()>1) {
            // Remove last distance
           final GpsPosition p1 = (GpsPosition) trackPoints.elementAt(lastIndex);
           final GpsPosition p2 = (GpsPosition) trackPoints.elementAt(lastIndex-1);
           double tripLength = p1.getDistanceFromPosition(p2);
           distance -= tripLength;
        }
        trackPoints.removeElementAt(lastIndex);
    }

    private void closeStreams(){
        /* If we get any IOException we must ensure that we close all stream
         * objects
         */
        try{
            if (streamPrint != null) {
                streamPrint.close();
                streamPrint = null;
            }

            if (streamOut != null) {
                streamOut.close();
                streamOut = null;
            }

            if (streamConnection != null) {
                streamConnection.close();
                streamConnection = null;
            }
        }catch(Exception e){
            Logger.error("Exception while closing streams: "+e.getMessage());
        }
    }

    /**
     * Instantiate a Track from a DataInputStream
     */
    public Track(DataInputStream dis) throws IOException {
        this();
        this.unserialize(dis);
    }

    /*
     * Getter Methods
     */
    /** Get whether this is a streaming trail */
    public boolean isStreaming() {
        return isStreaming;
    }

    /** 
     * Get track identifier
     * @return Track identifier
     */
    public long getId() {
        return id;
    }
    
    /** Get position count */
    public int getPositionCount() {
        int positionCount = trackPoints.size();
        return positionCount;
    }

    /** @return an Enumeration of this Tracks Points */
    public Enumeration getTrackPointsEnumeration() {
        return trackPoints.elements();
    }

    /**
     * @return the Track Point specified by the parameter
     * @param positionNumber ,
     *                the index of the Track Point to return
     */
    public GpsPosition getPosition(int positionNumber)
            throws ArrayIndexOutOfBoundsException {
        return (GpsPosition) trackPoints.elementAt(positionNumber);
    }

    /** @return the first position */
    public GpsPosition getStartPosition() {
        if( trackPoints==null || trackPoints.size()==0) {
            return null;
        }
        GpsPosition startPosition = null;
        try {
            startPosition = (GpsPosition) trackPoints.firstElement();
        } catch (NoSuchElementException nsee) {

        }
        return startPosition;
    }

    /**
     * @return the last position in the track, or null if there is no end
     *         position
     */
    public GpsPosition getEndPosition() {
        if( trackPoints==null || trackPoints.size()==0) {
            return null;
        }        
        GpsPosition endPosition = null;
        try {
            endPosition = (GpsPosition) trackPoints.lastElement();
        } catch (NoSuchElementException nsee) {

        }
        return endPosition;
    }

    /** @return the position of maximum speed */
    public GpsPosition getMaxSpeedPosition() {
        return maxSpeedPosition;
    }
    
    /** @return the position of minimum altitude */
    public GpsPosition getMinAltitudePosition() {
        return minAltitude;
    }
    
    /** @return the position of maximum altitude */
    public GpsPosition getMaxAltitudePosition() {
        return maxAltitude;
    }

    /** @return the track duration in milliseconds */
    public long getDurationMilliSeconds() {
        if(this.getStartPosition()!=null && this.getEndPosition()!=null) {
            Date startDate = this.getStartPosition().date;
            Date endDate = this.getEndPosition().date;
            return (endDate.getTime() - startDate.getTime());
        } else {
            return 0;
        }
    }
    
    public long getFullTrailDurationMilliSeconds() {
         return System.currentTimeMillis() - trailStartTime;
    }
    
    /** @return the average speed (kmh) */
    public double getAverageSpeed() {
        
        //this is good and accurate all along the trail
        double distanceKm = getDistance();
        
        //200908142033:thevikas:average speed goes inaccurate
        //this goes wrong after the default trail length is reached (e.g. 150)
        //after crossing, the time is calculated using just last 150 points while distance is for entire trail.
        //therefore, average inaccurately goes more than max speed
        
        //double durationMilliSeconds = getDurationMilliSeconds();
        double durationMilliSeconds = getFullTrailDurationMilliSeconds();
        
        
        if (durationMilliSeconds == 0) { return 0; }
        double hours = durationMilliSeconds / 3600000.0;
        if (distanceKm > 0.01) {
            return distanceKm / hours;
        } else {
            return 0;
        }
    }

    /** @return the marker count */
    public int getMarkerCount() {
        return trackMarkers.size();
    }

    /** @return an Enumeration of the Markers for this Track */
    public Enumeration getTrackMarkersEnumeration() {
        return trackMarkers.elements();
    }

    /**
     * @return the Marker specified by the parameter
     * @param markerNumber ,
     *                the index of the Marker to return
     */
    public Marker getMarker(int markerNumber) {
        return (Marker) trackMarkers.elementAt(markerNumber);
    }

    /** @return the Trackss distance in kilometers */
    public double getDistance() {
        return distance;
    }

    /** 
     * Gets this Track's Name
     * @return name
     */
    public String getName() {
        return name;
    }

    /*
     * Setter methods
     */
    /** Sets this Track's Name
     * @param name 
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * Other Methods
     */

    /** Add new Track Point to the end of this Track */
    public GpsPosition addPosition(GpsPosition pos) {
        /** Handle distance calculations */
        if (trackPoints.size() > 0) {
            // Increment Distance
            final GpsPosition lastPosition = getEndPosition();
            double tripLength = lastPosition.getDistanceFromPosition(pos);
            distance += tripLength;
        } else {
            maxSpeedPosition = pos;
            maxAltitude = pos;
            minAltitude = pos;
            minLatitude = pos.latitude;
            maxLatitude = pos.latitude;
            minLongitude = pos.longitude;
            maxLongitude = pos.longitude;
        }

        /** Check for max speed */
        if( maxSpeedPosition.speed < pos.speed ) {
            maxSpeedPosition = pos;
        }
        
        /** Check for min/max altitude */
        if( minAltitude.altitude > pos.altitude ) {
            minAltitude = pos;
        }
        if( maxAltitude.altitude < pos.altitude ) {
            maxAltitude = pos;
        }

        if (minLatitude > pos.latitude)
            minLatitude = pos.latitude;

        if (maxLatitude < pos.latitude)
            maxLatitude = pos.latitude;

        if (minLongitude > pos.longitude)
            minLongitude = pos.longitude;

        if (maxLongitude < pos.longitude)
            maxLongitude = pos.longitude;

        pos.currentAverageSpeed = this.getAverageSpeed();

        trackPoints.addElement(pos);

        // ----------------------------------------------------------------------
        // If this is a streaming track then we need to save the new position
        // and possibly forget about some old points
        // ----------------------------------------------------------------------
        if (isStreaming) {
            // ------------------------------------------------------------------
            // Store the new point
            // ------------------------------------------------------------------
            Controller lController = Controller.getController();
            RecorderSettings lSettings = lController.getSettings();
            StringBuffer gpxPos = new StringBuffer();
            // GpxConverter.addPosition(pos, gpxPos);
            GpxConverter.addPosition(pos, gpxPos);
            streamPrint.print(gpxPos.toString());
            streamPrint.flush();
            try {
                streamOut.flush();

                // ----------------------------------------------------------------
                // We only store in memory as many points as we are going to
                // draw
                // ----------------------------------------------------------------
                int maxNumPos = lSettings.getNumberOfPositionToDraw();
                // ----------------------------------------------------------------
                // While we have too many points remove the oldest point
                // ----------------------------------------------------------------
                while (trackPoints.size() > maxNumPos) {
                    trackPoints.removeElementAt(0);
                }
            } catch (IOException e) {
                lController.showError(LocaleManager.getMessage("track_addposition_exception")
                        + " : " + e.toString());
            }
        }
        
        return (GpsPosition)trackPoints.lastElement();

    }

    /** Add new marker
     * @param marker    Marker on a trail.
     */
    public void addMarker(Marker marker) {
        trackMarkers.addElement(marker);

        // ----------------------------------------------------------------------
        // If this is a streaming trail remove old markers from memory
        // ----------------------------------------------------------------------
        if (isStreaming) {
            RecorderSettings lSettings = Controller.getController().getSettings();
            int markerInterval = lSettings.getMarkerInterval();
            if (markerInterval > 0) {
                int maxNumMarkers = lSettings.getNumberOfPositionToDraw() / markerInterval;
                while (trackMarkers.size() > maxNumMarkers) {
                    trackMarkers.removeElementAt(0);
                }
            }
        }
    }

    /**
     * Clears <b>all</b> of this Tracks Points AND Markers and resets the
     * distance to 0.0
     */
    public void clear() {
        trackPoints.removeAllElements();
        trackMarkers.removeAllElements();
        distance = 0.0;
    }

    /**
     * TODO
     * 
     * @return
     * @throws IOException
     */
    public String closeStream() throws IOException {
        if (isStreaming) {
            StringBuffer gpxTail = new StringBuffer();
            GpxConverter.addTrailEnd(gpxTail);
            GpxConverter.addFooter(gpxTail);
            streamPrint.print(gpxTail.toString());
            streamPrint.flush();
            streamPrint.close();
            streamOut.close();
            streamConnection.close();
            isStreaming = false;
            return streamConnection.getPath() + "/"
                    + streamConnection.getName();
        } else {
            return "";
        }
    }

    /**
     * Export track to file.
     * 
     * @return Full path of file which was written to
     * 
     * @throws java.lang.Exception
     * @param folder
     *                Folder where file is written.
     * @param places
     *                Vector containing places.
     * @param distanceUnitType
     *                Type of units to use?
     * @param exportFormat
     *                Export format.
     * @param filename
     *                Name of file or null if we should create a timestamp
     * @param listener
     *                Reference to class which wants to be notified of events
     */
    public String writeToFile(String folder, Vector places,
            int distanceUnitType, int exportFormat, String filename,
            AlertHandler listener) throws Exception {
        String fullPath = "";
        // ----------------------------------------------------------------------
        // Notify listener that we have started a long running process
        // ----------------------------------------------------------------------
        if (listener != null) {
            String lType = "";
            switch (exportFormat) {
                case RecorderSettings.EXPORT_FORMAT_GPX:
                    lType = "GPX";
                    break;

                case RecorderSettings.EXPORT_FORMAT_KML:
                    lType = "KML";
                    break;
            }
            listener.notifyProgressStart(LocaleManager.getMessage("track_writetofile_prgs_start",
                    new Object[] {lType}));
            listener.notifyProgress(1);
        }
        // ------------------------------------------------------------------
        // Instanciate the correct converter
        // ------------------------------------------------------------------
        TrackConverter converter = null;
        String extension = ".xml";
        if (exportFormat == RecorderSettings.EXPORT_FORMAT_KML) {
            converter = new KmlConverter(distanceUnitType);
            extension = ".kml";
        } else if (exportFormat == RecorderSettings.EXPORT_FORMAT_GPX) {
            converter = new GpxConverter();
            extension = ".gpx";
        }

        // ------------------------------------------------------------------
        // Construct filename and connect to the file
        // ------------------------------------------------------------------
        if (filename == null || filename.length() == 0) {
            filename = DateTimeUtil.getCurrentDateStamp();
        }
        FileConnection connection;
        try {
            folder += (folder.endsWith("/") ? "" : "/");
            fullPath = "file:///" + folder + filename + extension;
            Logger.debug("Opening : " + fullPath);
            connection = (FileConnection) Connector.open(fullPath,
                    Connector.WRITE);
        } catch (Exception ex) {
            Logger.info("Open threw : " + ex.toString());
            ex.printStackTrace();
            throw new Exception(LocaleManager.getMessage("track_writetofile_exception_write")
                    + ": " + ex.toString());
        }
        try {
            // Create file
            connection.create();
        } catch (Exception ex) {
            connection.close();
            throw new Exception(LocaleManager.getMessage("track_writetofile_exception_open")
                    + ": " + ex.toString());
        }

        // ------------------------------------------------------------------
        // Create OutputStream to the file connection
        // ------------------------------------------------------------------
        OutputStream out;
        try {
            out = connection.openOutputStream();
        } catch (Exception ex) {
            connection.close();
            throw new Exception(LocaleManager.getMessage("track_writetofile_exception_stream")
                    + ": " + ex.toString());
        }
        DataOutputStream output = new DataOutputStream(out);

        // ------------------------------------------------------------------
        // Notify progress
        // ------------------------------------------------------------------
        if (listener != null) {
            listener.notifyProgress(2);
        }
        long start = System.currentTimeMillis();
        long end;
        // ------------------------------------------------------------------
        // Convert the data into a String
        // ------------------------------------------------------------------
        String exportData = converter.convert(this, places, true, true);
        end = System.currentTimeMillis();
        Logger.debug("Converted track in " +(end-start)+"ms");
        // ------------------------------------------------------------------
        // Notify progress
        // ------------------------------------------------------------------
        if (listener != null) {
            listener.notifyProgress(8);
        }

        // ------------------------------------------------------------------
        // Save the data to a file
        // ------------------------------------------------------------------
        // encode to KML/GPX UTF-8
        output.write(exportData.getBytes("UTF-8"));
        end = System.currentTimeMillis();
        Logger.debug("Wrote file in " +(end-start)+"ms");
        output.close();
        out.close();
        connection.close();

        // ----------------------------------------------------------------------
        // Notify progress
        // ----------------------------------------------------------------------
        if (listener != null) {
            listener.notifyProgress(10);
        }
        return fullPath;
    }

    /**
     * 
     * @throws FileIOException
     *                 if there is a problem saving to the FileSystem
     * @throws IllegalStateException
     *                 if this trail is empty
     */
    public void saveToRMS() throws FileIOException, IllegalStateException {
        if (this.trackMarkers.size() == 0 && this.trackPoints.size() == 0) {
            // May not save an empty trail.
            throw new IllegalStateException(
                    LocaleManager.getMessage("track_savetorms_error"));
        } else {
            final String filename;
            if (this.name == null || this.name.length() == 0) {
                filename = DateTimeUtil.getCurrentDateStamp();
            } else {
                filename = name;
            }
            FileSystem.getFileSystem().saveFile(filename, getMimeType(), this,
                    false);
        }
    }

    /**
     * Utility method to 'pause' the current track to the rms Not throwing any
     * exceptions, pausing is done on a best effort basis, If it fails there is
     * probably nothing that can be done about it in the circumstances
     */
    public void pause() {
        if (this.trackMarkers.size() == 0 && this.trackPoints.size() == 0) {
            return;
        } else {

            FileSystem fs = FileSystem.getFileSystem();

            // If there is already a pause track, overwrite it with this one
            try {
                if (fs.containsFile(Track.PAUSEFILENAME)) {
                    fs.deleteFile(Track.PAUSEFILENAME);
                }

                fs.saveFile(PAUSEFILENAME, getMimeType(), this, false);
            } catch (FileIOException e) {
                Logger.error("Error creating pause file " + e.getMessage());
            }
        }
    }

    /**
     * Serialize this object to a DataOutputStream
     * @throws java.io.IOException 
     */
    public void serialize(DataOutputStream dos) throws IOException {
        final int numPoints = trackPoints.size();
        dos.writeInt(numPoints);
        for (int i = 0; i < numPoints; i++) {
            ((GpsPosition) trackPoints.elementAt(i)).serialize(dos);
        }
        final int numMarkers = trackMarkers.size();
        dos.writeInt(numMarkers);
        for (int i = 0; i < numMarkers; i++) {
            ((Marker) trackMarkers.elementAt(i)).serialize(dos);
        }
        dos.writeDouble(distance);
        if (this.name == null) {
            dos.writeBoolean(false);
        } else {
            dos.writeBoolean(true);
            dos.writeUTF(name);
        }
    }

    /**
     * UnSerialize this object from a DataOutputStream
     * @throws java.io.IOException 
     */
    public void unserialize(DataInputStream dis) throws IOException {
        final int numPoints = dis.readInt();
        trackPoints = new Vector(numPoints);
        for (int i = 0; i < numPoints; i++) {
            GpsPosition pos = new GpsPosition(dis);
            this.addPosition(pos);
        }

        final int numMarkers = dis.readInt();
        trackMarkers = new Vector(numMarkers);
        for (int i = 0; i < numMarkers; i++) {
            Marker marker = new Marker(dis);
            this.addMarker(marker);
        }
        distance = dis.readDouble();
        if (dis.readBoolean()) {
            try {
                this.name = dis.readUTF();
            } catch(EOFException ex) {
                this.name = DateTimeUtil.getCurrentDateStamp();
                //throw new EOFException("Can't read trail name:" + ex.getMessage());
            }
        } else {
            this.name = null;
        }
    }

    /** Return the MIME type of this object */
    public String getMimeType() {
        return MIME_TYPE;
    }
}
