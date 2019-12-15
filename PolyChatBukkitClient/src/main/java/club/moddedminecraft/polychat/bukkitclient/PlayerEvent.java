package club.moddedminecraft.polychat.bukkitclient;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerEvent {
    public PlayerEvent(Player player,String eventType, AsyncPlayerChatEvent chatEvent) {
    	String serverId = "[Test]: ";
    	if(eventType == "chat") {
    		System.out.println(serverId + player.getName()+" has said: " + chatEvent.getMessage());
    		return;
    	}
    	if(eventType == "join") {
    		System.out.println(serverId + player.getName()+" has joined the game");
    	}
    	if(eventType == "leave") {
    		System.out.println(serverId + player.getName()+" has left the game");
    	}
       
    }
}
