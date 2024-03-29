/*
 * WaypointSurveyor.java
 *
 * Copyright (C) 2005-2008 Vikas Yadav
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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;

import com.substanceofcode.gps.GpsPosition;
import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.model.Place;
import com.substanceofcode.localization.LocaleManager;

import com.substanceofcode.tracker.model.OsmPoiPage;
import com.substanceofcode.tracker.model.OsmPoi;
import com.substanceofcode.tracker.grid.GridPosition;
import org.kxml2.io.KXmlParser;
import java.io.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import com.substanceofcode.util.StringUtil;
/**
 * Shows a list of POIs on screen with a designated key
 * Pages can be navigated before locating the desired POI
 * POIs are saved as regular place in text format.
 * @author Vikas Yadav
 * @author Patrick Steiner
 */
public class PlaceSurveyorCanvas extends BaseCanvas  {
    
    private final Command backCommand;
    
    private Place made;
    /** Type of the current point */
    private int pointType;
    
    private final static Font SMALL_FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    /** Traffic Signal */
    protected static final int OSM_TRAFFIC_SIGNAL = 1;
    /* Small Bus Stop */
    protected static final int OSM_BUS_STOP     = 2;
    /** Amenity=atm */
    protected static final int OSM_MAXSPEED     = 3;
    /** Expressway Exit */
    protected static final int OSM_EXIT         = 4;
    /** Expressway Entry */
    protected static final int OSM_ENTRY        = 5;
    /** Link to the right lane */
    protected static final int OSM_CUT          = 6;
    /** Petrol Pump, Fuel Stop */
    protected static final int OSM_FUEL         = 7;
    /** Gate */
    protected static final int OSM_GATE         = 8;
    /** Names */
    protected static final int OSM_NAME         = 9;
    /** More */
    protected static final int OSM_MORE         = 10;
    /** Power Tower */
    protected static final int OSM_POWER        = 11;
    /** Level Crossing */
    protected static final int OSM_LEVEL        = 12;
    /** Power Line */
    protected static final int OSM_POWERLINE    = 13;
    /** Power sub station */
    protected static final int OSM_POWERSTATION = 14;
    /** Amenity=atm */
    protected static final int OSM_ATM          = 15;

    int whiteSpace = titleFont.getHeight() + 5;

    private OsmPoiPage osmPoiPage;
    private GridPosition position;

    private GpsPosition recordedPosition;

    /*
    INFO: menu arrays should be bigger then 10 items,
          because they are bound to the keypad
    */

    private static final String[] OSM_MAINMENU = {
        LocaleManager.getMessage("place_surveyor_canvas_tag_12"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_13"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_22"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_15"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_16"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_17"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_18"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_19"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_20"),
        LocaleManager.getMessage("place_surveyor_canvas_menu_more") };

    private static final String[] OSM_MOREMENU = {
        LocaleManager.getMessage("place_surveyor_canvas_tag_1"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_2"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_3"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_4"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_5"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_6"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_7"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_8"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_14") };

    private static final String[] OSM_NAMEMENU = {
        LocaleManager.getMessage("place_surveyor_canvas_tag_9a"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_10a"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_11a"),
        LocaleManager.getMessage("place_surveyor_canvas_tag_21a") };

    private static final String[] OSM_DIRECTIONSMENU = {
        LocaleManager.getMessage("place_surveyor_canvas_direction_left"),
        LocaleManager.getMessage("place_surveyor_canvas_direction_right") };

    private static final String[] OSM_MAXSPEEDMENU = {
        "30",
        "40",
        "50",
        "70",
        "80",
        "100",
        "130" };
    
    /** Creates a new instance of WaypointList
     * @param controller 
     */
    public PlaceSurveyorCanvas(OsmPoiPage firstPage) 
    {

        

        //super(TITLE, List.IMPLICIT);
        this.controller = firstPage.getController();
        this.osmPoiPage = firstPage;

        this.addCommand(backCommand = new Command(LocaleManager.getMessage("menu_back"), Command.BACK, 10));
    }

