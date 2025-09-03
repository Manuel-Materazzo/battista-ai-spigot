package org.itsmanu.battistaAiSpigot.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

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
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use '/battista help' to see available commands.");
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
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return;
        }

        try {
            // Reload the plugin configuration
            plugin.reloadConfig();

            // Send a success message to the sender
            sender.sendMessage(ChatColor.GREEN + "Battista plugin configuration successfully reloaded!");

            // Log the reload action in the console
            logger.info("Configuration reloaded by " + sender.getName());

        } catch (Exception e) {
            // Handle any errors during the reload process
            sender.sendMessage(ChatColor.RED + "An error occurred while reloading the configuration!");
            logger.severe("Error during reload: " + e.getMessage());
        }
    }

    /**
     * Displays the help message for the /battista command.
     *
     * @param sender The sender who executed the command.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Battista Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/battista reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/battista help" + ChatColor.WHITE + " - Display this help message");
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
