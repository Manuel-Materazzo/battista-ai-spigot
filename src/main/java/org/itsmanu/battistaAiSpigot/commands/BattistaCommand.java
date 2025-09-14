package org.itsmanu.battistaAiSpigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.ChatUtil;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class BattistaCommand implements CommandExecutor, TabCompleter {
    private final Logger logger = BattistaAiSpigot.getInstance().getLogger();
    private final BattistaAiSpigot plugin = BattistaAiSpigot.getInstance();

    public BattistaCommand() {
    }

    /**
     * Handles the execution of the /battista command.
     *
     * @param sender The sender who executed the command.
     * @param command The command that was executed.
     * @param label The alias of the command used.
     * @param args The arguments passed with the command.
     * @return true if the command was handled successfully, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {

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

            case "documents":
                sendDocumentList(sender);
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
        String commands = "Battista commands:\n /battista reload\n/battista help\n/battista documents";
        var message = BattistaAiSpigot.getConfigs().getString("messages.help", commands);

        String[] lines = message.split("\n");

        // Send each line with color codes translated
        for (String line : lines) {
            var formattedLine = ChatUtil.formatMessage(line);
            sender.sendMessage(formattedLine);
        }
    }

    private void sendDocumentList(CommandSender sender) {
        // Check if the sender has the required permission
        if (!sender.hasPermission("battista.documents")) {
            var message = ChatUtil.formatConfigMessage("messages.no_permission", "You need battista.documents permission");
            sender.sendMessage(message);
            return;
        }

        // Ensure the sender is a player
        if (!(sender instanceof Player player)) {
            var message = ChatUtil.formatConfigMessage("messages.only_players", "You're not a player!");
            sender.sendMessage(message);
            return;
        }
        var processingMessage = ChatUtil.formatConfigMessage("messages.requesting_documents", "Requesting documents...");

        // Request the list of documents to the remote backend
        // Note: this will automatically handle thread switching
        var request = HttpUtil.getDocuments();
        ChatUtil.sendAiAnswer(request, player, processingMessage, logger);

    }

    /**
     * Provides tab completion for the /battista command.
     *
     * @param sender The sender who is requesting tab completion.
     * @param command The command for which tab completion is being requested.
     * @param alias The alias of the command used.
     * @param args The arguments that have been provided so far.
     * @return A list of possible completions for the current argument.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggestions for the first argument
            List<String> subcommands = Arrays.asList("reload", "help", "documents");

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    // Check permissions for tab completion
                    if (subcommand.equals("reload") && !sender.hasPermission("battista.reload")) {
                        continue;
                    }
                    if (subcommand.equals("documents") && !sender.hasPermission("battista.documents")) {
                        continue;
                    }
                    completions.add(subcommand);
                }
            }
        }

        return completions;
    }
}
