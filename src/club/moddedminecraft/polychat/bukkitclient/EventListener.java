package club.moddedminecraft.polychat.bukkitclient;

import club.moddedminecraft.polychat.networking.io.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Timer;
import java.util.TimerTask;

public class EventListener implements Listener {
    private static Server server;

    public EventListener(BukkitClient plugin) {
        server = plugin.getServer();
        server.getPluginManager().registerEvents(this, plugin);
    }

    public static void handleMessage(AbstractMessage message) {
        String finalMessage = null;
        String broadcastPrefix;
        if (message instanceof BroadcastMessage) {
            BroadcastMessage broadcastMessage = ((BroadcastMessage) message);  //Gets message
            broadcastPrefix = broadcastMessage.getPrefix();  //Gets the prefix

            ChatColor colorType = ChatColor.WHITE; //Sets a placeholder white
            int color = broadcastMessage.prefixColor();  //gets the color from message
            if ((color >= 0) && (color <= 15))
                colorType = ChatColor.getByChar(String.format("%01x", color));  //makes sure color is valid and sets it to bukkit color type
            finalMessage = broadcastPrefix + "" + colorType + "" + broadcastMessage.getMessage() + ChatColor.WHITE; //Prefix: color, message then white to finalMessage

        } else if (message instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage) message;
            if (chatMessage.getFormattedMessage().equals("empty")) {
                String prefix = ChatColor.DARK_PURPLE + "" + "[Discord] "; //Sets prefix to discord
                ChatColor colorType = ChatColor.WHITE; //Sets colorType to Dark Purple for the rest of a discord message
                finalMessage = prefix + "" + colorType + "" + chatMessage.getUsername() + " " + chatMessage.getMessage(); //Appends the color, username and message to the finalMessage
            } else {
                finalMessage = chatMessage.getFormattedMessage();
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
                finalMessage = serverStatus.getFormattedPrefix() + "§r" + finalMessage;
            }
        } else if (message instanceof PlayerStatusMessage) {
            PlayerStatusMessage playerStatus = ((PlayerStatusMessage) message);
            if (!(playerStatus.getSilent())) {
                if (playerStatus.getJoined()) {
                    finalMessage = " " + playerStatus.getUserName() + " has joined the game";
                } else {
                    finalMessage = " " + playerStatus.getUserName() + " has left the game";
                }

                finalMessage = playerStatus.getFormattedPrefix() + "§r" + finalMessage;
            }
        } else if (message instanceof CommandMessage) {
            CommandMessage commandMessage = (CommandMessage) message;
            final BukkitCommandSender sender = new BukkitCommandSender(commandMessage, server, BukkitClient.properties.getProperty("id_color", "15"));

            BukkitClient.queueCommand(sender);
        }
        if (finalMessage != null) {
            Bukkit.getServer().getLogger().info(finalMessage);
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                player.sendMessage(finalMessage);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String id = BukkitClient.properties.getProperty("server_id");
        event.setFormat(BukkitClient.idFormatted + " §7%s: §r%s");

        String name = event.getPlayer().getDisplayName();
        String cleanName = "";
        for (int i = 0; i < name.length(); ++i) {
            if (name.charAt(i) == '§') {
                ++i;
            } else {
                cleanName += name.charAt(i);
            }
        }

        String formattedName = BukkitClient.id + " " + cleanName + ": ";
        ChatMessage chatMessage = new ChatMessage(formattedName, event.getMessage(), String.format(event.getFormat(), cleanName, event.getMessage()));
        BukkitClient.sendMessage(chatMessage);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerStatusMessage loginMsg = new PlayerStatusMessage(event.getPlayer().getName(), BukkitClient.id, BukkitClient.idFormatted, true, false);
        BukkitClient.sendMessage(loginMsg);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        PlayerStatusMessage logoutMsg = new PlayerStatusMessage(event.getPlayer().getName(), BukkitClient.id, BukkitClient.idFormatted, false, false);
        BukkitClient.sendMessage(logoutMsg);
    }

}
