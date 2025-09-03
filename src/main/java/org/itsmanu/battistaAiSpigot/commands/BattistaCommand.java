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

        // Verifica che il comando sia /battista
        if (!command.getName().equalsIgnoreCase("battista")) {
            return false;
        }

        // Se non ci sono argomenti, mostra l'help
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        // Gestisce il sottocomando
        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Sottocomando sconosciuto. Usa '/battista help' per vedere i comandi disponibili.");
                break;
        }

        return true;
    }

    /**
     * Gestisce il comando reload
     */
    private void handleReload(CommandSender sender) {
        // Controlla i permessi
        if (!sender.hasPermission("battista.reload")) {
            sender.sendMessage(ChatColor.RED + "Non hai il permesso per usare questo comando!");
            return;
        }

        try {
            // Ricarica la configurazione
            plugin.reloadConfig();

            // Messaggio di successo
            sender.sendMessage(ChatColor.GREEN + "Configurazione del plugin Battista ricaricata con successo!");

            // Log nel console
            logger.info("Configurazione ricaricata da " + sender.getName());

        } catch (Exception e) {
            // Gestisce eventuali errori
            sender.sendMessage(ChatColor.RED + "Errore durante il ricaricamento della configurazione!");
            logger.severe("Errore durante il reload: " + e.getMessage());
        }
    }

    /**
     * Mostra l'help dei comandi
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Comandi Battista ===");
        sender.sendMessage(ChatColor.YELLOW + "/battista reload" + ChatColor.WHITE + " - Ricarica la configurazione");
        sender.sendMessage(ChatColor.YELLOW + "/battista help" + ChatColor.WHITE + " - Mostra questo messaggio");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggerimenti per il primo argomento
            List<String> subcommands = Arrays.asList("reload", "help");

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    // Controlla i permessi per il tab completion
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
