package com.sugree.twitter.views;

import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import com.substanceofcode.tasks.AbstractTask;
import com.substanceofcode.util.Log;
import com.substanceofcode.tracker.controller.Controller;
import com.sugree.utils.Loggable;

// use fully qualified classname, make sure it use native GUI, and not Polish GUI
public class WaitScreen extends javax.microedition.lcdui.Form implements CommandListener, Runnable, Loggable {
	private Controller controller;
	private AbstractTask task;
	private int cancelScreen;

	private javax.microedition.lcdui.StringItem stateField;
	private javax.microedition.lcdui.Gauge progressField;
	private javax.microedition.lcdui.StringItem logField;
	private Command cancelCommand;

	private Thread thread;

	public WaitScreen(Controller controller, AbstractTask task, int cancelScreen) {
		super("Wait");
		this.controller = controller;
		this.task = task;
		this.cancelScreen = cancelScreen;

		thread = new Thread(this);

		stateField = new javax.microedition.lcdui.StringItem("", "waiting");
		append(stateField);
		progressField = new javax.microedition.lcdui.Gauge("Progress", false, 100, 0);
		append(progressField);
		logField = new javax.microedition.lcdui.StringItem("", "");
		append(logField);

		cancelCommand = new Command("Cancel", Command.STOP, 1);
		addCommand(cancelCommand);

		setCommandListener(this);
	}

	public void setState(String text) {
		stateField.setText(text);
	}

	public void setProgress(int value) {
		progressField.setValue(value);
	}

	public void clear() {
		setText("");
	}

	public void print(String text) {
		setText(logField.getText()+text);
	}

	public void println(String text) {
		setText(logField.getText()+text+"\n");
	}

	public void setText(String text) {
		logField.setText(text);
	}

	public void commandAction(Command cmd, Displayable display) {
		if (cmd == cancelCommand) {
                        System.out.println("cancel wait");
			controller.setCurrentScreen(controller.getTrailCanvas());
		}
	}

	public void start() {
		thread.start();
	}

	public void run() {
		Log.setConsole(this);
		try {
			task.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.setConsole(null);
	}
}
