package com.filterbubbles.wu.serverdebug;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;

import com.wurmonline.server.creatures.Communicator;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * @author Friya, 18-Jul-2018
 */
public class Patcher 
{
	private static Logger logger = Logger.getLogger(Patcher.class.getName());
	private static Patcher instance = null;
	
	protected static Patcher getInstance()
	{
		if(instance == null) {
			instance = new Patcher();
		}
		return instance;
	}
	
	protected void configure()
	{
		try {
			if(Mod.monitorDefaultModHooks) {
				patchModLauncher();
			}
		} catch (NotFoundException | CannotCompileException e) {
			throw new RuntimeException("failed to patch modlauncher: ", e);
		}
	}

	protected void init()
	{
		try {
			if(Mod.logCmdIn) {
				interceptReallyHandle();
			}
			
			if(Mod.logCmdOut) {
				patchSends();
			}
			
			if(Mod.monitorGameLoop) {
				patchMonitoredMethods();
			}
			
			if(Mod.logNewAction) {
				patchActionCtors();
			}
			
			if(false && Mod.friyasDevelopmentServer) {
				patchTest();
			}

//			patchCmdAction();
			if(Mod.loginHandlerHealthNotice) {
				patchLoginHandler();
			}

			if(Mod.socketServerHealthNotice) {
				patchSocketServer();
			}
		} catch (NotFoundException | CannotCompileException e) {
			throw new RuntimeException("failed to patch game: ", e);
		}
	}
	
	private void patchTest() throws NotFoundException, CannotCompileException
	{
		// Test: in checkMove, add some lag -- what does monitor say?
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.server.creatures.Creature");
        CtMethod met = cls.getDeclaredMethod("checkMove");

        met.insertBefore(
        		"{"
        		+ "		" + Mod.class.getName() + ".fakeLag();"
//        		+ "		" + Mod.class.getName() + ".fakeError();"
        		+ "}"
        );
        logger.info("patchTest -- added fakeLag() call!");
		
		// Test: in checkMove, add some exception -- what happens to monitoring?
	}
	
	void logCallsInMethod(String classAndMethod)
	{
		StringBuffer sb = new StringBuffer();
		ClassPool cp = HookManager.getInstance().getClassPool();
		
		sb.append("Outputting calls made from " + classAndMethod + "()...\n");
		try {
			String[] cm = ServerMonitor.getClassAndMethodOf(classAndMethod);
	        CtClass cls = cp.get(cm[0]);
	        CtMethod met = cls.getDeclaredMethod(cm[1]);
	        
	        met.instrument(new ExprEditor(){
		        public void edit(MethodCall m) throws CannotCompileException {
		        	sb.append("\t");
		        	sb.append(m.getClassName());
		        	sb.append(".");
		        	sb.append(m.getMethodName());
		        	sb.append(",\\\n");
		        }
		    });
	        
	        
		} catch(NotFoundException | CannotCompileException e) {
			logger.log(Level.SEVERE, "failed to list calls in " + classAndMethod, e);
		}
	
		logger.info("Method calls made from " + classAndMethod + ":\n" + sb.toString());
		logger.info("Tip: to add monitoring of any of the above calls, use:\nmonitor." + classAndMethod + "=x.y.z\nmonitorThreshold." + classAndMethod + "=1000\n");
	}
	
	private void patchModLauncher() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("org.gotti.wurmunlimited.modloader.server.ServerHook");

