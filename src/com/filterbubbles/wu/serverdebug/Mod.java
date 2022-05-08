package com.filterbubbles.wu.serverdebug;

import java.io.File;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerLoginListener;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.ServerPollListener;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.players.Player;

import javassist.CtClass;

public class Mod implements WurmServerMod, Initable, Configurable, ServerStartedListener, PlayerMessageListener, PlayerLoginListener, ServerPollListener
{
	private static Logger logger = Logger.getLogger(Mod.class.getName());
	private static Timer timer;
	private static long pollCounter = 2;
	private static long tickCounter = 2;

	public static boolean enabled = true;
	public static boolean logCmdIn = false;
	public static boolean logCmdOut = false;
	public static boolean logNewAction = true;
	public static boolean monitorDefaultModHooks = true;
	public static boolean monitorGameLoop = true;
	public static boolean dumpModifiedClasses = true;
	public static int defaultLongCallThreshold = 1000;
	private static int monitorTickFrequency = 5000;
	private static int monitorTickWarningThreshold = 3000;
	public static int maxStoredFrameCalls = 100;
	public static boolean attachThisToMonitors = false;
	public static boolean completionStackTrace = false;
	public static boolean runningStackTrace = false;
	private static boolean monitorHealthNotice = true;
	private static boolean serverPollHealthNotice = true;
	public static boolean loginHandlerHealthNotice = true;
	public static boolean socketServerHealthNotice = true;
	public static boolean dumpThreadInfoOnSlowRunningCall = true;

	// Non-configurable; automatically detected.
	public static boolean friyasDevelopmentServer = false; 

	// Non-configurable; but global.
	public static long lastPeriodicThreadInfoDump = 0;

	// Because common sense is not common.
	public static String s = "How I *wanted* to end the announcement post.... "
			+ "PS. And to Sindusk: If I wanted to support your forks of my code, I'd let you know. "
			+ "If you are going to fork or borrow and put it in your own repository, at least make "
			+ "some substantial (and valuable) changes and then rename it (well, do what is right, "
			+ "basically). Other than that, I suggest you make suggestions of what can be added. "
			+ "Thank you.";

	
	@Override
	public void configure(Properties props)
	{
		Mod.enabled = Boolean.valueOf(props.getProperty("enabled", String.valueOf(Mod.enabled))).booleanValue();

		if(!enabled) {
			return;
		}

		Mod.logCmdIn = Boolean.valueOf(props.getProperty("logCmdIn", String.valueOf(Mod.logCmdIn))).booleanValue();
		Mod.logCmdOut = Boolean.valueOf(props.getProperty("logCmdOut", String.valueOf(Mod.logCmdOut))).booleanValue();
		Mod.logNewAction = Boolean.valueOf(props.getProperty("logNewAction", String.valueOf(Mod.logNewAction))).booleanValue();
		Mod.monitorDefaultModHooks = Boolean.valueOf(props.getProperty("monitorDefaultModHooks", String.valueOf(Mod.monitorDefaultModHooks))).booleanValue();
		Mod.monitorGameLoop = Boolean.valueOf(props.getProperty("monitorGameLoop", String.valueOf(Mod.monitorGameLoop))).booleanValue();
		Mod.dumpModifiedClasses = Boolean.valueOf(props.getProperty("dumpModifiedClasses", String.valueOf(Mod.dumpModifiedClasses))).booleanValue();
		Mod.defaultLongCallThreshold = Integer.valueOf(props.getProperty("defaultLongCallThreshold", String.valueOf(Mod.defaultLongCallThreshold))).intValue();
		Mod.monitorTickFrequency = Integer.valueOf(props.getProperty("monitorTickFrequency", String.valueOf(Mod.monitorTickFrequency))).intValue();
		Mod.monitorTickWarningThreshold = Integer.valueOf(props.getProperty("monitorTickWarningThreshold", String.valueOf(Mod.monitorTickWarningThreshold))).intValue();
		Mod.maxStoredFrameCalls = Integer.valueOf(props.getProperty("maxStoredFrameCalls", String.valueOf(Mod.maxStoredFrameCalls))).intValue();
		Mod.attachThisToMonitors = Boolean.valueOf(props.getProperty("attachThisToMonitors", String.valueOf(Mod.attachThisToMonitors))).booleanValue();
		Mod.completionStackTrace = Boolean.valueOf(props.getProperty("completionStackTrace", String.valueOf(Mod.completionStackTrace))).booleanValue();
		Mod.runningStackTrace = Boolean.valueOf(props.getProperty("runningStackTrace", String.valueOf(Mod.runningStackTrace))).booleanValue();
		Mod.monitorHealthNotice = Boolean.valueOf(props.getProperty("monitorHealthNotice", String.valueOf(Mod.monitorHealthNotice))).booleanValue();
		Mod.serverPollHealthNotice = Boolean.valueOf(props.getProperty("serverPollHealthNotice", String.valueOf(Mod.serverPollHealthNotice))).booleanValue();
		Mod.loginHandlerHealthNotice = Boolean.valueOf(props.getProperty("loginHandlerHealthNotice", String.valueOf(Mod.loginHandlerHealthNotice))).booleanValue();
		Mod.socketServerHealthNotice = Boolean.valueOf(props.getProperty("socketServerHealthNotice", String.valueOf(Mod.socketServerHealthNotice))).booleanValue();
		Mod.dumpThreadInfoOnSlowRunningCall = Boolean.valueOf(props.getProperty("dumpThreadInfoOnSlowRunningCall", String.valueOf(Mod.dumpThreadInfoOnSlowRunningCall))).booleanValue();

		// Output configuration to make the server log alone good enough to help assist in debugging.
		StringBuilder sb = new StringBuilder("Friya's Server Debug configuration:\n");
		for(String propKey : props.stringPropertyNames()) {
			sb.append("\t\t");
			sb.append(propKey);
			sb.append("\t=\t");
			sb.append(props.getProperty(propKey));
			sb.append("\n");
		}
		logger.info(sb.toString());

		String listCallsInMethodThenExit = String.valueOf(props.getProperty("listCallsInMethodThenExit", ""));
		if(listCallsInMethodThenExit != null && listCallsInMethodThenExit.length() > 0) {
			listCallsInMethodThenExit = listCallsInMethodThenExit.replace("/", ".");
			Patcher.getInstance().logCallsInMethod(listCallsInMethodThenExit);
			throw new RuntimeException("Throwing this error because \"listCallsInMethodThenExit\" is set in the properties. Comment it out if you want your server to start. :)");
		}
		
		if((new File(System.getProperty("user.dir") + File.separator + "__Friya's Development Server__")).exists()) {
			logger.info("######################### FRIYA'S DEVELOPMENT SERVER #########################");
			friyasDevelopmentServer = true;
		}

		if(dumpModifiedClasses) {
			CtClass.debugDump = System.getProperty("user.dir") + File.separator + "mods" + File.separator + "serverdebug" + File.separator + "dump";
		}

		ServerMonitor.createMethodMonitors(props);
//		fakeError();
		Patcher.getInstance().configure();
	}

