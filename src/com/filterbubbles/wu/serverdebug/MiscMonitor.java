package com.filterbubbles.wu.serverdebug;

import java.util.logging.Logger;

import com.wurmonline.server.creatures.Communicator;

public class MiscMonitor
{
	private static Logger logger = Logger.getLogger(MiscMonitor.class.getName());
	private static long[] timestamps = new long[]{0,0,0,0,0};


	static public void log(String msg)
	{
		logger.info(Thread.currentThread().getName() + "\t" + msg);
	}

	static public void log(String msg, int timestampIndex)
	{
		if((timestamps[timestampIndex] + 30000) < System.currentTimeMillis()) {
			logger.info(Thread.currentThread().getName() + "\t" + msg);
			timestamps[timestampIndex] = System.currentTimeMillis();
		}
	}

	static public void logMisc(Communicator com, String msg)
	{
		String pName;
		if(com != null && com.getPlayer() != null) {
			pName = com.getPlayer().getName() + "," + com.getPlayer().hasLink();
		} else {
			// Never mind this...
			//pName = "null";
			return;
		}
		logger.info(Thread.currentThread().getName() + "\t" + pName + "\t" + msg);
	}
}
