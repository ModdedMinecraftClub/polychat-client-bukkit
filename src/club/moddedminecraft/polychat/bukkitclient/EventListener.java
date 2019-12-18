package club.moddedminecraft.polychat.bukkitclient;

import club.moddedminecraft.polychat.networking.io.*;
import club.moddedminecraft.polychat.bukkitclient.BukkitClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Timer;
import java.util.TimerTask;

public class EventListener implements Listener{
    public EventListener(BukkitClient plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		event.setFormat(BukkitClient.idJson + "" + event.getFormat());
		String id = BukkitClient.properties.getProperty("server_id");
		String json = BukkitClient.idJson + " " +event.getPlayer().getDisplayName() + ": "+ event.getMessage();  //TODO: Fix this JSON encoding
		ChatMessage chatMessage = new ChatMessage(event.getPlayer().getDisplayName(), event.getMessage(), json);
		BukkitClient.sendMessage(chatMessage);
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		String id = BukkitClient.properties.getProperty("server_id");
		PlayerStatusMessage loginMsg = new PlayerStatusMessage(event.getPlayer().getName(), id, BukkitClient.idJson, true, false);
		BukkitClient.sendMessage(loginMsg);

		
	}
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		String id = BukkitClient.properties.getProperty("server_id");
		PlayerStatusMessage logoutMsg = new PlayerStatusMessage(event.getPlayer().getName(), id, BukkitClient.idJson, false, false);
		BukkitClient.sendMessage(logoutMsg);
	}

	public static void handleMessage(AbstractMessage message) {
		String finalMessage = null;
		String broadcastPrefix;
		if (message instanceof BroadcastMessage) {
			BroadcastMessage broadcastMessage = ((BroadcastMessage) message);  //Gets message
			broadcastPrefix = broadcastMessage.getPrefix();  //Gets the prefix

			ChatColor colorType = ChatColor.WHITE; //Sets a placeholder white
			int color = broadcastMessage.prefixColor();  //gets the color from message
			if ((color >= 0) && (color <= 15)) colorType = BukkitClient.colorSwitch(color);  //makes sure color is valid and sets it to bukkit color type
			finalMessage = broadcastPrefix + "" + colorType + "" + broadcastMessage.getMessage() + ChatColor.WHITE; //Prefix: color, message then white to finalMessage

		} else if (message instanceof ChatMessage) {
			ChatMessage chatMessage = (ChatMessage) message;
			if (chatMessage.getComponentJson().equals("empty")) {
				String prefix = ChatColor.DARK_PURPLE + "" +"[Discord] "; //Sets prefix to discord
				ChatColor colorType = ChatColor.WHITE; //Sets colorType to Dark Purple for the rest of a discord message
				finalMessage = prefix + "" + colorType + "" + chatMessage.getUsername() + " " + chatMessage.getMessage(); //Appends the color, username and message to the finalMessage
			} else {
				finalMessage = chatMessage.getComponentJson();//TODO: Fix the JSON decoding
			}
		} else if (message instanceof ServerStatusMessage) {
			ServerStatusMessage serverStatus = ((ServerStatusMessage) message);
			switch (serverStatus.getState()) {
				case 1:
					finalMessage = " Server Online";
					break;
				case 2:
					finalMessage = " Server Offline";
					break;
				case 3:
					finalMessage = " Server Crashed";
					break;
				default:
					System.err.println("Unrecognized server state " + serverStatus.getState() + " received from " + serverStatus.getServerID());
			}
			if (finalMessage != null) {
				finalMessage = serverStatus.getPrefixJson() + "" + ChatColor.WHITE + "" + finalMessage; //Server Prefix: *White* Server Status
			}
		} else if (message instanceof PlayerStatusMessage) {
			PlayerStatusMessage playerStatus = ((PlayerStatusMessage) message);
			if (!(playerStatus.getSilent())) {
				if (playerStatus.getJoined()) {
					finalMessage = " " + playerStatus.getUserName() + " has joined the game";
				} else {
					finalMessage = " " + playerStatus.getUserName() + " has left the game";
				}
				finalMessage = playerStatus.getPrefixJson() + "" + ChatColor.WHITE + "" + finalMessage; //Server Prefix: *White* Player name has left/joined

			}
		} else if (message instanceof CommandMessage) {
			String command = ((CommandMessage) message).getCommand();
			String channel = ((CommandMessage) message).getChannel();
			String serverID = ((CommandMessage) message).getServerID();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			// send command output to discord in .5 seconds
			new Timer().schedule(new TimerTask() { //TODO: Command output returning
				@Override
				public void run() {
					Object sender = null;
					Bukkit.broadcastMessage("Placeholder");
				}
			}, 500);
		}
		if (finalMessage != null) Bukkit.broadcastMessage(finalMessage);
	}
}
