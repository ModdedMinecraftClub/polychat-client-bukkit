package club.moddedminecraft.polychat.bukkitclient;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitClient extends JavaPlugin implements Listener{
	
	public static boolean shutdownClean = false;
	
    @Override
    public void onEnable() {
    	new EventListener(this);
    	new PlayerEvent(null,"start",null);
    	Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
    }
    
    @Override
    public void onDisable() {
    	getLogger().info("onDisable has been invoked!");
    	shutdownClean = true;
    	new PlayerEvent(null,"stop",null);
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    	getLogger().info("Player " + event.getPlayer().getName() + " is logging in");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (cmd.getName().equalsIgnoreCase("test")) { 
    		getLogger().info("Test has been invoked!");
    		return true;
    	} //If this has happened the function will return true. 
    	return false; 
    }
    
    public void shutdownHook() {

        //Sends either crashed or offline depending on if shutdown happened cleanly
        if (!shutdownClean) {
        	new PlayerEvent(null,"crash",null);
        }
        try {
            //Makes sure message has time to send
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }

}
