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

    private static final HashMap<Integer, ChatColor> colorHashMap = new HashMap<Integer, ChatColor>() {{
        put(0, ChatColor.getByChar('0'));
        put(1, ChatColor.getByChar('1'));
        put(2, ChatColor.getByChar('2'));
        put(3, ChatColor.getByChar('3'));
        put(4, ChatColor.getByChar('4'));
        put(5, ChatColor.getByChar('5'));
        put(6, ChatColor.getByChar('6'));
        put(7, ChatColor.getByChar('7'));
        put(8, ChatColor.getByChar('8'));
        put(9, ChatColor.getByChar('9'));
        put(10, ChatColor.getByChar('a'));
        put(11, ChatColor.getByChar('b'));
        put(12, ChatColor.getByChar('c'));
        put(13, ChatColor.getByChar('d'));
        put(14, ChatColor.getByChar('e'));
        put(15, ChatColor.getByChar('f'));
    }};
    public static boolean shutdownClean = false;
    public static MessageBus messageBus = null;
    public static Properties properties;
    public static File propertiesFolder;
    public static ReattachThread reattachThread;
    public static ActivePlayerThread playerThread;
    public static String idJson = null;
    public static String idJsonNoColor = null;
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

    public static ChatColor getColor(int color) {
        return colorHashMap.getOrDefault(color, ChatColor.getByChar("0"));
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

    public Server getBukkitServer() {
        return getServer();
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

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdownHook();
            }
        }));

        ServerInfoMessage infoMessage = new ServerInfoMessage(
                BukkitClient.properties.getProperty("server_id", "DEFAULT_ID"),
                BukkitClient.properties.getProperty("server_name", "DEFAULT_NAME"),
                BukkitClient.properties.getProperty("server_address", "DEFAULT_ADDRESS"),
                BukkitClient.getMaxPlayers()
        );

        BukkitClient.sendMessage(infoMessage);

        ServerStatusMessage onlineMsg = new ServerStatusMessage(properties.getProperty("server_id"), idJson, (short) 1);
        sendMessage(onlineMsg);

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() { //Repeating task for listening for commands so they are ran on the right thread
            @Override
            public void run() {
                for (BukkitCommandSender sender : commands) {
                    getServer().dispatchCommand(sender, sender.getCommand());
                }
                commands.clear();
            }
        }, 0L, 20L);
        if (!reattachKill) { //only start it on a fresh start, not on a reload
            reattachThread.start();//actually start the thread at the end so the main thread is running already
        }
    }

    @Override
    public void onDisable() {
        shutdownClean = true;
        reattachKill = true;

        ServerStatusMessage offlineMsg = new ServerStatusMessage(properties.getProperty("server_id"), idJson, (short) 2);
        sendMessage(offlineMsg);

        try {
            //Makes sure message has time to send
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        messageBus.stop();

        //TODO: Close Threads

    }

    public void shutdownHook() {
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
        String idText = properties.getProperty("server_id");
        ChatColor color;
        if (!(idText.equals("empty"))) {
            int code = Integer.parseInt(properties.getProperty("id_color"));
            if ((code < 0) || (code > 15)) {
                color = ChatColor.getByChar('f');
            } else {
                color = getColor(code);
            }
            idJsonNoColor = idText;
            idText = color + "" + idText;
            idJson = idText;
        }
    }

}
