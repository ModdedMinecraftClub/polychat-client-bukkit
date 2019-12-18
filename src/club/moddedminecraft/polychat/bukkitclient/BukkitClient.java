package club.moddedminecraft.polychat.bukkitclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;

import club.moddedminecraft.polychat.networking.io.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.java.JavaPlugin;

import club.moddedminecraft.polychat.bukkitclient.threads.ActivePlayerThread;
import club.moddedminecraft.polychat.bukkitclient.threads.ReattachThread;

public final class BukkitClient extends JavaPlugin implements Listener{
	
	public static boolean shutdownClean = false;
    public static MessageBus messageBus = null;
    public static Properties properties;
    public static ReattachThread reattachThread;
    public static ActivePlayerThread playerThread;
    public static String idJson = null;
    public static String idJsonNoColor = null;
	public static Object serverIdText = null;

    public static void handleClientConnection() {
        try {
            messageBus = new MessageBus(new Socket(properties.getProperty("address"), Integer.parseInt(properties.getProperty("port"))), EventListener::handleMessage);
            messageBus.start();
        } catch (IOException e) {
            System.err.println("Failed to establish polychat connection!");
            e.printStackTrace();
        }
    }

    public static void sendMessage(AbstractMessage message) {
        try {
            messageBus.sendMessage(message);
        } catch (NullPointerException ignored) {}
    }

    public static void sendGameMessage(String message) {
        Bukkit.broadcastMessage(message);
    }

    @Override
    public void onEnable() {
        //TODO: check if the folder exists
        handleConfiguration(this.getDataFolder());
        handlePrefix();
        reattachThread = new ReattachThread(5000);
        playerThread = new ActivePlayerThread(30000, properties.getProperty("server_id", "DEFAULT_ID"));
        handleClientConnection();
        
    	new EventListener(this);

    	Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));

        ServerStatusMessage onlineMsg = new ServerStatusMessage(properties.getProperty("server_id"), idJson, (short) 1);
        sendMessage(onlineMsg);
    }
    
    @Override
    public void onDisable() {
    	shutdownClean = true;
        ServerStatusMessage offlineMsg = new ServerStatusMessage(properties.getProperty("server_id"), idJson, (short) 2);
        sendMessage(offlineMsg);

    }

    
    public void shutdownHook() {
        reattachThread.interrupt();
        playerThread.interrupt();
        //Sends either crashed or offline depending on if shutdown happened cleanly
        if (!shutdownClean) {
            ServerStatusMessage crashMsg = new ServerStatusMessage(properties.getProperty("server_id"), idJson, (short) 3);
            sendMessage(crashMsg);
        }
        try {
            //Makes sure message has time to send
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }

	public static ArrayList<String> getOnlinePlayersNames() { //Might have to fix return type
		ArrayList<String> playerList = new ArrayList<> ();
		
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
		    playerList.add(player.getName());
		}
		return playerList;
	}
	
	public static int getMaxPlayers() { 
		return Bukkit.getMaxPlayers();
	}


	public static ChatColor colorSwitch(int colorInt){
        switch(colorInt){
            case 0: return ChatColor.getByChar('0');
            case 1: return ChatColor.getByChar('1');
            case 2: return ChatColor.getByChar('2');
            case 3: return ChatColor.getByChar('3');
            case 4: return ChatColor.getByChar('4');
            case 5: return ChatColor.getByChar('5');
            case 6: return ChatColor.getByChar('6');
            case 7: return ChatColor.getByChar('7');
            case 8: return ChatColor.getByChar('8');
            case 9: return ChatColor.getByChar('9');
            case 10: return ChatColor.getByChar('a');
            case 11: return ChatColor.getByChar('b');
            case 12: return ChatColor.getByChar('c');
            case 13: return ChatColor.getByChar('d');
            case 14: return ChatColor.getByChar('e');
            case 15: return ChatColor.getByChar('f');
            default: return ChatColor.getByChar("0");
        }
    }

    public void handleConfiguration(File modConfigDir) {
        BukkitClient.properties = new Properties();
        File config = new File(modConfigDir, "polychat.properties");

        //Loads config if it exists or creates a default one if not
        if (config.exists() && config.isFile()) {
            try (FileInputStream istream = new FileInputStream(config)) {
                BukkitClient.properties.load(istream);
            } catch (IOException e) {
                System.err.println("Error loading configuration file!");
                e.printStackTrace();
            }
        } else {
            BukkitClient.properties.setProperty("address", "127.0.0.1");
            BukkitClient.properties.setProperty("port", "25566");
            BukkitClient.properties.setProperty("server_id", "test");
            BukkitClient.properties.setProperty("server_name", "Test Server");
            BukkitClient.properties.setProperty("server_address", "empty");
            BukkitClient.properties.setProperty("id_color", "15"); //Default to white color
            try (FileOutputStream ostream = new FileOutputStream(config)) {
                BukkitClient.properties.store(ostream, null);
            } catch (IOException e) {
                System.err.println("Error saving new configuration file!");
                e.printStackTrace();
            }
        }
    }
    public void handlePrefix() {
        String idText = properties.getProperty("server_id");
        ChatColor color;
        if (!(idText.equals("empty"))) {
            int code = Integer.parseInt(properties.getProperty("id_color"));
            if ((code < 0) || (code > 15)) {
                color = ChatColor.getByChar('f');
            } else {
                color = colorSwitch(code);
            }
            idJsonNoColor = idText;
            idText = color + "" + idText;
            idJson = idText;
        }
    }



}
