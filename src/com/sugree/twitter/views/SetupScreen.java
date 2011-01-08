package com.sugree.twitter.views;

import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.rms.RecordStoreException;

import org.json.me.JSONException;

import com.substanceofcode.infrastructure.Device;
import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.twitter.Settings;

import com.substanceofcode.tracker.TweetNTrailMidlet;

//vikas
import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.model.RecorderSettings;
import com.substanceofcode.tracker.view.Logger;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class SetupScreen extends javax.microedition.lcdui.Form implements CommandListener, javax.microedition.lcdui.ItemStateListener {
	private final String[] gatewaysLabel = {
		"Custom",
		"twitter.com",
		"api.twitter.com",
		"Birdnest appspot",
		"Birdnest onedd",
	};
	private final String[] gatewaysValue = {
		null,
		"http://twitter.com/",
		"http://api.twitter.com/1/",
		"http://nest.appspot.com/text/",
		"http://nest.onedd.net/text/",
	};

	private final String[] pictureGatewaysLabel = {
		"Custom",
		"TwitPic",
		"TwitGoo",
		"yfrog",
		"upic.me",
		"Birdnest onedd TwitPic",
		"Birdnest onedd TwitGoo",
		"Birdnest onedd yfrog",
		"Birdnest onedd upic.me",
	};
	private final String[] pictureGatewaysValue = {
		null,
		"http://api.twitpic.com/2/upload.json",
		"http://twitgoo.com/api/upload",
		"http://yfrog.com/api/xauth_upload",
		"http://upic.me/api/upload",
		"http://nest.onedd.net/text/twitpic/2/upload.json",
		"http://nest.onedd.net/text/twitgoo/api/upload",
		"http://nest.onedd.net/text/yfrog/api/xauth_upload",
		"http://nest.onedd.net/text/upicme/api/upload",
	};

	private final String[] startsLabel = {
		"Empty",
		"Tweet",
		"Home",
		"Friends",
		"@Replies",
	};

	private final String[] flagsLabel = {
		"Force no Host",
		"Enable GZIP",
	};

//#ifdef polish.api.mmapi
//# 	private final String[] captureDevicesLabel = {
//# 		"Custom",
//# 		"capture://video",
//# 		"capture://image",
//# 		"capture://devcam0",
//# 		"capture://devcam1",
//# 	};
//# 	private final String[] captureDevicesValue = {
//# 		null,
//# 		"capture://video",
//# 		"capture://image",
//# 		"capture://devcam0",
//# 		"capture://devcam1",
//# 	};
//# 
//# 	private String[] snapshotEncodingsLabel;
//#endif

	private Controller controller;

	private javax.microedition.lcdui.TextField usernameField;
	private javax.microedition.lcdui.TextField passwordField;
	private javax.microedition.lcdui.TextField timelineLengthField;
	private javax.microedition.lcdui.TextField suffixTextField;
	private javax.microedition.lcdui.TextField refreshIntervalField;
	private javax.microedition.lcdui.TextField autoUpdateTextField;
	private javax.microedition.lcdui.TextField gatewayField;
	private javax.microedition.lcdui.ChoiceGroup gatewaysField;
//#ifdef polish.api.mmapi
//# 	private javax.microedition.lcdui.TextField pictureGatewayField;
//# 	private javax.microedition.lcdui.ChoiceGroup pictureGatewaysField;
//# 	private javax.microedition.lcdui.TextField captureDeviceField;
//# 	private javax.microedition.lcdui.ChoiceGroup captureDevicesField;
//# 	private javax.microedition.lcdui.TextField snapshotEncodingField;
//# 	private javax.microedition.lcdui.ChoiceGroup snapshotEncodingsField;
//#endif
	private javax.microedition.lcdui.TextField customWordsField;
	private javax.microedition.lcdui.TextField locationFormatField;
	private javax.microedition.lcdui.TextField cellIdFormatField;
	private javax.microedition.lcdui.ChoiceGroup startsField;
	private javax.microedition.lcdui.TextField timeOffsetField;
	private javax.microedition.lcdui.ChoiceGroup flagsField;
	private javax.microedition.lcdui.TextField hackField;
	private Command saveCommand;
	private Command cancelCommand;
	private Command togglePasswordCommand;
	private Command oauthRequestCommand;
	private Command oauthAccessCommand;
	private Command oauthDisableCommand;

	public SetupScreen(Controller controller) {
		super("Setup");
		this.controller = controller;

		RecorderSettings settings = controller.getSettings();

		String gateway = settings.getStringProperty(Settings.GATEWAY, "http://nest.onedd.net/text/");
		gatewayField = new javax.microedition.lcdui.TextField("Gateway", gateway, 128, TextField.URL);
		append(gatewayField);

		gatewaysField = new javax.microedition.lcdui.ChoiceGroup("Preset Gateways", Choice.EXCLUSIVE, gatewaysLabel, null);
		append(gatewaysField);
	

		boolean[] flags = {
			settings.settings.getBooleanProperty(Settings.FORCE_NO_HOST, false),
			settings.settings.getBooleanProperty(Settings.ENABLE_GZIP, true),
		};
		flagsField = new javax.microedition.lcdui.ChoiceGroup("Advanced Options", Choice.MULTIPLE, flagsLabel, null);
		flagsField.setSelectedFlags(flags);
		append(flagsField);

		String hack = settings.getStringProperty(Settings.HACK, "");
		hackField = new javax.microedition.lcdui.TextField("Hack", hack, 1024, TextField.ANY);
		append(hackField);

		saveCommand = new Command("Save", Command.OK, 1);
		addCommand(saveCommand);
		cancelCommand = new Command("Cancel", Command.CANCEL, 2);
		addCommand(cancelCommand);
		togglePasswordCommand = new Command("Toggle Password", Command.SCREEN, 3);
		addCommand(togglePasswordCommand);

		oauthRequestCommand = new Command("OAuth Request", Command.SCREEN, 4);
		addCommand(oauthRequestCommand);
		oauthAccessCommand = new Command("OAuth Access", Command.SCREEN, 5);
		addCommand(oauthAccessCommand);
		oauthDisableCommand = new Command("OAuth Disable", Command.SCREEN, 6);
		addCommand(oauthDisableCommand);

		setCommandListener(this);
		setItemStateListener(this);
	}

	public void itemStateChanged(javax.microedition.lcdui.Item item) {
		if (item == gatewaysField) {
			String url = gatewaysValue[gatewaysField.getSelectedIndex()];
			if (url != null) {
				gatewayField.setString(url);
			}
//#ifdef polish.api.mmapi
//# 		} else if (item == pictureGatewaysField) {
//# 			String url = pictureGatewaysValue[pictureGatewaysField.getSelectedIndex()];
//# 			if (url != null) {
//# 				pictureGatewayField.setString(url);
//# 			}
//# 		} else if (item == captureDevicesField) {
//# 			String device = captureDevicesValue[captureDevicesField.getSelectedIndex()];
//# 			if (device != null) {
//# 				captureDeviceField.setString(device);
//# 			}
//# 		} else if (item == snapshotEncodingsField) {
//# 			snapshotEncodingsLabel = Device.getSnapshotEncodings();
//# 			int index = snapshotEncodingsField.getSelectedIndex();
//# 			if (index > 0) {
//# 				snapshotEncodingField.setString(snapshotEncodingsLabel[index-1]);
//# 			}
//#endif
		}
	}

	public void commandAction(Command cmd, Displayable display) {
		if (cmd == saveCommand) {
			try {
				
				Logger.info("gatewayField");
				String gateway = gatewayField.getString();
//#ifdef polish.api.mmapi
//# 				Log.verbose("pictureGatewayField");
//# 				String pictureGateway = pictureGatewayField.getString();
//# 				Log.verbose("captureDeviceField");
//# 				String captureDevice = captureDeviceField.getString().trim();
//# 				Log.verbose("snapshotEncodingField");
//# 				String snapshotEncoding = snapshotEncodingField.getString().trim();
//#endif

				if (!gateway.endsWith("/")) {
					gateway += "/";
				}

				Logger.info("getSettings");
				RecorderSettings settings = controller.getSettings();

				//settings.settings.setStringProperty(Settings.USERNAME, username);
				//settings.settings.setStringProperty(Settings.PASSWORD, password);
				settings.settings.setStringProperty(Settings.GATEWAY, gateway);
				/*
                                 settings.settings.setIntProperty(Settings.TIMELINE_LENGTH, timelineLength);
				settings.settings.setStringProperty(Settings.SUFFIX_TEXT, suffixText);
				settings.settings.setIntProperty(Settings.REFRESH_INTERVAL, refreshInterval);
				settings.settings.setStringProperty(Settings.AUTO_UPDATE_TEXT, autoUpdateText);
				settings.settings.setStringProperty(Settings.CUSTOM_WORDS, customWords);
				settings.settings.setStringProperty(Settings.LOCATION_FORMAT, locationFormat);
				settings.setStringProperty(Settings.CELLID_FORMAT, cellIdFormat);
				settings.setStringProperty(Settings.TIME_OFFSET, timeOffset);
				settings.setIntProperty(Settings.START_SCREEN, startScreen);
				settings.setStringProperty(Settings.TIME_OFFSET, timeOffset);
				settings.setBooleanProperty(Settings.OPTIMIZE_BANDWIDTH, flags[0]);
				settings.setBooleanProperty(Settings.ALTERNATE_AUTHEN, flags[1]);
				settings.setBooleanProperty(Settings.SNAPSHOT_FULLSCREEN, flags[2]);
				settings.setBooleanProperty(Settings.STATUS_LENGTH_MAX, flags[3]);
				settings.setBooleanProperty(Settings.RESIZE_THUMBNAIL, flags[4]);
				settings.setBooleanProperty(Settings.WRAP_TIMELINE, flags[5]);
				settings.setBooleanProperty(Settings.ENABLE_SQUEEZE, flags[6]);
				settings.setBooleanProperty(Settings.ENABLE_GPS, flags[7]);
				settings.setBooleanProperty(Settings.ENABLE_REVERSE_GEOCODER, flags[8]);
				settings.setBooleanProperty(Settings.ENABLE_CELL_ID, flags[9]);
				settings.setBooleanProperty(Settings.ENABLE_REFRESH, flags[10]);
				settings.setBooleanProperty(Settings.ENABLE_REFRESH_ALERT, flags[11]);
				settings.setBooleanProperty(Settings.ENABLE_REFRESH_VIBRATE, flags[12]);
				settings.setBooleanProperty(Settings.ENABLE_REFRESH_COUNTER, flags[13]);
				settings.setBooleanProperty(Settings.SWAP_MINIMIZE_REFRESH, flags[14]);
				settings.setBooleanProperty(Settings.ENABLE_AUTO_UPDATE, flags[15]);
				settings.setBooleanProperty(Settings.ENABLE_AUTO_UPDATE_PICTURE, flags[16]);
				settings.setBooleanProperty(Settings.FORCE_NO_HOST, flags[17]);
				settings.setBooleanProperty(Settings.ENABLE_GZIP, flags[18]);
				settings.setStringProperty(Settings.HACK, hack);*/
//#ifdef polish.api.mmapi
//# 				settings.setStringProperty(Settings.PICTURE_GATEWAY, pictureGateway);
//# 				settings.setStringProperty(Settings.CAPTURE_DEVICE, captureDevice);
//# 				settings.setStringProperty(Settings.SNAPSHOT_ENCODING, snapshotEncoding);
//#endif

				controller.loadSettings();

				Logger.info("save");
				settings.settings.save(true);
			} catch (IOException e) {
				Logger.error(e.toString());
				controller.showError(e.toString());
			} catch (RecordStoreException e) {
				Logger.error(e.toString());
				controller.showError(e.toString());
			} catch (Exception e) {
				Logger.error(e.toString());
				controller.showError(e.toString());
			}
			
		} else if (cmd == togglePasswordCommand) {
			passwordField.setConstraints(passwordField.getConstraints()^TextField.PASSWORD);
		} else if (cmd == oauthRequestCommand) {
			controller.startOAuthRequestToken();
		} else if (cmd == oauthAccessCommand) {
			controller.showOAuth(null);
		} else if (cmd == oauthDisableCommand) {
			//controller.resetOAuth();
		}
                else if (cmd == cancelCommand) {
			controller.setCurrentScreen(controller.getTrailCanvas());
		}
	}
}
