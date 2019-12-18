package club.moddedminecraft.polychat.bukkitclient.threads;


import club.moddedminecraft.polychat.bukkitclient.BukkitClient;
import club.moddedminecraft.polychat.networking.io.PlayerListMessage;

import java.util.ArrayList;
import java.util.Collections;

public class ActivePlayerThread extends HeartbeatThread {

    private final String serverID;

    public ActivePlayerThread(int interval, String serverID) {
        super(interval);
        this.serverID = serverID;
    }

    @Override
    protected void run() throws InterruptedException {
        ArrayList<String> onlinePlayers = getPlayers();
        PlayerListMessage message = new PlayerListMessage(serverID, onlinePlayers);
        BukkitClient.sendMessage(message);
    }

    private ArrayList<String> getPlayers() { //TODO
        ArrayList<String> playerList = new ArrayList<>();
        playerList = BukkitClient.getOnlinePlayersNames();
        return playerList;
    }

}