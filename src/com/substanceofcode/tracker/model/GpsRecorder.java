/*
 * GpsRecorder.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import com.substanceofcode.gps.GpsGPGSA;
import com.substanceofcode.gps.GpsPosition;
import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.view.Logger;
import com.substanceofcode.util.StringUtil;
import com.substanceofcode.localization.LocaleManager;
import com.substanceofcode.util.DateTimeUtil;

import com.substanceofcode.tracker.view.PlaceSurveyorCanvas;

/**
 * Timer based class that encapsulates recording data from a GPS device.
 * 
 * @author Tommi Laukkanen
 */
public class GpsRecorder {

    /**
     * State : true = recording in progress, false = recording not in progress
     */
    private boolean recording = false;

    /** Current track being recorded to */
    private Track recordedTrack = new Track();

    /** Interval between recorded positions */
    private int recordingInterval;

    /** Interval between recorded Markers */
    private int markerInterval;

    /** Url to upload recorded points to */
    private String uploadURL;
    
    /** Reference to controller object */
    private Controller controller;
    
    /** Timer for worker */
    private Timer recorderTimer;
//    private HttpConnection conn;
    
    /** Recorder helpers */
    final GpsRmsRecorder rmsRecorder = new GpsRmsRecorder();
    GpsPosition lastRecordedPosition = null;
    GpsPosition lastPosition = null;
    int secondsFromLastTrailPoint = 0;
    int secondsFromLastWebRecording = 0;
    int recordedCount = 0;
    boolean isValidPosition = false;
    GpsPosition currentPosition = null;
    GpsGPGSA currentGPGSA = null;
    boolean useFilter = true;
        
    /**
     * Constructor - sets up local variables then launches the instance in a
     * thread.
     * @param controller 
     */
    public GpsRecorder(Controller controller) {
        this.controller = controller;
        RecorderSettings settings = controller.getSettings();
        recordingInterval = settings.getRecordingInterval();
        markerInterval = settings.getMarkerInterval();
        uploadURL = settings.getUploadURL();
        useFilter = settings.getFilterTrail();

        /** Start recorder timer as fixed rate (in every 1 second) */
        RecorderTask recorderTask = new RecorderTask();
        recorderTimer = new Timer();
        recorderTimer.scheduleAtFixedRate(recorderTask, 1000, 1000);
    }

    /**
     * Set filtering usage.
     * @param useFiltering
     */
    public void setFiltering(boolean useFiltering) {
        this.useFilter = useFiltering;
    }

    /** 
     * Set interval for recording
     * @param seconds 
     */
    public void setRecordingInterval(int seconds) {
        recordingInterval = seconds;
    }

    /** 
     * Set interval for marker recording
     * @param interval
     */
    public void setMarkerInterval(int interval) {
        markerInterval = interval;
    }

    /** 
     * Check status
     * @return 
     */
    public boolean isRecording() {
        return recording;
    }

    /** Clear track */
    public void clearTrack() {
        recordedTrack = new Track();
    }

    /** 
     * Get track
     * @return 
     */
    public Track getTrack() {
        return recordedTrack;
    }

    public void setTrack(Track track) {
        this.stopRecording();
        this.recordedTrack = track;
    }

    /** Start recording positions */
    public void startRecording() {
        recording = true;
    }

    /** Stop recording positions */
    public void stopRecording() {
        recording = false;
    }

    /** 
     * Set recording status
     * @param active 
     */
    public void setRecording(boolean active) {
        recording = active;
    }

    private boolean checkValidPosition(GpsPosition currentPosition,
            GpsPosition lastPosition, GpsPosition lastRecordedPosition) {
        /** Check for valid position */
        if (currentPosition == null) {
            return false;
        }

        /** Check for max speed */
        RecorderSettings settings = controller.getSettings();
        int maxSpeed = settings.getMaxRecordedSpeed();
        if (currentPosition.speed > (int) maxSpeed) {
            return false;
        }

        /** Check for max acceleration */
        if (lastPosition != null) {
            int maxAcceleration = settings.getMaxAcceleration();
            int acceleration = (int) (lastPosition.speed - currentPosition.speed);
            if (Math.abs(acceleration) > maxAcceleration) {
                return false;
            }
        }

        /** Check for min distance */
        if (lastRecordedPosition != null && currentPosition != null) {
            int minDistance = settings.getMinRecordedDistance();
            double distance = 1000 * lastRecordedPosition
                    .getDistanceFromPosition(currentPosition);
            if (distance < minDistance) {
                return false;
            }
        }

        /** We have a valid position */
        return true;
    }

    /**
     * Thread based class which encapsulates recording a point into a single
     * slot in the RMS. This thread makes calls to the method
     * putPositionInRMS(...) which is synchronised on the parent class. This is
     * needed so that calls to getPositionFromRMS() again contained in the
     * parent class return a consistent value.
     */
    private class GpsRmsRecorder implements Runnable {

        /**
         * Local reference to the Thread running this instance
         */
        private Thread rmsRecorderThread;

