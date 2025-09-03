package org.itsmanu.battistaAiSpigot.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.themoep.minedown.adventure.MineDown;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HttpUtil {

    private static final OkHttpClient httpClient;
    private static final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    static {
        // Configure the HTTP client with timeout settings
        int timeout = BattistaAiSpigot.getInstance().getConfig().getInt("endpoint.timeout", 30);

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sends a question to the AI endpoint asynchronously.
     * 
     * @param question The question to send.
     * @return A CompletableFuture containing the AI's response.
     */
    public static CompletableFuture<String> askAI(String question) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            // Prepare the JSON payload for the request
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("prompt", question);

            String jsonString = gson.toJson(requestBody);

            // Prepare the HTTP request
            String endpointUrl = BattistaAiSpigot.getInstance().getConfig().getString("endpoint.url");

            Request request = new Request.Builder()
                    .url(endpointUrl)
                    .post(RequestBody.create(jsonString, JSON))
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Log the request if debug mode is enabled
            if (BattistaAiSpigot.getInstance().getConfig().getBoolean("debug", false)) {
                BattistaAiSpigot.getInstance().getLogger().info("Sending HTTP request to: " + endpointUrl);
                BattistaAiSpigot.getInstance().getLogger().info("Payload: " + jsonString);
            }

            // Execute the request asynchronously
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    BattistaAiSpigot.getInstance().getLogger().log(Level.WARNING, 
                        "HTTP request failed: " + e.getMessage(), e);
                    future.complete("Sorry, I cannot process your request at the moment. Please try again later.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();

                            // Log the response if debug mode is enabled
                            if (BattistaAiSpigot.getInstance().getConfig().getBoolean("debug", false)) {
                                BattistaAiSpigot.getInstance().getLogger().info("HTTP response received: " + responseBody);
                            }

                            // Attempt to parse the response as JSON
                            try {
                                JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

                                // Look for common fields in the response
                                String aiResponse;
                                if (responseJson.has("answer")) {
                                    aiResponse = responseJson.get("answer").getAsString();
                                } else if (responseJson.has("response")) {
                                    aiResponse = responseJson.get("response").getAsString();
                                } else if (responseJson.has("text")) {
                                    aiResponse = responseJson.get("text").getAsString();
                                } else {
                                    // If no specific fields are found, use the raw response
                                    aiResponse = responseBody;
                                }

                                future.complete(aiResponse);

                            } catch (Exception e) {
                                // If JSON parsing fails, use the raw response
                                future.complete(responseBody);
                            }

                        } else {
                            BattistaAiSpigot.getInstance().getLogger().warning(
                                "Invalid HTTP response. Status code: " + response.code());
                            future.complete("Sorry, the AI service is currently unavailable (Error " + response.code() + ")");
                        }
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            BattistaAiSpigot.getInstance().getLogger().log(Level.SEVERE, 
                "Error preparing the HTTP request", e);
            future.complete("An internal error occurred. Please contact an administrator.");
        }

        return future;
    }

    /**
     * Sends a question to the AI and responds to the player.
     * This method automatically handles thread switching to avoid issues with the Bukkit API.
     * 
     * @param player    The player who asked the question.
     * @param question  The question to send.
     * @param isPrivate Whether the response should be private (only to the player) or public (in chat).
     */
    public static void askAIAndRespond(Player player, String question, boolean isPrivate) {
        // Display a processing message
        String processingMessage = colorize(BattistaAiSpigot.getInstance().getConfig().getString("chat.processing_message"));

        if (isPrivate) {
            player.sendMessage(processingMessage);
        } else {
            Bukkit.broadcastMessage(processingMessage);
        }

        // Execute the request asynchronously
        askAI(question).thenAccept(response -> {
            // Switch back to the main thread to send the message
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                String formattedResponse = formatResponse(response);

                if (isPrivate) {
                    var message = new MineDown(formattedResponse).toComponent();
                    player.sendMessage(message);
                } else {
                    Bukkit.broadcastMessage(formattedResponse);
                }
            });
        }).exceptionally(throwable -> {
            // Handle errors
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                String errorMessage = colorize(BattistaAiSpigot.getInstance().getConfig().getString("chat.response_prefix") + 
                    "An error occurred: " + throwable.getMessage());

                if (isPrivate) {
                    player.sendMessage(errorMessage);
                } else {
                    Bukkit.broadcastMessage(errorMessage);
                }
            });

            BattistaAiSpigot.getInstance().getLogger().log(Level.SEVERE, "Error during AI request", throwable);
            return null;
        });
    }

    /**
     * Formats the AI response with the configured prefix.
     *
     * @param response The raw response from the AI.
     * @return The formatted response with the prefix.
     */
    private static String formatResponse(String response) {
        String prefix = BattistaAiSpigot.getInstance().getConfig().getString("chat.response_prefix");
        return colorize(prefix + response);
    }

    /**
     * Converts color codes to the Minecraft format.
     *
     * @param message The message containing color codes.
     * @return The message with converted color codes.
     */
    private static String colorize(String message) {
        return message.replace("&", "§");
    }
}