		//patchModLauncherMethod("serverStarted", cls);
		//patchModLauncherMethod("itemTemplatesCreated", cls);
		patchModLauncherMethod("message", "playerMessage", cls, "$1, $2, $3");
		patchModLauncherMethod("playerLogin", cls, "$1");
		patchModLauncherMethod("playerLogout", "playerLogin", cls, "$1");
		patchModLauncherMethod("serverPoll", cls);
		patchModLauncherMethod("kingdomMessage", "channelMessage", cls, "$1");
		patchModLauncherMethod("villageMessage", "channelMessage", cls, "$1, $2");
		patchModLauncherMethod("allianceMessage", "channelMessage", cls, "$1, $2");
	}
	
	private void patchModLauncherMethod(String hookName, CtClass c) throws CannotCompileException, NotFoundException
	{
        patchModLauncherMethod(hookName, hookName, c, null);
	}

	private void patchModLauncherMethod(String hookName, CtClass c, String extraArgs) throws CannotCompileException, NotFoundException
	{
        patchModLauncherMethod(hookName, hookName, c, extraArgs);
	}

	private void patchModLauncherMethod(String hookName, String varName, CtClass c, String extraArgs) throws CannotCompileException, NotFoundException
	{
		c.getDeclaredMethod("fireOn" + capitalize(hookName)).setBody(
			  "{"
			+ 	"	return " + ModMonitor.class.getName() + ".replaced" + capitalize(hookName) + "(" + varName + (extraArgs == null ? "" : "," + extraArgs) + ");"
			+ "}"
		);
		logger.info("Replaced fireOn" + hookName);
	}
	
	private String capitalize(String s)
	{
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	private void patchMonitoredMethods() throws CannotCompileException, NotFoundException
	{
		ServerMonitor.patch();
        logger.info("end of patchMonitoredMethods()");
	}

	@SuppressWarnings({ "rawtypes", "serial" })
	private <T> List getDuplicate(Collection<T> list) {

	    final List<T> duplicatedObjects = new ArrayList<T>();
	    Set<T> set = new HashSet<T>() {
		    @Override
		    public boolean add(T e) {
		        if (contains(e)) {
		            duplicatedObjects.add(e);
		        }
		        return super.add(e);
		    }
	    };
	    for (T t : list) {
	        set.add(t);
	    }
	    return duplicatedObjects;
	}
	
	@SuppressWarnings("unused")
	private <T> boolean hasDuplicate(Collection<T> list) {
	    if (getDuplicate(list).isEmpty())
	        return false;
	    return true;
	}	
	
	
	private void patchSocketServer() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.communication.SocketServer");
        CtMethod met = cls.getDeclaredMethod("tick");

        met.insertBefore(
        		"{"
        		+ "		" + MiscMonitor.class.getName() + ".log(\"" + cls.getName() + "." + met.getName() + "()\", 0);"
        		+ "}"
        );
        logger.info("patchSocketServer");
	}

	private void patchLoginHandler() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.server.LoginHandler");
        CtMethod met = cls.getDeclaredMethod("reallyHandle");

        met.insertBefore(
        		"{"
        		+ "		" + MiscMonitor.class.getName() + ".log(\"com.wurmonline.server.LoginHandler.reallyHandle()\");"
        		+ "}"
        );
	}
	
	private void patchActionCtors() throws NotFoundException, CannotCompileException
	{
		// Action(final Creature aPerformer, final long aSubj, final long _target, final short act,
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.server.behaviours.Action");
        CtConstructor[] mets = cls.getDeclaredConstructors();
        for(CtConstructor met : mets) {
        	met.insertBefore(""
        			+ "{"
        			+ "		String myActName = \"\";"
            		+ "		if($4 > 0 && $4 < com.wurmonline.server.behaviours.Actions.actionEntrys.length) {"
            		+ "			myActName = com.wurmonline.server.behaviours.Actions.getActionString((short)$4);"
            		+ "		}"
            		+ ""
            		+ "		String myCritterName = \"UNKNOWN!?\";"
            		+ "		if($1 != null) {"
            		+ "			myCritterName = $1.getName() + \" [npc:\" + $1.isNpc() + \"] \" + $1.getTilePos();"
            		+ "		}"
            		+ "		" + MiscMonitor.class.getName() + ".log(\"new Action(): \" + myCritterName + \" \" + $2 + \" \" + $3 + \" \" + $4 + \" \" + myActName);"
        			+ "}"
        	);
        }
        
	}

	private void patchSends() throws NotFoundException, CannotCompileException
	{
		ClassPool cp = HookManager.getInstance().getClassPool();
        CtClass cls = cp.get("com.wurmonline.server.creatures.Communicator");
        CtMethod[] mets = cls.getDeclaredMethods();
        for(CtMethod met : mets) {
        	if(met.getName().startsWith("send") 
        			&& met.getName().equals("sendByteStringLength") == false
        			&& met.getName().equals("sendShortStringLength") == false
        			&& met.getName().equals("sendTileStrip") == false
        			&& met.getName().equals("sendTileStripFar") == false
        		) {
                met.insertBefore(""
                		+ "{"
                		+ "		" + MiscMonitor.class.getName() + ".logMisc(this, \"cmd_out:\t" + met.getName() + "\");"
                		+ "}"
                );
        		logger.info("Added logger for " + met.getName());
        	}
        }
	}


	private void interceptReallyHandle() throws NotFoundException, CannotCompileException
	{
        HookManager.getInstance().registerHook("com.wurmonline.server.creatures.Communicator", "reallyHandle", null,
            new InvocationHandlerFactory() {
                @Override
                public InvocationHandler createInvocationHandler() {
                    return new InvocationHandler() {
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							
							ByteBuffer bb = (ByteBuffer)args[1];
							final byte[] buff = new byte[bb.remaining()];
							bb.duplicate().get(buff);
							
							if(buff.length > 0) {
								Communicator com = (Communicator)proxy;
								MiscMonitor.logMisc(com, "cmd_in:\t" + buff[0] + "(" + buff.length + ")");
							}
							
							return method.invoke(proxy, args);
						}
                    };
                }
        	}
        );
        
	} // interceptReallyHandle
}
