package com.filterbubbles.wu.serverdebug;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class MethodMonitor
{
	private static Logger logger = Logger.getLogger(MethodMonitor.class.getName());
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

	// Which class are we hooking into to monitor.
	private String className;
	
	// Which method in that class.
	private String methodName;

	// Which calls FROM that method are we monitoring.
	private Map<String, Call> monitoredCalls = new HashMap<String, Call>();
	
	// Keep track of which calls we have done since we entered this method.
	private List<String> lastFramesCalls;

	// What is determined a "slow" call for the calls from this method.
	private int longCallThreshold;

	StackTraceElement[] currentStackTrace = null;	

	
	MethodMonitor(String classAndMethod, int longCallThreshold) 
	{
		String[] cm = ServerMonitor.getClassAndMethodOf(classAndMethod);
		this.className = cm[0];
		this.methodName = cm[1];
		this.longCallThreshold = longCallThreshold;
		
		if(Mod.maxStoredFrameCalls > 0) {
			lastFramesCalls = new CopyOnWriteArrayList<String>();
		}
	}

	MethodMonitor(String className, String methodName, int longCallThreshold) 
	{
		this.className = className;
		this.methodName = methodName;
		this.longCallThreshold = longCallThreshold;

		if(Mod.maxStoredFrameCalls > 0) {
			lastFramesCalls = new CopyOnWriteArrayList<String>();
		}
	}

	boolean belongsTo(String classAndMethod)
	{
		return classAndMethod.equals(getClassAndMethod());
	}
	
	boolean hasCall(String classAndMethod)
	{
		return monitoredCalls.containsKey(classAndMethod);
	}
	
	Map<String, Call> getMonitoredCalls()
	{
		return monitoredCalls;
	}
	
	void addCall(String classAndMethod)
	{
		String[] cm = ServerMonitor.getClassAndMethodOf(classAndMethod);
		getCall(cm[0], cm[1]);
	}

	void addCall(String classAndMethod, String extraCallArgs)
	{
		String[] cm = ServerMonitor.getClassAndMethodOf(classAndMethod);
		Call c = getCall(cm[0], cm[1]);
		c.setCallArgString(extraCallArgs);
	}
	
	Call getCall(String className, String hookName)
	{
		String ch = className + "." + hookName;
		if(monitoredCalls.containsKey(ch) == false) {
			monitoredCalls.put(
				ch,
				new Call(className, hookName, longCallThreshold)
			);
		}

		return monitoredCalls.get(ch);
	}
	
	String getCallArgs(String className, String hookName)
	{
		return getCall(className, hookName).getCallArgString();
	}

	void periodicSlowCallMonitoring(int threshold)
	{
		Call c;
		boolean foundSlowCall = false;
		
		for (Map.Entry<String, Call> e : monitoredCalls.entrySet()) {
			c = e.getValue();
			if(c.isRunning() && c.getDuration() > threshold) {
				logger.severe("Hey Friya! [running]: in [" + getClassAndMethod() + "()] " + c.toString());
				outputAnyLastFrameCalls();
				foundSlowCall = true;
			}
		}
		
		if(foundSlowCall) {
			if(Mod.runningStackTrace && currentStackTrace != null) {
				outputStackTrace(currentStackTrace, 3, null);
				
				// Make sure we only output it once (since this is called from tick()).
				currentStackTrace = null;
			}

			if(Mod.dumpThreadInfoOnSlowRunningCall && mayDumpPeriodicThreadInfo()) {
				dumpAllThreads();
				Mod.lastPeriodicThreadInfoDump = System.currentTimeMillis();
			} else {
				logger.info("May not dump thread info!");
			}
		}
	}
	
	private boolean mayDumpPeriodicThreadInfo()
	{
		return System.currentTimeMillis() > (Mod.lastPeriodicThreadInfoDump + 1000);
	}

	private void outputAnyLastFrameCalls()
	{
		if(lastFramesCalls != null && lastFramesCalls.size() > 0) {
			StringBuffer sb = new StringBuffer();
			for(String s : lastFramesCalls) {
				sb.append("\t");
				sb.append(s);
				sb.append("\n");
			}

			logger.severe("Monitored calls from [" + getClassAndMethod() + "] since we entered (max: " + Mod.maxStoredFrameCalls + "), NOTE: 'running' status may not be accurate at the time of output. This is NOT a stack trace, it's a history of monitored calls from a method\n" + sb.toString());
			// Make sure we don't output this more than once.
			lastFramesCalls.clear();
		}
	}
	
	void resetCalls()
	{
		if(Mod.runningStackTrace) {
			currentStackTrace = Thread.currentThread().getStackTrace();
		}

		for (Map.Entry<String, Call> e : monitoredCalls.entrySet()) {
			if(e.getValue().isRunning()) {
				logger.info("Resetting active call in [" + getClassAndMethod() + "] (can happen due to exceptions or recursiveness): " + e.getValue().toString());
				outputAnyLastFrameCalls();
				e.getValue().reset();
			}
		}

		if(lastFramesCalls != null) {
			lastFramesCalls.clear();
		}
	}

	void beginCall(String className, String methodName, Object argObject)
	{
		Call c = getCall(className, methodName);
		c.setCallArg(argObject);

		if(Mod.maxStoredFrameCalls > 0) {
			if(lastFramesCalls.size() > Mod.maxStoredFrameCalls) {
				lastFramesCalls.remove(0);
			}
			
			lastFramesCalls.add(c.toString());
		}

		c.beginCall();
	}
	
	void finalizeCall(String className, String methodName)
	{
		finalizeCall(className, methodName, null);
		/*
		Call c = getCall(className, methodName);
		c.finalizeCall(getClassAndMethod());
		c.reset();
		*/
	}
	
	void finalizeCall(String className, String methodName, Object sourceObject)
	{
		Call c = getCall(className, methodName);
		c.finalizeCall(getClassAndMethod(), sourceObject);
		c.reset();
	}
	
	String getClassName()
	{
		return className;
	}
	
	String getMethodName()
	{
		return methodName;
	}
	
	String getClassAndMethod()
	{
		return className + "." + methodName;
	}
	
	int getLongCallThreshold()
	{
		return longCallThreshold;
	}

	static void outputStackTrace(StackTraceElement[] stes, int ignoreStepsCount, Call sourceCall)
	{
		// xxx output it somewhere
		// remove last entries of stack trace (to hide MY presence)...
		// "The stack trace going to <class/method> was:
		
		StringBuffer sb = new StringBuffer();
		
		for(int i = 0; i < stes.length; i++) {
			if(i < ignoreStepsCount) {
				continue;
			}
			
			if(i == ignoreStepsCount) {
				sb.append("\t|\t");
				if(sourceCall != null) {
					sb.append(sourceCall.className);
					sb.append(".");
					sb.append(sourceCall.methodName);
					sb.append(" (this is the monitored call)");
				} else {
					sb.append("<unknown method> This is a stored stack trace. To find out which call is slow, see monitor warning above.");
				}
				sb.append("\n");
			}

			sb.append("\t|\t" + stes[i].toString() + "\n");
		}
		
		logger.info("Heads up: This is NOT an exception or an error, it's a stack trace of a monitored call (you can turn these off):\n" + sb.toString());
	}

	private void dumpAllThreads()
	{
        String ts = dtf.format(LocalDateTime.now());
        
		StringBuilder dump = new StringBuilder();
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
		for(ThreadInfo threadInfo : threadInfos) {
			dump.append(ts);
			dump.append(" ");
			dump.append('"');
			dump.append(threadInfo.getThreadName());
			dump.append("\" ");
			Thread.State state = threadInfo.getThreadState();
			dump.append("\n	java.lang.Thread.State: ");
			dump.append(state);
			StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
			for(StackTraceElement stackTraceElement : stackTraceElements) {
				dump.append("\n		at ");
				dump.append(stackTraceElement);
			}
			dump.append("\n\n");
		}
		
		logger.info("Hey Friya! Thread dump:\n" + dump.toString());
	}

}
