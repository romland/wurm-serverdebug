package com.filterbubbles.wu.serverdebug;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modcomm.ModComm;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modloader.server.Listeners;

import com.wurmonline.server.Message;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.PvPAlliance;
import com.wurmonline.server.villages.Village;

public class ModMonitor implements Monitor
{
	private static Logger logger = Logger.getLogger(ModMonitor.class.getName());
	private static Map<String, Call> calls = new ConcurrentHashMap<String, Call>(); 

	protected static Call getCall(String className, String hookName)
	{
		if(calls.containsKey(hookName) == false) {
			calls.put(hookName, new Call(className, hookName));
		}

		return calls.get(hookName);
	}

	protected static void periodicSlowCallMonitoring(int threshold)
	{
		for (Map.Entry<String, Call> e : calls.entrySet()) {
			if(e.getValue().isRunning() && e.getValue().getDuration() > threshold) {
				logger.severe("Hey Friya! [running] slow mod: " + e.getValue().toString());
			}
		}
	}

	//replacedServerStarted
	//replacedItemTemplatesCreated
	
	public static boolean replacedMessage(Listeners<PlayerMessageListener, MessagePolicy> playerMessage, Communicator communicator, String message, String title)
	{
		// XXX ugh, wtf I can't get Eclipse to accept multiple types to Listeners. Using separate method to measure cost.
		return playerMessage.fire(listener -> onOnePlayerMessage(listener, communicator, message, title), () -> MessagePolicy.PASS, MessagePolicy.ANY_DISCARDED).orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}
	
	private static MessagePolicy onOnePlayerMessage(PlayerMessageListener listener, Communicator communicator, String message, String title)
	{
		Call c = getCall(listener.getClass().getName(), "onPlayerMessage");
		c.beginCall();
		MessagePolicy ret = listener.onPlayerMessage(communicator, message, title);
		c.finalizeCall();
		c.reset();
		return ret;
	}

	public static void replacedPlayerLogin(Listeners<PlayerLoginListener, Void> playerLogin, Player player)
	{
		ModComm.playerConnected(player);
		playerLogin.fire(listener -> {
			Call c = getCall(listener.getClass().getName(), "onPlayerLogin");
			c.beginCall();
			listener.onPlayerLogin(player);
			c.finalizeCall();
			c.reset();
		});
	}
	
	public static void replacedPlayerLogout(Listeners<PlayerLoginListener, Void> playerLogin, Player player)
	{
		playerLogin.fire(listener -> {
			Call c = getCall(listener.getClass().getName(), "onPlayerLogout");
			c.beginCall();
			listener.onPlayerLogout(player);
			c.finalizeCall();
			c.reset();
		});
	}

	public static void replacedServerPoll(Listeners<ServerPollListener, Void> serverPoll)
	{
		serverPoll.fire(listener -> {
			Call c = getCall(listener.getClass().getName(), "onServerPoll");
			c.beginCall();
			listener.onServerPoll();
			c.finalizeCall();
			c.reset();
		});
	}
	
	public static boolean replacedKingdomMessage(Listeners<ChannelMessageListener, MessagePolicy> channelMessage, Message message)
	{
		return channelMessage.fire(listener -> onOneKingdomMessage(listener, message), () -> MessagePolicy.PASS, MessagePolicy.ANY_DISCARDED).orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}

	private static MessagePolicy onOneKingdomMessage(ChannelMessageListener listener, Message message)
	{
		Call c = getCall(listener.getClass().getName(), "onKingdomMessage");
		c.beginCall();
		MessagePolicy ret = listener.onKingdomMessage(message);
		c.finalizeCall();
		c.reset();
		return ret;
	}

	public static boolean replacedVillageMessage(Listeners<ChannelMessageListener, MessagePolicy> channelMessage, Village village, Message message)
	{
		return channelMessage.fire(listener -> onOneVillageMessage(listener, village, message), () -> MessagePolicy.PASS, MessagePolicy.ANY_DISCARDED).orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}

	private static MessagePolicy onOneVillageMessage(ChannelMessageListener listener, Village village, Message message)
	{
		Call c = getCall(listener.getClass().getName(), "onVillageMessage");
		c.beginCall();
		MessagePolicy ret = listener.onVillageMessage(village, message);
		c.finalizeCall();
		c.reset();
		return ret;
	}
	
	public static boolean replacedAllianceMessage(Listeners<ChannelMessageListener, MessagePolicy> channelMessage, PvPAlliance alliance, Message message)
	{
		return channelMessage.fire(listener -> onOneAllianceMessage(listener, alliance, message), () -> MessagePolicy.PASS, MessagePolicy.ANY_DISCARDED).orElse(MessagePolicy.PASS) == MessagePolicy.DISCARD;
	}

	private static MessagePolicy onOneAllianceMessage(ChannelMessageListener listener, PvPAlliance alliance, Message message)
	{
		Call c = getCall(listener.getClass().getName(), "onAllianceMessage");
		c.beginCall();
		MessagePolicy ret = listener.onAllianceMessage(alliance, message);
		c.finalizeCall();
		c.reset();
		return ret;
	}

}
