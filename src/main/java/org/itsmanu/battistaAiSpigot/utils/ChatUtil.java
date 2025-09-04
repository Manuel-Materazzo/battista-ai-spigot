package org.itsmanu.battistaAiSpigot.utils;

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

public class ChatUtil {

    /**
     * Retrieves a message from the configuration and formats it with the configured prefix and color codes.
     *
     * @param path The path to the message in the configuration.
     * @param def The default message to use if the path is not found.
     * @return The formatted message with the prefix and color codes.
     */
    public static Component formatConfigMessage(String path, String def) {
        var message = BattistaAiSpigot.getInstance().getConfig().getString(path, def);
        return formatMessage(message);
    }

    /**
     * Formats the chat message with the configured prefix and color codes.
     *
     * @param message The raw message.
     * @return The formatted message with the prefix.
     */
    public static Component formatMessage(String message) {
        String prefix = BattistaAiSpigot.getInstance().getConfig().getString("chat.response_prefix");
        return new MineDown(prefix + message).toComponent();
    }

    /**
     * Sends a debug message to the console if debug mode is enabled in the configuration.
     *
     * @param message The debug message to be sent.
     */
    public static void sendDebug(String message) {
        message = formatMessage(message).toString();
        if (BattistaAiSpigot.getInstance().getConfig().getBoolean("debug", false)) {
            BattistaAiSpigot.getInstance().getLogger().info(message);
        }
    }

}
