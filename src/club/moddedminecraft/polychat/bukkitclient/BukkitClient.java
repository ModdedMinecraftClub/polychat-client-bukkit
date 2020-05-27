package club.moddedminecraft.polychat.bukkitclient;

import club.moddedminecraft.polychat.bukkitclient.threads.ActivePlayerThread;
import club.moddedminecraft.polychat.bukkitclient.threads.ReattachThread;
import club.moddedminecraft.polychat.networking.io.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BukkitClient extends JavaPlugin implements Listener {

    public static boolean shutdownClean = false;
    public static MessageBus messageBus = null;
    public static Properties properties;
    public static File propertiesFolder;
    public static ReattachThread reattachThread;
    public static ActivePlayerThread playerThread;
    public static String id = null;
    public static String idFormatted = null;
    public static boolean reattachKill = false;
    public static ArrayList<BukkitCommandSender> commands = new ArrayList<>();

    public static void handleClientConnection() {
        try {
            messageBus = new MessageBus(new Socket(properties.getProperty("address"), Integer.parseInt(properties.getProperty("port"))), new ReceiverCallback() {
                @Override
                public void receive(AbstractMessage abstractMessage) {
                    EventListener.handleMessage(abstractMessage);
                }
            });
            messageBus.start();
        } catch (IOException e) {
            System.err.println("Failed to establish polychat connection!");
            e.printStackTrace();
        }
    }

    public static void sendMessage(AbstractMessage message) {
        try {
            messageBus.sendMessage(message);
        } catch (NullPointerException ignored) {
        }
    }

    public static void sendGameMessage(String message) {
        Bukkit.broadcastMessage(message);
    }

    public static int calculateParameters(String command) {
        Pattern pattern = Pattern.compile("(\\$\\d+)");
        Matcher matcher = pattern.matcher(command);
        return matcher.groupCount();
    }

    public static void queueCommand(BukkitCommandSender sender) {  //Holding place for commands
        commands.add(sender);
    }

    public static ArrayList<String> getOnlinePlayersNames() { //Might have to fix return type
        ArrayList<String> playerList = new ArrayList<>();

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            playerList.add(player.getName());
        }
        return playerList;
    }

    public static int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public void onEnable() {
        //TODO: check if the folder exists
        handleConfiguration(this.getDataFolder());
        handlePrefix();

        reattachThread = new ReattachThread(5000);
        playerThread = new ActivePlayerThread(30000, id);

        handleClientConnection();

        new EventListener(this);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdownHook();
            }
        }));

        ServerInfoMessage infoMessage = new ServerInfoMessage(
                id,
                BukkitClient.properties.getProperty("server_name", "DEFAULT_NAME"),
                BukkitClient.properties.getProperty("server_address", "DEFAULT_ADDRESS"),
                BukkitClient.getMaxPlayers()
        );

        BukkitClient.sendMessage(infoMessage);

        ServerStatusMessage onlineMsg = new ServerStatusMessage(id, idFormatted, (short) 1);
        sendMessage(onlineMsg);

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() { //Repeating task for listening for commands so they are ran on the right thread
            @Override
            public void run() {
                for (BukkitCommandSender sender : commands) {
                    String command = sender.getCommand();
                    if (command != null) {
                        try {
                            getServer().dispatchCommand(sender, command);
                            sender.sendOutput();
                        } catch (Exception e) {
                            System.err.println(e);
                        }
                    }
                }
                commands.clear();
            }
        }, 0L, 20L);

        reattachThread.start(); //start the thread at the end so the main thread is running already
        playerThread.start();
    }

    @Override
    public void onDisable() {
        shutdownClean = true;
        reattachKill = true;

        ServerStatusMessage offlineMsg = new ServerStatusMessage(id, idFormatted, (short) 2);
        sendMessage(offlineMsg);

        try {
            //Makes sure message has time to send
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        messageBus.stop();

        playerThread.interrupt();
        reattachThread.interrupt();

    }

    public void shutdownHook() {
        //Sends either crashed or offline depending on if shutdown happened cleanly
        if (!shutdownClean) {
            ServerStatusMessage crashMsg = new ServerStatusMessage(id, idFormatted, (short) 3);
            sendMessage(crashMsg);
        }
        try {
            //Makes sure message has time to send
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        messageBus.stop();
    }

    public void handleConfiguration(File modConfigDir) {
        BukkitClient.propertiesFolder = modConfigDir;
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
        String serverId = properties.getProperty("server_id");
        if (!(serverId.equals("empty"))) {
            int code = Integer.parseInt(properties.getProperty("id_color"));
            if ((code < 0) || (code > 15)) {
                code = 15;
            }
            id = serverId;
            idFormatted = String.format("§%01x%s", code, serverId);
        }
    }

}
