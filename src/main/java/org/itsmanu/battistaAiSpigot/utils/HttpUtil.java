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
import java.util.logging.Logger;

public class HttpUtil {

    private static OkHttpClient httpClient;
    private static final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final Logger logger = BattistaAiSpigot.getInstance().getLogger();

    static {
        initializeHttpClient();
    }

    /**
     * Initializes the HTTP client with timeout settings from the configuration.
     */
    public static void initializeHttpClient(){
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
                HttpUtil.logger.info("Sending Battista HTTP request to: " + endpointUrl);
                HttpUtil.logger.info("Battista Payload: " + jsonString);
            }

            // Execute the request asynchronously
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    HttpUtil.logger.log(Level.WARNING, 
                        "Battista HTTP request failed: " + e.getMessage(), e);
                    String message = ChatUtil.formatConfigMessage("messages.cant_process", "Can't process request");
                    future.complete(message);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();

                            // Log the response if debug mode is enabled
                            if (BattistaAiSpigot.getInstance().getConfig().getBoolean("debug", false)) {
                                HttpUtil.logger.info("Battista HTTP response received: " + responseBody);
                            }

                            // Attempt to parse the response as JSON
                            try {
                                JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

                                String aiResponse;
                                if (responseJson.has("response")) {
                                    aiResponse = responseJson.get("response").getAsString();
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
                            HttpUtil.logger.warning(
                                "Invalid Battista HTTP response. Status code: " + response.code());
                            String message = BattistaAiSpigot.getInstance().getConfig().getString("messages.cant_process", "Service unavailable, Error: ");
                            message += response.code();
                            future.complete(message);
                        }
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            HttpUtil.logger.log(Level.SEVERE, 
                "Error preparing the Battista HTTP request", e);
            String message = ChatUtil.formatConfigMessage("messages.internal_error", "Internal Error");
            future.complete(message);
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
        String processingMessage = ChatUtil.formatConfigMessage("messages.processing", "Processing question...");

        if (isPrivate) {
            player.sendMessage(processingMessage);
        } else {
            Bukkit.broadcastMessage(processingMessage);
        }

        // Execute the request asynchronously
         askAI(question).thenAccept(response -> {
            // Switch back to the main thread to send the message
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                String formattedResponse = ChatUtil.formatMessage(response);

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
                String errorMessage = ChatUtil.formatMessage("An error occurred: " + throwable.getMessage());

                if (isPrivate) {
                    player.sendMessage(errorMessage);
                } else {
                    Bukkit.broadcastMessage(errorMessage);
                }
            });

            HttpUtil.logger.log(Level.SEVERE, "Error during Battista AI request", throwable);
            return null;
        });
    }



}