	@Override
	public void init() 
	{
		if(!enabled) {
			return;
		}

		Patcher.getInstance().init();
//		Mod.fakeError();
	}

	@Override
	public void onServerStarted() 
	{
		if(!enabled) {
			return;
		}

		if(timer == null) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					tick();
				}
			}, 0, monitorTickFrequency);
		}

	}

	@Override
	public void onPlayerLogin(Player arg0) 
	{
		if(!enabled) {
			return;
		}

		//fakeLag();
	}

	@Override
	public boolean onPlayerMessage(Communicator arg0, String arg1) 
	{
		if(!enabled) {
			return false;
		}

		return false;
	}

	@Override
	public void onServerPoll() 
	{
		if(!enabled) {
			return;
		}

		if((pollCounter++ % 1000) == 1) {
			if(Mod.serverPollHealthNotice) {
				MiscMonitor.log(Mod.class.getName() + ".onServerPoll()");
			}
//			fakeLag();
		}
	}
	
	static public void fakeError()
	{
		if(!friyasDevelopmentServer) {
			return;
		}

		logger.info("Forcing div by 0 to make things go boom!");
		int x = 10 / 0; logger.info(""+x);
	}
	
	static public void fakeLag()
	{
		if(!friyasDevelopmentServer) {
			return;
		}

		logger.info("Inducing a bit of fake lag...");
		long lag = System.currentTimeMillis() + 20000;
		int counter = 0;
		while(lag > System.currentTimeMillis()) {
			// Let's just do something to see if something else is bogging down the machine...
	        final Creature[] creatures = Creatures.getInstance().getCreatures();
	        for (final Creature c : creatures) {
	        	c.getName();
	        }
        	counter++;
		}
		logger.info("Fake lag ended. Had time for: " + counter + " creature iterations in that period");
	}

	private void tick()
	{
		if(Mod.monitorHealthNotice &&  (tickCounter++ % 10) == 1) {
			MiscMonitor.log("Monitoring thread is still OK!");
		}

		ModMonitor.periodicSlowCallMonitoring(monitorTickWarningThreshold);
		ServerMonitor.periodicSlowCallMonitoring(monitorTickWarningThreshold);
	}
}