        /**
         * Next position to be written
         */
        private GpsPosition nextPositionToWrite = null;

        /**
         * Last position which has already been written
         */
        private GpsPosition lastPositionWritten = null;

        /**
         * Constructor - Start a thread running a new instance of this class
         */
        private GpsRmsRecorder() {
            rmsRecorderThread = new Thread(this);
            rmsRecorderThread.start();
        }

        /**
         * @param xiPos
         *                Position to be written to RMS
         */
        public synchronized void setGpsPosition(GpsPosition pos) {
            // ------------------------------------------------------------------
            // If work has already been set - wait until we are notified that it
            // has been written
            // ------------------------------------------------------------------
            while (this.nextPositionToWrite != this.lastPositionWritten) {
                try {
                    this.wait();
                } catch (InterruptedException lInterEx) {
                    /* Throw away and continue */
                }
            }
            nextPositionToWrite = pos;

            // ------------------------------------------------------------------
            // Notify everyone including the worker thread that there is work
            // to be completed
            // ------------------------------------------------------------------
            this.notifyAll();
        }

        /**
         * Main worker thread method
         */
        public void run() {
            synchronized (this) {
                while (true) {
                    try {
                        if (nextPositionToWrite != lastPositionWritten) {
                            putPositionInRMS(nextPositionToWrite);
                            lastPositionWritten = nextPositionToWrite;
                        } else {
                            this.wait();
                        }
                    } catch (InterruptedException lInterEx) {
                        /* Throw away and continue */
                    } catch (Exception lAnyOtherException) {
                        // XXX : mchr : log this exception?
                        lAnyOtherException.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Name of record store to use
     */
    private static final String POSITION_RMS_STORE = "gps-position-rms-store";

    /**
     * <p>
     * Saves the specified GPS position to the RMS.
     * 
     * <p>
     * This method should later be expanded to store multiple positions to the
     * RMS, but for the time being, it ensures that there is ONLY ONE position
     * in the RMS at a time.
     * 
     * @param pos 
     *                The {@link GpsPosition} to save to the RMS.
     * 
     * @see Settings#getPositionFromRMS()
     * 
     */
    public synchronized void putPositionInRMS(GpsPosition pos) {
        if (pos != null) {
            try {
                // Get the position as a byte array so it can be saved in a
                // RecordStore.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                pos.serialize(dos);
                byte[] data = baos.toByteArray();
                dos.close();
                baos.close();

                RecordStore positionStore = RecordStore.openRecordStore(
                        POSITION_RMS_STORE, true);
                /*
                 * Enusre that there is no other positions in there. (this
                 * should be removed in the future when multiple records are
                 * allowed in the store.
                 */
                RecordEnumeration re = positionStore.enumerateRecords(null,
                        null, false);
                while (re.hasNextElement()) {
                    positionStore.deleteRecord(re.nextRecordId());
                }
                re.destroy();

                positionStore.addRecord(data, 0, data.length);
                positionStore.closeRecordStore();
            } catch (RecordStoreException e) {
                Logger.error("GpsRecorder RecordStoreException:"+e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Logger.error("GpsRecorder IOError:"+ e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>
     * Returns the GpsPosition that was saved in the RMS by the last call to
     * {@link Settings#putPositionInRMS(GpsPosition)}.
     * 
     * @return the last GpsPosition saved to the RMS, or NULL if there is none
     *         saved there.
     * 
     * @see Settings#save(boolean)
     * 
     */
    public synchronized GpsPosition getPositionFromRMS() {
        GpsPosition result = null;
        try {
            RecordStore positionStore = RecordStore.openRecordStore(
                    POSITION_RMS_STORE, false);
            RecordEnumeration re = positionStore.enumerateRecords(null, null,
                    false);
            byte[] data = positionStore.getRecord(re.nextRecordId());
            re.destroy();
            positionStore.closeRecordStore();

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            result = new GpsPosition(dis);
            dis.close();
            bais.close();
            return result;
        } catch (RecordStoreException e) {
            // RecordStore does not exist, no positions saved there yet.
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    /** 
     * Timer task for performing the actual recording: Add current position to
     * current trail.
     */
    protected class RecorderTask extends TimerTask {
        
        public void run() {
            try {
                secondsFromLastTrailPoint++;

                if (recording==true){
                    currentPosition = controller.getPosition();
                    isValidPosition = checkValidPosition(
                            currentPosition, 
                            lastPosition, 
                            lastRecordedPosition);
                }

                if (recording == true && isValidPosition) {

                    /**
                     * Check if user haven't moved -> don't record the same
                     * position
                     */
                    boolean stopped = false;
                    if (lastRecordedPosition != null && currentPosition != null) {
                        stopped = currentPosition.equals(lastRecordedPosition);
                    }
                    
                    //Logger.debug("interval: "+ recordingInterval +
                    //" currentPosition is " + (currentPosition==null?"null":"not null"));
                    /**
                     * Record current position if user have moved or this is a
                     * first recorded position.
                     */
                    if ( currentPosition != null && !stopped ) {

                        if( secondsFromLastTrailPoint >= recordingInterval ) {

                            secondsFromLastTrailPoint = 0;

                            rmsRecorder.setGpsPosition(currentPosition);

                            /** Apply the filtering before the new position is added */
                            int posCount = recordedTrack.getPositionCount();
                            if(useFilter && posCount>2) {
                                GpsPosition lastPos = recordedTrack.getPosition(posCount-1);
                                GpsPosition oneFromLastPos = recordedTrack.getPosition(posCount-2);
                                if(RecorderFilter.canRemovePreviousPosition(oneFromLastPos, lastPos, currentPosition)){
                                    recordedTrack.removeLastPosition();
                                }
                            }

                            GpsPosition posr = recordedTrack.addPosition(currentPosition);
                            PlaceSurveyorCanvas ps = controller.getPlaceSurveyor();
                            if(posr != null && ps != null)
                                ps.setLastRecordedPosition(posr);

                            if (markerInterval > 0 && recordedCount > 0
                                    && recordedCount % markerInterval == 0) {
                                Marker marker = new Marker(
                                        currentPosition,
                                        "",
                                        "");
                                recordedTrack.addMarker( marker );
                            }

                            lastRecordedPosition = currentPosition;
                            recordedCount++;
                        }

                        //If the uploadURL is set (not "") then try to upload the
                        //GpsPosition too.
                        RecorderSettings settings = controller.getSettings();
                        boolean uploadToWeb = settings.getWebRecordingUsage();
                        int webRecordingInterval = settings.getWebRecordingInterval();
                        uploadURL = controller.getSettings().getUploadURL();

                        if(uploadToWeb && !uploadURL.equals("")){

                            secondsFromLastWebRecording++;

                            if(webRecordingInterval>0 && secondsFromLastWebRecording>=webRecordingInterval)
                            {
                                secondsFromLastWebRecording=0;
                                WebUpload();
                            }
                        }
                    }
                    lastPosition = currentPosition;
                } else {
                   // Logger.debug("GpsRecorder getPosition called 2");
                   lastPosition = controller.getPosition();
                }
                controller.repaintDisplay();
            } catch (Exception ex) {
                controller.showError(LocaleManager.getMessage("gps_recorder_error",
                        new Object[] {ex.toString(), controller, controller.getPosition()}));
            }     
        }

    }


    //TODO handle http return value
    
    private void WebUpload() {
        HttpConnection conn = null;
        DataOutputStream dos = null;

        Logger.debug("GpsRecorder WebUpload");

        try {
            boolean serialize = true;
            GpsGPGSA gpgsa = currentPosition.getGpgsa();
            if (uploadURL.indexOf("@LAT@") > 0) {
                String lat = String.valueOf(currentPosition.latitude);
                uploadURL = StringUtil.replace(uploadURL, "@LAT@", lat);
                String lon = String.valueOf(currentPosition.longitude);
                uploadURL = StringUtil.replace(uploadURL, "@LON@", lon);
                String alt = String.valueOf(currentPosition.altitude);
                uploadURL = StringUtil.replace(uploadURL, "@ALT@", alt);
                String id = String.valueOf(recordedTrack.getId());
                uploadURL = StringUtil.replace(uploadURL, "@TRAILID@", id);
                String hea = String.valueOf(currentPosition.course);
                uploadURL = StringUtil.replace(uploadURL, "@HEA@", hea);
                String spd = String.valueOf(currentPosition.speed);
                uploadURL = StringUtil.replace(uploadURL, "@SPD@", spd);
                if (gpgsa != null) {
                    String fix = String.valueOf(gpgsa.getFixtype());
                    uploadURL = StringUtil.replace(uploadURL, "@FIX@", fix);
                    String hdop = String.valueOf(gpgsa.getHdop());
                    uploadURL = StringUtil.replace(uploadURL, "@HDOP@", hdop);
                    String pdop = String.valueOf(gpgsa.getPdop());
                    uploadURL = StringUtil.replace(uploadURL, "@PDOP@", pdop);
                }
                String sat = String.valueOf(controller.getSatelliteCount());
                uploadURL = StringUtil.replace(uploadURL, "@SAT@", sat);
                String time = DateTimeUtil.getUniversalDateStamp(currentPosition.date);
                uploadURL = StringUtil.replace(uploadURL, "@TIME@", time);
                serialize = false;
            }
            conn = (HttpConnection) Connector.open(uploadURL);
            conn.setRequestMethod(HttpConnection.POST);
            conn.setRequestProperty("Content-Type", "text/plain");
            if (serialize) {
                dos = conn.openDataOutputStream();
                currentPosition.serialize(dos);
                dos.write("\r\n".getBytes());
                dos.flush();
            } else {
                conn.setRequestProperty("Content-Length", "0");
            }
            InputStream dis = conn.openInputStream();
            int ch;
            StringBuffer b = new StringBuffer();
            while ((ch = dis.read()) != -1) {
                b = b.append((char) ch);
            }
            Logger.debug(b.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {
                if (dos != null) {
                    dos.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
