package com.sugree.twitter.tasks;

import java.util.Vector;

import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.twitter.TwitterApi;

import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.TweetNTrailMidlet;

import com.sugree.twitter.TwitterConsumer;
import com.substanceofcode.gps.GpsPosition;

import com.sugree.twitter.TwitterException;
import net.oauth.j2me.OAuthServiceProviderException;

public class UpdateStatusTask extends AbstractTask {
	public Controller controller;
	private TwitterConsumer oauth;
        private GpsPosition position;
	private String status;
	private long replyTo;
	private byte[] snapshot;
	
	private String mimeType;
	private boolean nonBlock;

	public UpdateStatusTask(Controller controller, TwitterConsumer oauth_in, String status, GpsPosition pos) {
		this.controller = controller;
		this.oauth = oauth_in;
		this.status = status;
                this.position = pos;
	}

	public void doTask() {
		try {

                    oauth.doUpdate(this, status, position);
                    controller.setCurrentScreen(controller.getTrailCanvas());
                    controller.resetWaitScreen();
		} 
                catch (OAuthServiceProviderException e) {
			controller.showError(e.toString());
		}
	}
}
