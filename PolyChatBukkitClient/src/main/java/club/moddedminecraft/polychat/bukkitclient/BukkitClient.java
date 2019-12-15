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
    @Override
    public void onEnable() {
    	getLogger().info("onEnable has been invoked!");
    	new EventListener(this);
    }
    
    @Override
    public void onDisable() {
    	getLogger().info("onDisable has been invoked!");
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

}
