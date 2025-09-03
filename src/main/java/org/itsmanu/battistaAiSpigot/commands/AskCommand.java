package org.itsmanu.battistaAiSpigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;

/**
 * Handles the /ask command to send questions to the AI.
 */
public class AskCommand implements CommandExecutor {

    private final BattistaAiSpigot plugin;

    public AskCommand(BattistaAiSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the /ask command.
     *
     * @param sender  The entity that executed the command.
     * @param command The command being executed.
     * @param label   The alias used for the command.
     * @param args    The arguments provided with the command.
     * @return true if the command was successfully executed, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if arguments are provided
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /ask <question>");
            sender.sendMessage("§eExample: /ask How do I craft a diamond sword?");
            return true;
        }

        // Ensure the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Check if the player has the required permission
        if (!player.hasPermission("aihelper.ask")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        // Combine all arguments into a single question
        StringBuilder questionBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            questionBuilder.append(args[i]);
            if (i < args.length - 1) {
                questionBuilder.append(" ");
            }
        }

        String question = questionBuilder.toString().trim();

        // Validate the question
        if (question.isEmpty()) {
            player.sendMessage("§cThe question cannot be empty!");
            return true;
        }

        if (question.length() < 3) {
            player.sendMessage("§cThe question is too short! Please write at least 3 characters.");
            return true;
        }

        if (question.length() > 500) {
            player.sendMessage("§cThe question is too long! Maximum 500 characters allowed.");
            return true;
        }

        // Log the question if debug mode is enabled
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Command /ask executed by " + player.getName() + ": " + question);
        }

        // Send the question to the AI - private response (only to the player who executed the command)
        HttpUtil.askAIAndRespond(player, question, true);

        return true;
    }
}
