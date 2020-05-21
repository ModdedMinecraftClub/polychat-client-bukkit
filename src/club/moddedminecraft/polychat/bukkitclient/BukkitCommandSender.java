package club.moddedminecraft.polychat.bukkitclient;

import club.moddedminecraft.polychat.networking.io.CommandMessage;
import club.moddedminecraft.polychat.networking.io.CommandOutputMessage;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BukkitCommandSender implements CommandSender {

    private final CommandMessage commandMessage;
    private final Server server;
    private final ArrayList<String> output = new ArrayList<>();
    private final String color;

    public BukkitCommandSender(CommandMessage commandMessage, Server server, String color) {
        this.commandMessage = commandMessage;
        this.server = server;
        this.color = color;
    }

    public void sendOutput() {
        StringBuilder commandOutput = new StringBuilder();
        for (String output : this.output) {
            commandOutput.append(output).append("\n");
        }
        String serverID = commandMessage.getServerID();
        String channel = commandMessage.getChannel();
        CommandOutputMessage message = new CommandOutputMessage(serverID, "/" + getCommand(), commandOutput.toString(), channel, color);
        BukkitClient.sendMessage(message);
    }

    @Override
    public void sendMessage(String text) {
        this.output.add(text.replaceAll("§.", ""));
    }

    @Override
    public void sendMessage(String[] strings) {
        for (String text : strings) {
            this.output.add(text.replaceAll("§.", ""));
        }
    }

    public String getCommand() {
        String name = commandMessage.getName();
        String command = commandMessage.getCommand();
        ArrayList<String> args = commandMessage.getArgs();

        // Replaces default command with override if exists
        String override_lookup = "override_command_" + name;
        String override = BukkitClient.properties.getProperty(override_lookup, "");
        if (!override.isEmpty()) {
            command = override;
        }

        // get the last instance of every unique $(number)
        // ie. /ranks set $1 $2 $1 $3 returns $2 $1 $3
        Pattern pattern = Pattern.compile("(\\$\\d+)(?!.*\\1)");
        Matcher matcher = pattern.matcher(command);

        while (matcher.find()) {
            for (int i = 0; i <= matcher.groupCount(); i++) {
                String toBeReplaced = matcher.group(i);
                String replaceWith;
                int argNum = Integer.parseInt(toBeReplaced.substring(1));
                replaceWith = args.get(argNum - 1);
                command = command.replace(toBeReplaced, replaceWith);
            }
        }

        command = command.replace("$args", String.join(" ", args));
        return command;
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public String getName() {
        return "PolyChat";
    }

    @Override
    public boolean isPermissionSet(String s) {
        return true;
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return true;
    }

    @Override
    public boolean hasPermission(String s) {
        return true;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return true;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b, int i) {
        return null;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        return null;
    }

    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) {

    }

    @Override
    public void recalculatePermissions() {
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return null;
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean b) {

    }
}
