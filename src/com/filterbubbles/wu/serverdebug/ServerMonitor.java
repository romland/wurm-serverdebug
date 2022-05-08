package com.filterbubbles.wu.serverdebug;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import com.wurmonline.server.zones.Zone;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class ServerMonitor implements Monitor
{
	private static Logger logger = Logger.getLogger(ServerMonitor.class.getName());
	private static Map<Integer, MethodMonitor> methodMonitors = new ConcurrentHashMap<Integer, MethodMonitor>();


	static MethodMonitor getMethodMonitor(String classAndMethod)
	{
		for (Map.Entry<Integer, MethodMonitor> e : methodMonitors.entrySet()) {
			MethodMonitor c = e.getValue();
			if(c.belongsTo(classAndMethod)) {
				return c;
			}
		}

		return null;
	}


	static void createMethodMonitors(Properties props)
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
		String targetClassAndMethod;
		
		Map<String, Integer> thresholds = new HashMap<String, Integer>();
		
		for(String propKey : props.stringPropertyNames()) {
			if(propKey.startsWith("monitorThreshold.") == false) {
				continue;
			}
			
			thresholds.put(
				propKey.substring(17),
				Integer.valueOf(props.getProperty(propKey, String.valueOf(Mod.defaultLongCallThreshold))).intValue()
			);
		}

		for(String propKey : props.stringPropertyNames()) {
			if(propKey.startsWith("monitor.")) {
				targetClassAndMethod = propKey.substring(8);
				
				Map<Integer, MethodMonitor> mms = ServerMonitor.getMethodMonitors();

				MethodMonitor mm;
				if(ServerMonitor.hasMethodMonitor(targetClassAndMethod)) {
					mm = ServerMonitor.getMethodMonitor(targetClassAndMethod);
				} else {
					if(thresholds.containsKey(targetClassAndMethod)) {
						mm = new MethodMonitor(targetClassAndMethod, thresholds.get(targetClassAndMethod));
					} else {
						mm = new MethodMonitor(targetClassAndMethod, Mod.defaultLongCallThreshold);
					}

					logger.info("Using long-call threshold of " + mm.getLongCallThreshold() + "ms...");
					mms.put(mm.hashCode(), mm);
				}
				
				String[] calls = props.getProperty(propKey).split(",");
				for(String call : calls) {
					call = call.trim();
					
					if(call.length() == 0) {
						continue;
					}
					
					String[] callArgs = call.split("\\(");
					String args = null;
					if(callArgs.length > 1) {
						call = callArgs[0];
						args = callArgs[1].replace(")", "");
					}

					if(mm.hasCall(call)) {
						logger.warning("Duplicate. Skipping " + call);
						continue;
					}
					
					String[] cm = ServerMonitor.getClassAndMethodOf(call);
					CtClass c = null;
					try {
						c = cp.get(cm[0]);
					} catch (NotFoundException e) {
						// I want this to halt server so you don't end up monitoring something that does not exist (waiting in vain sucks).
						throw new RuntimeException("attempting to monitor a call to a class that does not exist: " + cm[0], e);
					}
					
					try {
						c.getDeclaredMethod(cm[1]);
					} catch (NotFoundException e) {
						logger.warning("You MIGHT be attempting to monitor a call to a method that does not exist: " + call + "()");
					}
					
					mm.addCall(call, args);
					logger.info("Understood option to monitor calls to [" + call + "()] from " + mm.getClassAndMethod() + "()");
				}

			}
		}
	}
	
	static String[] getClassAndMethodOf(String classAndMethod)
	{
		String[] tokens = classAndMethod.split("\\.");
		String c = String.join(".", Arrays.copyOfRange(tokens, 0, tokens.length - 1));
		String m = tokens[tokens.length - 1];
		return new String[]{ c, m };
	}
	
	static MethodMonitor getMethodMonitor(int hash)
	{
		return methodMonitors.get(hash);
	}
	
	static boolean hasMethodMonitor(String classAndMethod)
	{
		return getMethodMonitor(classAndMethod) != null;
	}
	
	static Map<Integer, MethodMonitor> getMethodMonitors()
	{
		return methodMonitors;
	}

	static void periodicSlowCallMonitoring(int threshold)
	{
		for (Map.Entry<Integer, MethodMonitor> e : methodMonitors.entrySet()) {
			e.getValue().periodicSlowCallMonitoring(threshold);
		}
	}
	
	// NOTE: Called by game
	public static void resetCalls(int methodMonitor)
	{
		getMethodMonitor(methodMonitor).resetCalls();
	}
	
	// NOTE: Called by game
	public static void startCall(int methodMonitor, String className, String methodName)
	{
		getMethodMonitor(methodMonitor).beginCall(className, methodName, null);
	}

	// NOTE: Called by game (and other startCall() methods in this class)
	public static void startCall(int methodMonitor, String className, String methodName, Object sourceObject)
	{
		getMethodMonitor(methodMonitor).beginCall(className, methodName, sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, byte sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}
	
	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, short sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}
	
	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, int sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, long sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, float sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, double sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, char sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, String sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void startCall(int methodMonitor, String className, String methodName, boolean sourceObject)
	{
		startCall(methodMonitor, className, methodName, (Object)sourceObject);
	}
	
	// NOTE: Called by game
	public static void endCall(int methodMonitor, String className, String methodName)
	{
		getMethodMonitor(methodMonitor).finalizeCall(className, methodName);
	}

	// NOTE: Called by game (and other endCall() methods in this class)
	public static void endCall(int methodMonitor, String className, String methodName, Object sourceObject)
	{
		getMethodMonitor(methodMonitor).finalizeCall(className, methodName, sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, byte sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}
	
	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, short sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}
	
	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, int sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, long sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, float sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, double sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, char sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, String sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	// NOTE: Called by game (because Javassist is failing me...)
	public static void endCall(int methodMonitor, String className, String methodName, boolean sourceObject)
	{
		endCall(methodMonitor, className, methodName, (Object)sourceObject);
	}

	static void patch() throws CannotCompileException, NotFoundException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
		
		for (Map.Entry<Integer, MethodMonitor> e : methodMonitors.entrySet()) {
			MethodMonitor mm = e.getValue();

			CtClass cls = cp.get(mm.getClassName());
			CtMethod met = cls.getDeclaredMethod(mm.getMethodName());
	        met.insertBefore(""
        		+ "{"
        		+ "		" + ServerMonitor.class.getName() + ".resetCalls(" + mm.hashCode() + ");"
        		+ ""
        		+ "}");

	        Map<String, Call> monitoredCalls = mm.getMonitoredCalls();
	        met.instrument(new ExprEditor(){
		        public void edit(MethodCall m) throws CannotCompileException {
		        	if(monitoredCalls.containsKey(m.getClassName() + "." + m.getMethodName())) {
			        	logger.info("in [" + cls.getName() + "." + met.getName() + "()] adding monitor of calls to " + m.getClassName() + "." + m.getMethodName() + "()");

			        	boolean isStatic = Modifier.isStatic(met.getModifiers());
			        	String replaceWith = ""
			        			+ "{"
			        			+ createCallString(mm, m, isStatic, "startCall")
				        		+ "$_ = $proceed($$);"
			        			+ createCallString(mm, m, isStatic, "endCall")
			        			+ "}";
			        	m.replace(replaceWith);
		        	}
		        }
		    });
	        
		}
	}
	
	private static String createCallString(MethodMonitor mm, MethodCall m, boolean isStatic, String callCallMethod)
	{
		String ret = ServerMonitor.class.getName() + "." + callCallMethod + "(" + mm.hashCode() + ", \"" + m.getClassName() + "\", \"" + m.getMethodName() + "\"";

    	String callArgString = mm.getCallArgs(m.getClassName(), m.getMethodName());

    	if(callArgString != null) {
    		ret += ", " + callArgString + ");";

    	} else if(isStatic == false && Mod.attachThisToMonitors == true) {
    		ret += ", this);";

    	} else {
    		ret += ");";
    	}

//    	logger.info("\t" + ret);

    	return ret;
	}
	

	static String getServerObjectString(Object argObject)
	{
		String sourceObjectStr = "";
		if(argObject != null) {
			switch(argObject.getClass().getName()) {
				case "com.wurmonline.server.zones.Zone" :
				case "com.wurmonline.server.zones.DbZone" :
					Zone z = ((Zone)argObject);
					sourceObjectStr = "zone:" + z.getStartX() + "," + z.getStartY() + " - " + z.getEndX() + "," + z.getEndY();
					break;
				default :
					sourceObjectStr = argObject.toString();
					break;
			}
		} else {
			sourceObjectStr = "null";
		}
		
		return sourceObjectStr;
	}

}
