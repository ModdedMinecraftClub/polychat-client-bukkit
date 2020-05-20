package club.moddedminecraft.polychat.bukkitclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import club.moddedminecraft.polychat.networking.io.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.java.JavaPlugin;

import club.moddedminecraft.polychat.bukkitclient.threads.ActivePlayerThread;
import club.moddedminecraft.polychat.bukkitclient.threads.ReattachThread;
import org.bukkit.scheduler.BukkitScheduler;

public final class BukkitClient extends JavaPlugin implements Listener{
	
	public static boolean shutdownClean = false;
    public static MessageBus messageBus = null;
    public static Properties properties;
    public static File propertiesFolder;
    public static ReattachThread reattachThread;
    public static ActivePlayerThread playerThread;
    public static String idJson = null;
    public static String idJsonNoColor = null;
    public static boolean reattachKill = false;
	public static String serverIdText = null;
	public static ArrayList<String> commands = new ArrayList<>();

    public static void handleClientConnection() {
        try {
            messageBus = new MessageBus(new Socket(properties.getProperty("address"), Integer.parseInt(properties.getProperty("port"))), new ReceiverCallback(){
                @Override
                public void receive(AbstractMessage abstractMessage){
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
        } catch (NullPointerException ignored) {}
    }

    public static void sendGameMessage(String message) {
        Bukkit.broadcastMessage(message);
    }

    public static int calculateParameters(String command) {
        Pattern pattern = Pattern.compile("(\\$\\d+)");
        Matcher matcher = pattern.matcher(command);
        return matcher.groupCount();
    }

    public static void runCommand(String command) {  //Command Firer

        //Seems a little heavyweight to be doing this for every command but (1) it means we can update config with
        //server running and (2) we dont use many commands...

        Properties override_properties = new Properties();
        File override_config = new File(propertiesFolder, "polychat_override.properties");

        //Loads overrides if it exists or creates a sample config if not
        if (override_config.exists() && override_config.isFile()) {
            try (FileInputStream istream = new FileInputStream(override_config)) {
                override_properties.load(istream);
                System.out.println("override properties has " + override_properties.size() + " entries)");
            } catch (IOException e) {
                System.err.println("Error loading override configuration file!");
                e.printStackTrace();
            }
        } else {
            override_properties.setProperty("override_command_sample", "say hello to $2");
            try (FileOutputStream ostream = new FileOutputStream(override_config)) {
                override_properties.store(ostream, null);
            } catch (IOException e) {
                System.err.println("Error saving new configuration file!");
                e.printStackTrace();
            }
        }

        System.out.println("Polychat: Received Command <" + command + ">");

        String[] args = command.split("\\s+");
        String override_lookup = "override_command_" + args[0];
        String override = override_properties.getProperty( override_lookup, "");

        if (!override.isEmpty()) {
            command = override;

            // get the last instance of every unique $(number)
            // ie. /ranks set $1 $2 $1 $3 returns $2 $1 $3
            Pattern pattern = Pattern.compile("(\\$\\d+)(?!.*\\1)");
            Matcher matcher = pattern.matcher(override);

            while (matcher.find()) {
                int argCount = calculateParameters(override);
                if (args.length < 1) {
                    System.err.println("Error running command: Server prefix required");
                    return;
                }

                if (args.length < (argCount - 1)) {
                    System.err.println("Expected at least " + argCount + " parameters, received " + (args.length - 1));
                    return;
                }

                for (int i = 0; i <= matcher.groupCount(); i++) {
                    String toBeReplaced = matcher.group(i);
                    String replaceWith;
                    int argNum = Integer.parseInt(toBeReplaced.substring(1));
                    replaceWith = args[argNum - 1];
                    command = command.replace(toBeReplaced, replaceWith);
                }
            }
            System.out.println("Polychat: Command overridden to <" + command + ">");
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public static void addCommand(String command) {  //Holding place for commands
        commands.add(command);
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

    	Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            @Override
            public void run(){
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
                if (commands.size() != 0) {
                    int i = 0;
                    while(i<commands.size()){
                        runCommand(commands.get(i));
                        i++;
                    }
                    commands.clear();
                }
            }
        }, 0L, 20L);
        if(!reattachKill) { //only start it on a fresh start, not on a reload
            reattachThread.start();//actually start the thread at the end so the main thread is running already
        }else{

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
                color = colorSwitch(code);
            }
            idJsonNoColor = idText;
            idText = color + "" + idText;
            idJson = idText;
        }
    }



}
