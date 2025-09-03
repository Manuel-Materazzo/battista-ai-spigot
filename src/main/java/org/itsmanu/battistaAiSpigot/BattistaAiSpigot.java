package org.itsmanu.battistaAiSpigot;

import org.bukkit.plugin.java.JavaPlugin;
import org.itsmanu.battistaAiSpigot.commands.AskCommand;
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

        getLogger().info("BattistaAiSpigot successfully enabled!");
        getLogger().info("Configured endpoint: " + getConfig().getString("endpoint.url"));

        // Log active modes
        if (getConfig().getBoolean("chat.auto_detect_questions")) {
            getLogger().info("Automatic question detection: ENABLED");
        }
        getLogger().info("Active chat tag: " + getConfig().getString("chat.tag"));
    }

    @Override
    public void onDisable() {
        getLogger().info("BattistaAiSpigot successfully disabled!");
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        // Register the /ask command
        getCommand("ask").setExecutor(new AskCommand());

        getLogger().info("Commands successfully registered!");
    }

    /**
     * Registers all event listeners for the plugin.
     */
    private void registerEvents() {
        // Register the listener for chat events
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        getLogger().info("Event listeners successfully registered!");
    }

    /**
     * Reloads the plugin configuration.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        getLogger().info("Configuration successfully reloaded!");
    }

    /**
     * Retrieves the instance of the plugin.
     *
     * @return The current instance of the plugin.
     */
    public static BattistaAiSpigot getInstance() {
        return instance;
    }
}
