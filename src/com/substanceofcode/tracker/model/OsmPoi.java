/*
 * Marker.java
 *
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

package com.substanceofcode.tracker.model;

import com.substanceofcode.gps.GpsPosition;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.model.Place;
import com.substanceofcode.gps.GpsPosition;
import com.substanceofcode.tracker.view.Logger;
import com.substanceofcode.tracker.view.PlaceSurveyorCanvas;
import com.substanceofcode.tracker.grid.WGS84Position;
import com.substanceofcode.tracker.grid.GridPosition;
/**
 * @author Vikas Yadav
 */
public class OsmPoi extends Place {
    protected String caption;
    protected int inputType;
    protected String prefix;
    protected String suffix;
    protected boolean askRightLeft;
    protected int key;
    protected Controller controller;
    public static int pkctr;
    protected int id;

    public OsmPoi(int key,String caption,String prefix,int inputType,boolean askRL)
    {
        super("");
        id = pkctr++;
        this.key = key;
        this.caption = caption;
        this.prefix = prefix;
        this.inputType = inputType;
        this.askRightLeft = askRL;
        System.out.println(String.valueOf(key) + "," +
                            caption + "," +
                            prefix + "," +
                            String.valueOf(inputType) + "," +
                            (askRL == true ? "1" : "0")
                            );
    }

    public OsmPoi(Controller controller)
    {
        super("");
        id = pkctr++;
        GpsPosition pos = controller.getPosition();
        if(pos != null)
            this.setPosition(new WGS84Position(pos.latitude,pos.longitude));
        this.controller = controller;
        this.prefix = new String("");
    }

    /*public OsmPoi(String caption,byte inputType,String prefix,String suffix,boolean askRightLeft,byte key) {
        this.askRightLeft = askRightLeft;
        this.caption = caption;
        this.inputType = inputType;
        this.key = key;
        this.prefix = prefix;
        this.suffix = suffix;
    }*/

    public OsmPoiPage getRLPage()
    {
        try
        {
            OsmPoiPage page = new OsmPoiPage(this.controller);
            OsmPoi itemLeft = new OsmPoi(1,"Left",this.prefix + " Left",0,false);
            OsmPoi itemRight = new OsmPoi(3,"Right",this.prefix + " Right",0,false);
            page.addPoi(itemLeft);
            page.addPoi(itemRight);
            return page;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        
    }

    public OsmPoi process(GridPosition pos)
    {
        System.out.println("poi process");
        System.out.println(this.prefix + "=" + this.caption + "=" + this.key);
        this.setPosition(pos);
        if(this.inputType == 0) //just prefix
        {
            
            if(this.askRightLeft)
            {
                return getRLPage();
            }
            else
            {
                this.addPlace(this.prefix);
            }
        }
        else if(this.inputType == 10)
        {
            return this;
        }
        else if(this.inputType == 1 || this.inputType == 2)
        {
            
            return this;
        }
        return null;
    }

    public void addInput(String txt)
    {
        String txt2 = this.prefix + " " + txt;
        this.setName(txt2);
    }

    public void addPlace(String txt)
    {
        this.setName(txt);
        GpsPosition pos = controller.getPosition();
        if(pos != null)
            this.setPosition(new WGS84Position(pos.latitude,pos.longitude));

        if(txt == null)
            System.out.println("won't write NULL in places," + this.prefix + ", " + this.caption);
        else
        {
            PlaceSurveyorCanvas ps = this.controller.getPlaceSurveyor();
            if(ps == null)
                return;
            Place p = new Place(this.getName(),ps.getPosition());
            this.controller.addPlace(p);
        }
    }

    public void setKey(int key)
    {
        this.key = key;
    }

    public int getKey()
    {
        return this.key;
    }

    public void setCaption(String cap)
    {
        this.caption = cap;
    }

    public String getCaption()
    {
        return this.caption;
    }

    public int getInputType()
    {
        return this.inputType;
    }

    public void setInputType(int itype)
    {
        this.inputType = itype;
    }

    public void setAskRightLeft(boolean arl)
    {
        this.askRightLeft = arl;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix)
    {
        this.suffix = suffix;
    }

    public Controller getController()
    {
        return controller;
    }
}