package org.itsmanu.battistaAiSpigot.utils;

import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

public class ChatUtil {

    /**
     * Retrieves a message from the configuration and formats it with the configured prefix and color codes.
     *
     * @param path The path to the message in the configuration.
     * @param def The default message to use if the path is not found.
     * @return The formatted message with the prefix and color codes.
     */
    public static String formatConfigMessage(String path, String def) {
        var message = BattistaAiSpigot.getInstance().getConfig().getString(path, def);
        return formatMessage(message);
    }

    /**
     * Formats the chat message with the configured prefix and color codes.
     *
     * @param message The raw message.
     * @return The formatted message with the prefix.
     */
    public static String formatMessage(String message) {
        String prefix = BattistaAiSpigot.getInstance().getConfig().getString("chat.response_prefix");
        return colorize(prefix + message);
    }

    /**
     * Sends a debug message to the console if debug mode is enabled in the configuration.
     *
     * @param message The debug message to be sent.
     */
    public static void sendDebug(String message) {
        message = formatMessage(message);
        if (BattistaAiSpigot.getInstance().getConfig().getBoolean("debug", false)) {
            BattistaAiSpigot.getInstance().getLogger().info(message);
        }
    }

    /**
     * Converts color codes to the Minecraft format.
     *
     * @param message The message containing color codes.
     * @return The message with converted color codes.
     */
    public static String colorize(String message) {
        return message.replace("&", "§");
    }

}
