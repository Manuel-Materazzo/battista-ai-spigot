package org.itsmanu.battistaAiSpigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.ChatUtil;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class BattistaCommand implements CommandExecutor, TabCompleter {
    private final Logger logger = BattistaAiSpigot.getInstance().getLogger();
    private final BattistaAiSpigot plugin = BattistaAiSpigot.getInstance();

    public BattistaCommand() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Verify that the command is /battista
        if (!command.getName().equalsIgnoreCase("battista")) {
            return false;
        }

        // If no arguments are provided, display the help message
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // Handle subcommands
        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                var message = ChatUtil.formatConfigMessage("messages.unknown_command", "Unknown subcommand.");
                sender.sendMessage(message);
                break;
        }

        return true;
    }

    /**
     * Handles the reload subcommand.
     *
     * @param sender The sender who executed the command.
     */
    private void handleReload(CommandSender sender) {
        // Check if the sender has the required permission
        if (!sender.hasPermission("battista.reload")) {
            var message = ChatUtil.formatConfigMessage("messages.no_permission", "You need battista.reload permission");
            sender.sendMessage(message);
            return;
        }

        try {
            // Reload the plugin configuration
            plugin.reloadConfig();
            HttpUtil.initializeHttpClient();

            // Send a success message to the sender
            var message = ChatUtil.formatConfigMessage("messages.realoaded", "Reloaded successfully");
            sender.sendMessage(message);

            // Log the reload action in the console
            logger.info("Battista Configuration reloaded by " + sender.getName());

        } catch (Exception e) {
            // Handle any errors during the reload process
            var message = ChatUtil.formatConfigMessage("messages.not_realoaded", "Error during reload");
            sender.sendMessage(message);
            logger.severe("Error during Battista reload: " + e.getMessage());
        }
    }

    /**
     * Displays the help message for the /battista command.
     *
     * @param sender The sender who executed the command.
     */
    private void sendHelp(CommandSender sender) {
        String commands = "Battista commands:\n /battista reload\n/battista help";
        var message = BattistaAiSpigot.getInstance().getConfig().getString("messages.help", commands);

        String[] lines = message.split("\n");

        // Send each line with color codes translated
        for (String line : lines) {
            var formattedLine = ChatUtil.formatMessage(line);
            sender.sendMessage(formattedLine);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggestions for the first argument
            List<String> subcommands = Arrays.asList("reload", "help");

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    // Check permissions for tab completion
                    if (subcommand.equals("reload") && !sender.hasPermission("battista.reload")) {
                        continue;
                    }
                    completions.add(subcommand);
                }
            }
        }

        return completions;
    }
}
