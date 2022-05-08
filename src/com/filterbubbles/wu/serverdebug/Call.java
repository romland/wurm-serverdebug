package com.filterbubbles.wu.serverdebug;

import java.util.logging.Logger;

public class Call
{
	private static Logger logger = Logger.getLogger(ModMonitor.class.getName());

	public String className = null;
	public String methodName = null;
	public int threshold = 0;

	private long startTime = 0;
	private long endTime = 0;
	
	// Statistics
	private long numCalls = 0;
	private long totCost = 0;
	
	// Keep a reference to an arbitrary object passed in from a monitored location (typicall to identify a troublesome object)
	private String callArgString = null;

	// We want to make sure we do not interact with any game methods at all here; which is why we store a string of the object when we get it.
	private String callArgCurrentString = null;
	
	Call(String className, String methodName)
	{
		this(className, methodName, Mod.defaultLongCallThreshold);
	}

	Call(String className, String methodName, int threshold)
	{
		this.className = className;
		this.methodName = methodName;
		this.threshold = threshold;
	}
	
	void beginCall()
	{
		this.beginCall(null);
	}

	void beginCall(Object obj)
	{
		this.endTime = -1;
		this.startTime = System.nanoTime();
		this.callArgCurrentString = ServerMonitor.getServerObjectString(obj);
	}

	void finalizeCall()
	{
		finalizeCall("not set", null);
	}
	
	void finalizeCall(String sourceMethod)
	{
		finalizeCall(sourceMethod, null);
	}

	void finalizeCall(String sourceMethod, Object obj)
	{
		endTime = System.nanoTime();
		this.callArgCurrentString = ServerMonitor.getServerObjectString(obj);
		
		if(isOverThreshold()) {
			logger.warning("Hey Friya! [completed]: in [" + sourceMethod + "()] " + toString());
			if(Mod.completionStackTrace) {
				MethodMonitor.outputStackTrace(Thread.currentThread().getStackTrace(), 5, this);
			}
		}
	}

	boolean isRunning()
	{
		return endTime == -1;
	}

	long getDuration()
	{
		return getPreciseDuration() / 1000000;
	}

	private long getPreciseDuration()
	{
		if(isRunning()) {
			return (System.nanoTime() - startTime);
		} else {
			return (endTime - startTime);
		}
	}

	private long getPreciseAverageCost()
	{
		if(numCalls == 0 || totCost == 0) {
			return 0;
		}
		
		return totCost / numCalls;
	}

	private float getAverageCost()
	{
		return (float)getPreciseAverageCost() / 1000000f;
	}

	void reset()
	{
		numCalls++;
		totCost += getPreciseDuration();
		
		startTime = 0;
		endTime = 0;
		this.callArgCurrentString = null;
	}

	boolean isOverThreshold()
	{
		return getDuration() >= threshold;
	}
	
	public String toString()
	{
		return "Call to " 
			+ className + "." + methodName + "() "
			+ "started: " + startTime + ", "
			+ "duration: " + getDuration() + "ms "
			+ "(" + numCalls + " x " + getPreciseAverageCost() + "ns = " + getAverageCost() + "ms). "
			+ "Running: " + isRunning()
			+ (callArgCurrentString != null ? ". Arg: "+ callArgCurrentString : "")
		;
	}
	
	void setCallArg(Object obj)
	{
		this.callArgCurrentString = ServerMonitor.getServerObjectString(obj);
	}

	String getCallArgString()
	{
		return callArgString;
	}

	void setCallArgString(String callArgs)
	{
		this.callArgString = callArgs;
	}

}
