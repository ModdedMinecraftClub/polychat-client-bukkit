package club.moddedminecraft.polychat.bukkitclient;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener{
    public EventListener(BukkitClient plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		player.sendMessage(event.getMessage());
		new PlayerEvent(player,"chat",event);
		
		//event.getPlugin().getLogger().info("Player " + event.getPlayer().getName() + " has said something");
	}
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		new PlayerEvent(player,"join",null);
		
		//event.getPlugin().getLogger().info("Player " + event.getPlayer().getName() + " has said something");
	}
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		new PlayerEvent(player,"leave",null);
	}


}
