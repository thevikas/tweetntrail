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

import javax.microedition.lcdui.Canvas;
import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.model.OsmPoi;
import com.substanceofcode.tracker.grid.GridPosition;
/**
 * @author Vikas Yadav
 */
public class OsmPoiPage extends OsmPoi {
    
    protected OsmPoi keys[];

    public OsmPoiPage(int key,String caption,String prefix)
    {
        this();
        this.setCaption(caption);
        this.setKey(key);
        this.prefix = prefix;
    }

    public OsmPoiPage(Controller controller)
    {
        super(controller);
        keys = new OsmPoi[10];
        this.inputType = 10;
        this.askRightLeft = false;
        this.suffix = "";
        this.prefix = "";

        /*for(int i=0; i<10; i++)
        {
            keys[i] = new OsmPoi(this.controller);
        }*/
        
    }

    public void setController(Controller controller)
    {
        this.controller = controller;
    }

    public OsmPoiPage()
    {
        super(0,"","",0,false);
        keys = new OsmPoi[10];
        this.inputType = 10;

        /*for(int i=0; i<10; i++)
        {
            keys[i] = new OsmPoi(this.controller);
        }*/

    }


    public OsmPoi[] getAllPois()
    {
        System.out.println(String.valueOf(this.keys.length) + " keys found!");
        return this.keys;
    }

    public void showAsCurrent()
    {
        //this.controller.setCurrentScreen(new Place);
    }

    public void addPoi(OsmPoi poi) throws Exception
    {
        System.out.println("adding poi using addPoi");
        if(this.controller == null)
            throw new Exception("cannot add child without controller");
        poi.controller = this.controller;
        this.keys[poi.getKey()] = poi;
    }

    public void addPoi(OsmPoiPage poi)
    {
        System.out.println("adding page using addPoi");
        poi.controller = this.controller;
        this.keys[poi.getKey()] = poi;
    }

    public OsmPoi handleKeyPressed(int keyCode,GridPosition pos)
    {
        OsmPoi nextPage = null;
        OsmPoi pageItem = null;
        OsmPoiPage pageItemPage = null;
        
        if (keyCode == Canvas.KEY_NUM0)
        {
            pageItem = keys[0];
        }
        else if (keyCode == Canvas.KEY_NUM1)
        {
            pageItem = keys[1];
        }
        else if (keyCode == Canvas.KEY_NUM2)
        {
            pageItem = keys[2];
        }
        else if (keyCode == Canvas.KEY_NUM3)
        {
            pageItem = keys[3];
        }
        else if (keyCode == Canvas.KEY_NUM4)
        {
            pageItem = keys[4];
        }
        else if (keyCode == Canvas.KEY_NUM5)
        {
            pageItem = keys[5];
        }
        else if (keyCode == Canvas.KEY_NUM6)
        {
            pageItem = keys[6];
        }
        else if (keyCode==Canvas.KEY_NUM7)
        {
            pageItem = keys[7];
        }
        else if (keyCode == Canvas.KEY_NUM8)
        {
            pageItem = keys[8];
        }
        else if (keyCode == Canvas.KEY_NUM9)
        {
            pageItem = keys[9];
        }
        else if(keyCode == Canvas.KEY_STAR)
        { //abort!
            return null;
        }

        System.out.println("TJ handle 2");
        nextPage = pageItem.process(pos);


        return nextPage;
    }
}