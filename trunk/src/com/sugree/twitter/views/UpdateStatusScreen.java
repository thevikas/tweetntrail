package com.sugree.twitter.views;

import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import com.substanceofcode.tracker.controller.Controller;
import com.substanceofcode.tracker.TweetNTrailMidlet;
import com.substanceofcode.tracker.model.RecorderSettings;
import com.sugree.utils.Location;
import com.substanceofcode.twitter.Settings;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class UpdateStatusScreen extends javax.microedition.lcdui.TextBox implements CommandListener {
	private Controller controller;

	private Command sendCommand;
	private Command cancelCommand;
	private Command insertCommand;
	private Command statCommand;
	private Command squeezeCommand;
	
	public UpdateStatusScreen(Controller controller, String text) {
		super("What are you doing?", text, controller.getStatusMaxLength(), TextField.ANY);
		this.controller = controller;

		RecorderSettings settings = controller.getSettings();

		sendCommand = new Command("Send", Command.OK, 1);
		addCommand(sendCommand);
		cancelCommand = new Command("Cancel", Command.CANCEL, 2);
		addCommand(cancelCommand);
		insertCommand = new Command("Insert@#", Command.SCREEN, 3);
		addCommand(insertCommand);

                if (settings.settings.getBooleanProperty(Settings.ENABLE_SQUEEZE, true)) {
			squeezeCommand = new Command("Squeeze", Command.SCREEN, 10);
			addCommand(squeezeCommand);
		}

		setCommandListener(this);
	}

	public void insert(String text) {
		insert(text, getCaretPosition());
	}

	public void commandAction(Command cmd, Displayable display) {
		if (cmd == sendCommand) {
			controller.updateStatus(getString());
		}
                if (cmd == cancelCommand) {
			controller.setCurrentScreen(controller.getTrailCanvas());
		}
	}
}
