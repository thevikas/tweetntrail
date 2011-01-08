package com.sugree.twitter.tasks;

import java.util.Vector;

import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.TweetNTrailMidlet;
import com.sugree.twitter.TwitterException;
import com.sugree.twitter.TwitterConsumer;

import com.substanceofcode.tracker.controller.Controller;

public class OAuthTask extends AbstractTask {
	private Controller controller;
	private TwitterConsumer oauth;
	private int objectType;
	private String pin;

	public final static int REQUEST_TOKEN = 0;
	public final static int ACCESS_TOKEN = 1;

	public OAuthTask(Controller controller, TwitterConsumer oauth, int objectType, String pin) {
		this.controller = controller;
		this.oauth = oauth;
		this.objectType = objectType;
		this.pin = pin;
	}

	public void doTask() {
		String url = null;

		try {
			switch (objectType) {
				case REQUEST_TOKEN:
					url = controller.oauthRequestToken();
					controller.showOAuth(url);
					break;
				case ACCESS_TOKEN:
					controller.oauthAccessToken(pin);
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			controller.showError(e.toString());
		}
	}
}
