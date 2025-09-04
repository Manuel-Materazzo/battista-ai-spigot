package org.itsmanu.battistaAiSpigot;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.itsmanu.battistaAiSpigot.commands.AskCommand;
import org.itsmanu.battistaAiSpigot.commands.BattistaCommand;
import org.itsmanu.battistaAiSpigot.listeners.ChatListener;

public final class BattistaAiSpigot extends JavaPlugin {

    private static BattistaAiSpigot instance;

    @Override
    public void onEnable() {
        // Set the instance of the plugin
        instance = this;

        // Save the default configuration if it doesn't exist
        saveDefaultConfig();

        // Register commands
        registerCommands();

        // Register event listeners
        registerEvents();

        getLogger().info("Battista successfully enabled!");
        getLogger().info("Configured Battista backend endpoint: " + getConfig().getString("endpoint.url"));

        // Log active modes
        if (getConfig().getBoolean("chat.auto_detect_questions")) {
            getLogger().info("Battista Automatic question detection: ENABLED");
        }
        getLogger().info("Battista Active chat tag: " + getConfig().getString("chat.tag"));
    }

    @Override
    public void onDisable() {
        getLogger().info("Battista successfully disabled!");
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        // Register the /ask command
        getCommand("ask").setExecutor(new AskCommand());
        getCommand("battista").setExecutor(new BattistaCommand());
        getCommand("battista").setTabCompleter(new BattistaCommand());

        getLogger().info("Battista Commands successfully registered!");
    }

    /**
     * Registers all event listeners for the plugin.
     */
    private void registerEvents() {
        // Register the listener for chat events
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        getLogger().info("Battista Event listeners successfully registered!");
    }

    /**
     * Retrieves the instance of the plugin.
     *
     * @return The current instance of the plugin.
     */
    public static BattistaAiSpigot getInstance() {
        return instance;
    }

    /**
     * Retrieves the plugin's configuration file.
     *
     * @return The FileConfiguration object containing the plugin's settings.
     */
    public static FileConfiguration getConfigs() {
        return instance.getConfig();
    }
}
