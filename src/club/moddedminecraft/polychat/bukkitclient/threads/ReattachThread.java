package club.moddedminecraft.polychat.bukkitclient.threads;

import club.moddedminecraft.polychat.bukkitclient.BukkitClient;
import club.moddedminecraft.polychat.bukkitclient.EventListener;
import club.moddedminecraft.polychat.networking.io.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import static club.moddedminecraft.polychat.bukkitclient.BukkitClient.*;

public class ReattachThread extends HeartbeatThread {

    private boolean isConnected = true;

    public ReattachThread(int interval) {
        super(interval);
    }

    @Override
    protected void run() throws InterruptedException, IOException {
        try {
            if (!reattachKill) {
                if (BukkitClient.messageBus == null || (BukkitClient.messageBus.isSocketClosed())) {

                    System.out.println("Firing reattach");

                    //Tells players ingame that the connection failed
                    if (isConnected) {
                        isConnected = false;
                        BukkitClient.sendGameMessage("[PolyChat] Lost connection to main server, attempting reconnect...");
                    }

                    //Stops threads if they are still running
                    if (BukkitClient.messageBus != null) BukkitClient.messageBus.stop();

                    //Attempts to start the connection
                    BukkitClient.messageBus = new MessageBus(new Socket(BukkitClient.properties.getProperty("address"), Integer.parseInt(BukkitClient.properties.getProperty("port"))), new ReceiverCallback() {
                        @Override
                        public void receive(AbstractMessage abstractMessage) {
                            EventListener.handleMessage(abstractMessage);
                        }
                    });
                    BukkitClient.messageBus.start();

                    //If the socket was reopened, wait 3 seconds to make sure sending online message works
                    if (!BukkitClient.messageBus.isSocketClosed()) {
                        Thread.sleep(2000);
                        BukkitClient.sendGameMessage("[PolyChat] Connection re-established!");
                        sendServerOnline();
                        Thread.sleep(1000);
                        sendOnlinePlayers();
                        isConnected = true;
                    }
                }
            }

        } catch (UnknownHostException e) {
            System.out.println("Unknown host exception on reattach");
        } catch (IOException e) {
            System.out.println("IOException on reattach");
        }
    }

    public void sendServerOnline() {
        //Reports the server as starting
        ServerInfoMessage infoMessage = new ServerInfoMessage(
                id,
                BukkitClient.properties.getProperty("server_name", "DEFAULT_NAME"),
                BukkitClient.properties.getProperty("server_address", "DEFAULT_ADDRESS"), BukkitClient.getMaxPlayers());
        BukkitClient.sendMessage(infoMessage);
        //Reports the server as online and ready to receive players
        String id = BukkitClient.idFormatted;
        ServerStatusMessage statusMessage = new ServerStatusMessage(BukkitClient.id, idFormatted, (short) 1);
        BukkitClient.sendMessage(statusMessage);
    }


    //Sends a list of all online players silently for auto reconnect
    public void sendOnlinePlayers() {
        ArrayList<String> playerList;
        playerList = BukkitClient.getOnlinePlayersNames();
        PlayerListMessage message = new PlayerListMessage(id, playerList);
        BukkitClient.sendMessage(message);
    }
}