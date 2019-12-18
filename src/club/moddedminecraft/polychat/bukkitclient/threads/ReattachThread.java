package club.moddedminecraft.polychat.bukkitclient.threads;

import club.moddedminecraft.polychat.bukkitclient.EventListener;
import club.moddedminecraft.polychat.bukkitclient.BukkitClient;
import club.moddedminecraft.polychat.networking.io.MessageBus;
import club.moddedminecraft.polychat.networking.io.PlayerListMessage;
import club.moddedminecraft.polychat.networking.io.ServerInfoMessage;
import club.moddedminecraft.polychat.networking.io.ServerStatusMessage;


import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

public class ReattachThread extends HeartbeatThread {

    private boolean isConnected = true;

    public ReattachThread(int interval) {
        super(interval);
    }

    @Override
    protected void run() throws InterruptedException, IOException {
        try {

            if (BukkitClient.messageBus == null || (BukkitClient.messageBus.isSocketClosed())) {
                //Tells players ingame that the connection failed
                if (isConnected) {
                    isConnected = false; //TODO
                    BukkitClient.sendGameMessage("[PolyChat] Lost connection to main server, attempting reconnect...");
                }

                //Stops threads if they are still running
                if (BukkitClient.messageBus != null) BukkitClient.messageBus.stop();

                //Attempts to start the connection //TODO
                BukkitClient.messageBus = new MessageBus(new Socket(BukkitClient.properties.getProperty("address"), Integer.parseInt(BukkitClient.properties.getProperty("port"))), EventListener::handleMessage);
                BukkitClient.messageBus.start();

                //If the socket was reopened, wait 3 seconds to make sure sending online message works
                if (!BukkitClient.messageBus.isSocketClosed()) {
                    Thread.sleep(2000); //TODO
                    BukkitClient.sendGameMessage("[PolyChat] Connection re-established!");
                    sendServerOnline();
                    Thread.sleep(1000);
                    sendOnlinePlayers();
                    isConnected = true;
                }

            }
        } catch (UnknownHostException e) {
            System.out.println("Unknown host exception on reattach");
        } catch (IOException e) {
            System.out.println("IOException on reattach");
        }
    }

    public void sendServerOnline() { //TODO
        //Reports the server as starting
        ServerInfoMessage infoMessage = new ServerInfoMessage(BukkitClient.properties.getProperty("server_id", "DEFAULT_ID"),
        		BukkitClient.properties.getProperty("server_name", "DEFAULT_NAME"),
        		BukkitClient.properties.getProperty("server_address", "DEFAULT_ADDRESS"), BukkitClient.getMaxPlayers());
        BukkitClient.sendMessage(infoMessage);
        //Reports the server as online and ready to receive players
        //ServerStatusMessage statusMessage = new ServerStatusMessage(BukkitClient.properties.getProperty("server_id"),
                //ITextComponent.Serializer.componentToJson(BukkitClient.serverIdText), (short) 1);
        //BukkitClient.sendMessage(statusMessage);
    }


    //Sends a list of all online players silently for auto reconnect
    public void sendOnlinePlayers() { //TODO
        ArrayList<String> playerList = new ArrayList<>();
        playerList = BukkitClient.getOnlinePlayersNames();
        PlayerListMessage message = new PlayerListMessage(BukkitClient.properties.getProperty("server_id"), playerList);
        BukkitClient.sendMessage(message);
    }
}