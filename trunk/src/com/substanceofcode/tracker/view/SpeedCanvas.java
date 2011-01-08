/*
 * SpeedCanvas.java
 *
 * Copyright (C) 2010 Vikas Yadav
 * Copyright (C) 2005-2008 Tommi Laukkanen
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
package com.substanceofcode.tracker.view;

import java.util.Date;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import com.substanceofcode.gps.GpsPosition;
import com.substanceofcode.tracker.model.Track;
import com.substanceofcode.util.DateTimeUtil;
import com.substanceofcode.util.ImageUtil;
import com.substanceofcode.localization.LocaleManager;
import com.substanceofcode.tracker.model.LengthFormatter;
import com.substanceofcode.tracker.model.UnitConverter;
import com.substanceofcode.tracker.model.RecorderSettings;
import com.substanceofcode.tracker.view.Logger;

/**
 * <p> Elevation canvas shows the change in elevation over the course of the Trail
 *
 * @author Vikas Yadav
 * @author Barry Redmond
 */
public class SpeedCanvas extends BaseCanvas {

    private static final int X_SCALE_TYPE_MASK = 1;
    private static final int X_SCALE_SCALE_MASK = ~X_SCALE_TYPE_MASK;

    private static final int X_SCALE_TIME = 0;
    private static final int X_SCALE_DISTANCE = 1;
    private static final int X_MIN_ZOOM = Integer.MAX_VALUE & X_SCALE_SCALE_MASK;
    private static final int X_MAX_ZOOM = 0;
    //Values for minimum and maximum speed which appear "reasonable" to humans
    private static final double[] speedLevels = {1.0, 5.0, 10.0, 25.0, 50.0,
                                               100.0, 125.0, 250.0, 500.0, 1000.0};
    private static int speedZoomValue = 0;

    private final int MARGIN = this.getWidth() > 200 ? 5 : 2;
    //In pixel
    private final int verticalMovementSize, horizontalMovementSize;
    //In meter
    private int verticalMovement, horizontalMovement;
    //Number of increments between minimun and maximum speed
    private int maxPositions = 10;

    private GpsPosition lastPosition;
    private Image redDotImage;
    //Pixel per meter?
    private int xScale, yScale;
    private boolean gridOn;
    
    //In meter
    private double minSpeed, maxSpeed;
    private double minSpeedCurUnit, maxSpeedCurUnit;
    
    private boolean manualZoom = false;

    private RecorderSettings settings;

    public SpeedCanvas(GpsPosition initialPosition) {
        super();
        
        this.lastPosition = initialPosition;
        
        this.verticalMovementSize = this.getHeight() / 8;
        this.horizontalMovementSize = this.getWidth() / 8;
        this.verticalMovement = this.horizontalMovement = 0;
        this.xScale = X_SCALE_TIME | X_MAX_ZOOM;
        this.settings = controller.getSettings();

        this.gridOn = true;

        redDotImage = ImageUtil.loadImage("/images/red-dot.png");

        this.setMinMaxValues(speedZoomValue);
    }

    /** Get the minimum and maximum speed values of the current track
     * 
     */
    private void setMinMaxValues(int speedLevelOffset) {
        double speedMin , speedMax, speedDiff;

        Track curTrack = controller.getTrack();
        LengthFormatter lengthFormatter = new LengthFormatter(settings);
        int speedUnitType = lengthFormatter.getSpeedUnitType();


        //Set initial values if the track is still empty
        if(curTrack.getPositionCount() == 0){
            //Initial values in current unit
            minSpeedCurUnit = 0;
            maxSpeedCurUnit = 500;
        } else {
            try {
                //Get maximum speed in Meter and convert to current Unit
                speedMax = UnitConverter.convertSpeed(curTrack.getMaxSpeed(), UnitConverter.UNITS_KPH, speedUnitType);
                //Get minimum speed in Meter and convert to current Unit
                speedMin = UnitConverter.convertSpeed(curTrack.getMinSpeed(), UnitConverter.UNITS_KPH, speedUnitType);
                //Calculate the difference between minium and maximum speed
                speedDiff = calculateSpeedDiff(speedMax - speedMin, speedLevelOffset);
                //Calculate the minimum speed
                minSpeedCurUnit = calculateMinSpeed(speedMin, speedDiff);
                //Check if we need a bigger range
                if( speedMax > minSpeedCurUnit + speedDiff)
                {
                    //Recalculate the difference between minium and maximum
                    //speed and get the range one index bigger
                    speedDiff = calculateSpeedDiff(speedMax - speedMin, speedLevelOffset + 1);
                }
                maxSpeedCurUnit = minSpeedCurUnit + speedDiff;
            }
            catch (Exception ex) {
                //ignore it
            }
        }
    }