    public void setOsmPoiPage(OsmPoiPage firstPage)
    {
        //kamal yadav = 09467388777, 25/11 8
        this.osmPoiPage = firstPage;
    }

    public void setPosition(GridPosition pos)
    {
        this.position = pos;
    }

    public GridPosition getPosition()
    {
        return position;
    }

    public void setLastRecordedPosition(GpsPosition pos)
    {
        recordedPosition = pos;
    }
    
    public GpsPosition setLastRecordedPosition()
    {
        return recordedPosition;
    }

    public void commandAction(Command command, Displayable displayable) {
        if(command == backCommand) {
            /** Display the trail canvas */
            controller.showTrail();
        }
    }
    
    public void keyPressed(int keyCode) {
        Logger.debug("Surveyor keypress");
        
        OsmPoi nextPage =  this.osmPoiPage.handleKeyPressed(keyCode,position);
        
        if(nextPage == null) //no next page needed, return to controller default screen
        {
            this.controller.showTrail();
            return;
        }

        
        if(nextPage.getInputType() == 1 || nextPage.getInputType() == 2)
        {
            this.controller.setCurrentScreen(new SurveyorForm(nextPage));
        
        }
        else if(nextPage.getInputType() == 10)
        {
            this.osmPoiPage = (OsmPoiPage)nextPage;
            this.repaint();
        }
        
        return;
        //OsmPoiPage.handleKeyPressed(keyCode);

        /*if(pointType == OSM_MORE) {
            if (keyCode == Canvas.KEY_NUM1) //level crossing
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_1"));
                Logger.debug("Surveyor level");
                pointType = OSM_LEVEL;
            }
            else if (keyCode == Canvas.KEY_NUM2) //power tower
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_2"));
                pointType = OSM_POWER;
                repaint();
                Logger.debug("Surveyor Power Tower");
                return;
            }
            else if (keyCode == Canvas.KEY_NUM3) //power line
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_3"));
                pointType = OSM_POWERLINE;
                Logger.debug("Surveyor PowerL");
            }
            else if (keyCode == Canvas.KEY_NUM4) //power line
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_4"));
                pointType = OSM_POWERSTATION;
                Logger.debug("Surveyor PowerS");
            }
            else if (keyCode == Canvas.KEY_NUM5) //platform start
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_5"));
                Logger.debug("Surveyor Platform Start");
            }
            else if (keyCode == Canvas.KEY_NUM6) //Platform End
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_6"));
                Logger.debug("Surveyor Platform End");
            }
            else if (keyCode==Canvas.KEY_NUM7) //bridge start
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_7"));
                Logger.debug("Surveyor Bridge Start");
            }
            else if (keyCode == Canvas.KEY_NUM8) //brudge End
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_8"));
                Logger.debug("Surveyor Bridge End");
            }
            else if (keyCode == Canvas.KEY_NUM9) //ATM
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_14"));
                pointType = OSM_ATM;
                repaint();
                Logger.debug("Surveyor ATM");
                return;
            }

        }
        else if(pointType == OSM_NAME) {
            pointType = 0;
            if(keyCode == Canvas.KEY_NUM1) { //Road name
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_9"));
            }
            else if(keyCode == Canvas.KEY_NUM2) { //Railway Station name
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_10"));
            }
            else if(keyCode == Canvas.KEY_NUM3) { //Locality name
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_11"));
            }
            else if(keyCode == Canvas.KEY_NUM4) { //House number
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_21"));
            }
            this.controller.showSurveyorForm(made);
            return;
        }
        else if(pointType == OSM_MAXSPEED) {
            pointType = 0;
            if(keyCode == Canvas.KEY_NUM1) { //30
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_22") + " 30");
            }
            else if(keyCode == Canvas.KEY_NUM2) { //40
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_22") + " 40");
            }
            else if(keyCode == Canvas.KEY_NUM3) { //50
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_22") + " 50");
            }
            else if(keyCode == Canvas.KEY_NUM4) { //70
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_22") + " 70");
            }
            else if(keyCode == Canvas.KEY_NUM5) { //80
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_22") + " 80");
            }
            else if(keyCode == Canvas.KEY_NUM6) { //100
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_22") + " 100");
            }
            else if(keyCode == Canvas.KEY_NUM7) { //130
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_22") + " 130");
            }
        }
        else if(   pointType == OSM_FUEL 
                || pointType == OSM_GATE 
                || pointType == OSM_ATM 
                || pointType == OSM_BUS_STOP
                || pointType == OSM_POWER 
                ) {
            String name = made.getName();
            String name2 = "";
            Logger.debug("Direction?");
            if(keyCode == Canvas.KEY_NUM1) { //left
                Logger.debug("Left");
                name2=" " + LocaleManager.getMessage("place_surveyor_canvas_direction_left");
            }
            else if(keyCode == Canvas.KEY_NUM2) { //right
                Logger.debug("Right");
                name2 = " " + LocaleManager.getMessage("place_surveyor_canvas_direction_right");
            }
            made.setName(name + name2);
        }
        else {
            if (keyCode == Canvas.KEY_NUM1) //traffic
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_12"));
                Logger.debug("Surveyor Signal");
            }
            else if (keyCode == Canvas.KEY_NUM2) //bus
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_13"));
                pointType = OSM_ATM;
                repaint();
                Logger.debug("Surveyor Bus Stop");
                return;
            }
    
            else if (keyCode == Canvas.KEY_NUM3) //maxspeed
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_14"));
                pointType = OSM_MAXSPEED;
                repaint();
                Logger.debug("Surveyor Maxspeed");
                return;
            }
            else if (keyCode == Canvas.KEY_NUM4) //exit
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_15"));
                Logger.debug("Surveyor Exit");
            }
            else if (keyCode == Canvas.KEY_NUM5)//entry
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_16"));
                Logger.debug("Surveyor Entry");
            }
            else if (keyCode==Canvas.KEY_NUM6) //CUT
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_17"));
                Logger.debug("Surveyor Cut");
            }
            else if (keyCode == Canvas.KEY_NUM7) //fuel
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_18"));
                pointType = OSM_FUEL;
                repaint();
                Logger.debug("Surveyor Fuel");
                return;
            }
            else if (keyCode == Canvas.KEY_NUM8) //gate
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_19"));
                pointType = OSM_GATE;
                repaint();
                Logger.debug("Surveyor Gate");
                return;
            }
            else if (keyCode == Canvas.KEY_NUM9) //names
            {
                made.setName(LocaleManager.getMessage("place_surveyor_canvas_tag_20"));
                pointType = OSM_NAME;
                repaint();
                Logger.debug("Surveyor name");
                return;
            }
            else if (keyCode == Canvas.KEY_NUM0) //more
            {
                pointType = OSM_MORE;
                repaint();
                Logger.debug("Surveyor more");
                return;
            }
            else
            {
                controller.showTrail();
                return;
            }
        }
        pointType = 0;
        controller.addPlace(made);
        controller.showTrail();*/
    }

    void drawPlaceText(Graphics g, int key, String text, int whitespace) {
        g.setFont(SMALL_FONT);
        g.drawString(key + "   " + text, 10, whitespace, Graphics.TOP|Graphics.LEFT);
    }

    void drawPlaceMenu(Graphics g) {
        int y = 2; // start space between title
        
        OsmPoi[] pois = this.osmPoiPage.getAllPois();

        for(int i = 0; i < pois.length; i++) {
            if(pois[i] == null)
                continue;
            drawPlaceText(g, pois[i].getKey(), pois[i].getCaption(), whiteSpace * y);
            y++;
        }
    }

    /** Paint waypoint list and distances to each waypoint */
    protected void paint(Graphics g)
    {
        /** Clear background */
        g.setColor(Theme.getColor(Theme.TYPE_BACKGROUND));
        g.fillRect(0,0,getWidth(),getHeight());
        
        /** Draw title */
        g.setColor(Theme.getColor(Theme.TYPE_TITLE));
        g.setFont(titleFont);
        g.drawString(LocaleManager.getMessage("place_surveyor_canvas_title"),
                getWidth()/2, 1, Graphics.TOP|Graphics.HCENTER);

        drawPlaceMenu(g);
    }
}