    protected void paint(Graphics g) {
        try
        {
            //Logger.debug("SC paint");
            g.setColor( Theme.getColor(Theme.TYPE_BACKGROUND) );

            g.fillRect(0, 0, this.getWidth(), this.getHeight());

            // Top position in pixel where to start drawing grid
            final int top = drawTitle(g, 0);

            g.setFont(Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN,
                    Font.SIZE_SMALL));
            //Bottom position in pixel
            final int bottom = this.getHeight() - (2 * MARGIN);
            //Refresh min/max speed before repaint
            if (!manualZoom)
            {
                setMinMaxValues(0);
            }
            drawYAxis(g, top, bottom);

            drawXAxis(g, MARGIN, this.getWidth() - 2 * MARGIN, top, bottom);

            final int[] clip = { g.getClipX(), g.getClipY(), g.getClipWidth(),
                    g.getClipHeight() };

            g.setClip(MARGIN, top, this.getWidth() - 2 * MARGIN, bottom);

            drawTrail(g, top, bottom);
            g.setClip(clip[0], clip[1], clip[2], clip[3]);
        }
        catch(Exception e)
        {
            Logger.debug(e.getMessage());
        }
    }

    /**
     * Draws the Title for this screen and returns the yPosition of the bottom
     * of the title.
     * 
     * @param g
     * @param top
     * @return
     */
    private int drawTitle(Graphics g, int yPos) {
        g.setFont(titleFont);
        g.setColor( Theme.getColor(Theme.TYPE_TITLE) );
        final String title = LocaleManager.getMessage("speed_canvas_title");
        g.drawString(title, this.getWidth() / 2, yPos, Graphics.TOP
                | Graphics.HCENTER);
        return yPos + g.getFont().getHeight();
    }

    private void drawYAxis(Graphics g, final int top, final int bottom) {
        try
        {
            g.setColor( Theme.getColor(Theme.TYPE_LINE) );

            // Draw the vertical Axis
            g.drawLine(MARGIN, top, MARGIN, bottom);

            // Draw the top speed in current unit
            drawSpeedBar(g, top, this.maxSpeedCurUnit,false);

            // Draw the bottom speed
            drawSpeedBar(g, bottom, this.minSpeedCurUnit,false);

            // Draw intermediate speed positions.

            /*
             * We'll try and draw 5 intermediate speed's, assuming there's room on
             * the screen for this to look OK
             */
            final int availableHeight = bottom - top;
            final int spaceHeight = g.getFont().getHeight() * 2;

            int pixelIncrement = availableHeight / maxPositions;
            //Increment in current unit
            double speedIncrement = (this.maxSpeedCurUnit - this.minSpeedCurUnit)
                    / maxPositions;
            int yPos = bottom - pixelIncrement;
            double ySpeed = this.minSpeedCurUnit + speedIncrement;
            for (int i = 1; i < maxPositions; i++, yPos -= pixelIncrement, ySpeed += speedIncrement) {
                drawSpeedBar(g, yPos, ySpeed,i % 2 != 0);
            }
            g.setStrokeStyle(g.SOLID);
        }
        catch(Exception e)
        {
            Logger.debug("drawYAxis(" + String.valueOf(top) + "," + String.valueOf(bottom) + " - " + e.getMessage());
        }
    }

    private void drawSpeedBar(Graphics g, int pixel, double speed, boolean minorLine) {
        int decimalCount = 0;
        g.drawLine(1, pixel, 2 * (MARGIN - 1) + 1, pixel);

        //Get the speed as string with unit appended
        LengthFormatter height = new LengthFormatter(settings.getDistanceUnitType());
        //Check if there are decimals after ".". If yes, show them
        if(speed % 1 != 0)
        {
            decimalCount = 1;
        }

        final String altString = height.getSpeedString(speed, true, decimalCount, true);
        if(!minorLine)
            g.drawString(altString, MARGIN + 2, pixel, Graphics.BOTTOM
                    | Graphics.LEFT);
        if (this.gridOn) {
            final int color = g.getColor();
            g.setColor( Theme.getColor(Theme.TYPE_SUBLINE));
            final int right = this.getWidth() - (2 * MARGIN);
            if(minorLine)
                g.setStrokeStyle(g.DOTTED);
            else
                g.setStrokeStyle(g.SOLID);
            
            g.drawLine(2 * (MARGIN - 1) + 1, pixel, right, pixel);
            g.setColor(color);
        }
    }

    private void drawXAxis(Graphics g, final int left, final int right,
            final int top, final int bottom) {
        g.setColor(Theme.getColor(Theme.TYPE_LINE));
        g.drawLine(left, bottom, right, bottom);

        String time = null;
        try {
            DateTimeUtil.convertToTimeStamp(this.lastPosition.date, false);// "By_Time";
        } catch (Exception e) {
        }
        if (time == null) {
            time = ""; // "N/A";
        }

        drawTimeDistanceBar(g, right, time, top, bottom);
        // TODO: draw Scale
    }

    private void drawTimeDistanceBar(Graphics g, int pixel, String value,
            final int top, final int bottom) {
        g.drawLine(pixel, this.getHeight() - 2 * (MARGIN - 1), pixel, this
                .getHeight() - 1);

        g.drawString(value, pixel, this.getHeight() - MARGIN, Graphics.BOTTOM
                | Graphics.HCENTER);
        if (this.gridOn) {
            final int color = g.getColor();
            g.setColor(Theme.getColor(Theme.TYPE_SUBLINE));
            g.drawLine(pixel, top, pixel, bottom - MARGIN);
            g.setColor(color);
        }
    }

    private void drawTrail(Graphics g, final int top, final int bottom) {
        try {
            //Logger.debug("SC drawTrail");
            // Exit if we don't have anything to draw
            final GpsPosition temp = controller.getPosition();
            if (temp != null) {
                lastPosition = temp;
            }
            if (lastPosition == null) {
                return;
            }

            double currentLatitude = lastPosition.latitude;
            double currentLongitude = lastPosition.longitude;
            //
            LengthFormatter lengthFormatter = new LengthFormatter(settings);
            int speedUnitType = lengthFormatter.getSpeedUnitType();

            //Convert to current unit
            double currentSpeed = UnitConverter.convertSpeed(lastPosition.speed, UnitConverter.UNITS_KPH, speedUnitType);
            double currentSpeedAverage = UnitConverter.convertSpeed(lastPosition.currentAverageSpeed, UnitConverter.UNITS_KPH, speedUnitType);
            Date currentTime = lastPosition.date;

            double lastLatitude = currentLatitude;
            double lastLongitude = currentLongitude;
            //In current unit
            double lastSpeed = currentSpeed;
            double lastSpeedAverage = currentSpeedAverage;
            Date lastTime = currentTime;

            final Track track = controller.getTrack();
            final int numPositions;
            synchronized (track) {
                /*
                 * Synchronized so that no element can be added or removed
                 * between getting the number of elements and getting the
                 * elements themselfs.
                 */
                numPositions = track.getPositionCount();

                
                final int numPositionsToDraw = controller.getSettings()
                        .getNumberOfPositionToDraw();

                final int lowerLimit;
                if (numPositions - numPositionsToDraw < 0) {
                    lowerLimit = 0;
                } else {
                    lowerLimit = numPositions - numPositionsToDraw;
                }
                for (int positionIndex = numPositions - 1; positionIndex >= lowerLimit; positionIndex--) {

                    GpsPosition pos = (GpsPosition) track
                            .getPosition(positionIndex);

                    //Actual Speed ***

                    //set colour for actual speed line
                    g.setColor(Theme.getColor(Theme.TYPE_TRAIL));

                    double lat = pos.latitude;
                    double lon = pos.longitude;
                    //Convert speed from meter in current unit
                    double speed = UnitConverter.convertSpeed(pos.speed, UnitConverter.UNITS_KPH, speedUnitType);
                    Date time = pos.date;
                    CanvasPoint point1 = convertPosition(lat, lon, speed, time,
                            top, bottom);

                    CanvasPoint point2 = convertPosition(lastLatitude,
                            lastLongitude, lastSpeed, lastTime, top, bottom);

                    g.drawLine(point1.X, point1.Y, point2.X, point2.Y);

                    //Convert speed from meter in current unit
                    double speedAverage = UnitConverter.convertSpeed(pos.currentAverageSpeed, UnitConverter.UNITS_KPH, speedUnitType);

                    //Average Speed ***

                    //set colour for average apeed line
                    g.setColor(Theme.getColor(Theme.TYPE_AVGSPEED));
                    
                    point1 = convertPosition(lat, lon, speedAverage, time,
                            top, bottom);

                    point2 = convertPosition(lastLatitude,
                            lastLongitude, lastSpeedAverage, lastTime, top, bottom);

                    g.drawLine(point1.X, point1.Y, point2.X, point2.Y);

                    lastLatitude = pos.latitude;
                    lastLongitude = pos.longitude;
                    lastSpeed = speed;
                    lastSpeedAverage = speedAverage;
                    lastTime = pos.date;
                    //Logger.debug(String.valueOf(speed) + " canvas point - " + String.valueOf(point1.X) + "," + String.valueOf(point1.Y) + " - " + String.valueOf(point2.X) + "," + String.valueOf(point2.Y));
                }
            }

            int height = 0;
            if (lastPosition != null) {
                //Pass speed in current unit
                height = getYPos(currentSpeed, top, bottom);
            }
/*
            if (height < -5000) { //set some reasonable limits
                height = -5000; // This prevents the gui from stalling from wildly bad data
            } // to do: should figure out why the data is bad in the first place.

            if (height > 60000){
                height = 60000;
            }
*/
            int right = this.getWidth() - (2 * MARGIN);
            // Draw red dot on current location
            g.drawImage(redDotImage, right + horizontalMovement, height,
                    Graphics.VCENTER | Graphics.HCENTER);

        } catch (Exception ex) {
            g.setColor( Theme.getColor(Theme.TYPE_ERROR) );
            g.drawString(LocaleManager.getMessage("elevation_canvas_error") +
                    " " + ex.toString(), 1, 120, Graphics.TOP | Graphics.LEFT);

            Logger.error(
                    "Exception occured while drawing elevation: "
                            + ex.toString());
        }
    }

    private CanvasPoint convertPosition(double latitude, double longitude,
            double speed, Date time, final int top, final int bottom) {
        int xPos;
        if ((this.xScale & X_SCALE_TYPE_MASK) == X_SCALE_TIME) {
            // x-Scale is time based
            int secondsSinceLastPosition = (int) ((this.lastPosition.date
                    .getTime() - time.getTime()) / 1000);

            int scale = (this.xScale & X_SCALE_SCALE_MASK) >> 1;

            if (scale == 0)
                scale = 1;
            xPos = this.getWidth() - (2 * MARGIN)
                    - (secondsSinceLastPosition / scale) + horizontalMovement;

        } else {
            // x-Scale is distance based
            xPos = 0;
            return null;
        }
        //Pass speed in current unit
        int yPos = getYPos(speed, top, bottom);
        return new CanvasPoint(xPos, yPos);
    }

    private int getYPos(double speed, final int top, final int bottom) {
        final double availableHeight = bottom - top;
        final double speedDiff = this.maxSpeedCurUnit - this.minSpeedCurUnit;
        //Number of pixels for one current unit
        final double oneUnit = availableHeight / speedDiff;
        //Height in pixels
        int pixels = (int) ((speed - minSpeedCurUnit) * oneUnit);
        return bottom - (MARGIN + pixels) + verticalMovement;
    }

    public void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        
        /** Handle zooming keys */
        switch (keyCode) {
            case (KEY_NUM1):

                // Zoom in vertically
                manualZoom = true;
                if(speedZoomValue <= 0)
                {
                    speedZoomValue = 0;
                }
                else
                {
                    speedZoomValue = speedZoomValue - 1;
                }
                setMinMaxValues(speedZoomValue);
                break;

            case (KEY_NUM2):
                // Fix speed scale
                manualZoom = false;
                setMinMaxValues(0);
                break;

            case (KEY_NUM3):
                // Zoom out vertically
                manualZoom = true;
                if(speedZoomValue > speedLevels.length - 1)
                {
                    speedZoomValue = speedLevels.length - 1;
                }
                else
                {
                    speedZoomValue = speedZoomValue + 1;
                }
                setMinMaxValues(speedZoomValue);
                break;

            case (KEY_NUM7):
                // Zoom in horizontally
                int xScaleType = this.xScale & X_SCALE_TYPE_MASK;
                int xScaleScale = this.xScale & X_SCALE_SCALE_MASK;
                if (xScaleScale == X_MIN_ZOOM) {
                    break;
                }
                xScaleScale = ((xScaleScale >> 1) + 1) << 1;
                this.xScale = (byte) (xScaleScale | xScaleType);
                break;

            case (KEY_NUM9):
                // Zoom out horizontally
                xScaleType = this.xScale & X_SCALE_TYPE_MASK;
                xScaleScale = this.xScale & X_SCALE_SCALE_MASK;
                if (xScaleScale == X_MAX_ZOOM) {
                    break;
                }
                xScaleScale = ((xScaleScale >> 1) - 1) << 1;
                this.xScale = (byte) (xScaleScale | xScaleType);
                break;

            default:
        }

        /** Handle panning keys */
        int gameKey = -1;
        try {
            gameKey = getGameAction(keyCode);
        } catch (Exception ex) {
            /**
             * We don't need to handle this error. It is only caught because
             * getGameAction() method generates exceptions on some phones for
             * some buttons.
             */
        }
        if (gameKey == UP || keyCode == KEY_NUM2) {
            // Disable vertical-movement until the scales at the side reflect it
            // properly
            // verticalMovement += verticalMovementSize;
        }
        if (gameKey == DOWN || keyCode == KEY_NUM8) {
            // Disable vertical-movement until the scales at the side reflect it
            // properly
            // verticalMovement -= verticalMovementSize;
        }
        if (gameKey == LEFT || keyCode == KEY_NUM4) {
            horizontalMovement += horizontalMovementSize;
        }
        if (gameKey == RIGHT || keyCode == KEY_NUM6) {
            horizontalMovement -= horizontalMovementSize;
        }
        if (gameKey == FIRE || keyCode == KEY_NUM5) {
            verticalMovement = 0;
            horizontalMovement = 0;
        }
        this.repaint();
    }

    /**
     * Calculate the difference between min and max speed.
     * Set it to a "reasonable value" stored in speedLevels.
     * By passing an zoomValue value other than zero you can zoom in (<0)
     * or zoom out (>0)
     *
     * @param diffAlt Difference between minimum and maximum speed
     * @param zoomValue
     * @return minimum speed
     */
    private double calculateSpeedDiff(double diff, int zoomValue)
    {
        int index = 0;

        //Get a reasonable value bigger than the difference between
        //speedMin and speedMax;
        for(int i=speedLevels.length - 1;i > 0;i--)
        {
                            if(diff > speedLevels[i])
            {
                index =  i + 1;
                break;
            }
        }

        //Check lower array boundary
        if( index + zoomValue < 0)
        {
            return speedLevels[0];
        }
        //Check upper array boundary
        if( index + zoomValue > speedLevels.length - 1)
        {
            return speedLevels[speedLevels.length - 1];
        }
        return speedLevels[index + zoomValue];
    }

    /**
     * Calculate the lower altitude boundary. It is set to
     * "reasonable value".
     *
     * @param curAlt Altitude in current unit
     * @param diffAlt Difference between minimum and maximum altitude
     * @return minimum altitude
     */
    private double calculateMinSpeed(double curSpeed, double diffSpeed)
    {
        //Divide the altitude difference by the number of lines to draw + 1
        int speedIncrement = (int) (diffSpeed/maxPositions);
        if(speedIncrement == 0){
            return (int)curSpeed;
        }
        //Get integer division
        int intDiv = ((int)curSpeed / speedIncrement);
        return intDiv * speedIncrement;
    }

    public void setLastPosition(GpsPosition position) {
        this.lastPosition = position;
        this.setMinMaxValues(speedZoomValue);
    }
}