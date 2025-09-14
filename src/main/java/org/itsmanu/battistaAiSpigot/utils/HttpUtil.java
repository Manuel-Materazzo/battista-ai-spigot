package org.itsmanu.battistaAiSpigot.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.jetbrains.annotations.NotNull;

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
    public static void initializeHttpClient() {
        int timeout = BattistaAiSpigot.getConfigs().getInt("endpoint.timeout", 30);

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
            String jsonString = prepareJsonPayload(question);

            // Prepare the HTTP request
            String endpointUrl = BattistaAiSpigot.getConfigs().getString("endpoint.url", "http://localhost:8000/v2/answer");

            Request request = new Request.Builder()
                    .url(endpointUrl)
                    .post(RequestBody.create(jsonString, JSON))
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Log the request if debug mode is enabled
            if (BattistaAiSpigot.getConfigs().getBoolean("debug", false)) {
                HttpUtil.logger.info("Sending Battista HTTP request to: " + endpointUrl);
                HttpUtil.logger.info("Battista Payload: " + jsonString);
            }

            // Execute the request asynchronously
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    HttpUtil.logger.log(Level.WARNING,
                            "Battista HTTP request failed: " + e.getMessage(), e);
                    var message = BattistaAiSpigot.getConfigs().getString("messages.cant_process", "Can't process request");
                    future.complete(message);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try (response) {
                        // on rest errors
                        if (!response.isSuccessful() || response.body() == null) {
                            HttpUtil.logger.warning(
                                    "Invalid Battista HTTP response. Status code: " + response.code());
                            String message = BattistaAiSpigot.getConfigs().getString("messages.cant_process", "Service unavailable, Error: ");
                            message += response.code();
                            future.complete(message);
                            return;
                        }

                        String responseBody = response.body().string();

                        // Log the response if debug mode is enabled
                        if (BattistaAiSpigot.getConfigs().getBoolean("debug", false)) {
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
                    }
                }
            });

        } catch (Exception e) {
            HttpUtil.logger.log(Level.SEVERE,
                    "Error preparing the Battista HTTP request", e);
            var message = BattistaAiSpigot.getConfigs().getString("messages.internal_error", "Internal Error");
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
        var processingMessage = ChatUtil.formatConfigMessage("messages.processing", "Processing question...");

        if (isPrivate) {
            player.sendMessage(processingMessage);
        } else {
            Bukkit.broadcast(processingMessage);
        }

        // Execute the request asynchronously
        askAI(question).thenAccept(response -> {
            // Switch back to the main thread to send the message
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                var formattedResponse = ChatUtil.formatMessage(response);

                if (isPrivate) {
                    player.sendMessage(formattedResponse);
                } else {
                    Bukkit.broadcast(formattedResponse);
                }
            });
        }).exceptionally(throwable -> {
            // Handle errors
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                var errorMessage = ChatUtil.formatMessage("An error occurred: " + throwable.getMessage());

                if (isPrivate) {
                    player.sendMessage(errorMessage);
                } else {
                    Bukkit.broadcast(errorMessage);
                }
            });

            HttpUtil.logger.log(Level.SEVERE, "Error during Battista AI request", throwable);
            return null;
        });
    }


    /**
     * Prepares the JSON payload for the AI request.
     *
     * @param question The question to include in the payload.
     * @return A JSON string representing the request payload.
     */
    private static String prepareJsonPayload(String question) {
        // add user request
        JsonObject requestBody = new JsonObject();
        if (!question.isEmpty()) {
            requestBody.addProperty("prompt", question);
        }

        String folderFilter = BattistaAiSpigot.getConfigs().getString("source-filter.folder", "");
        // add trailing slash if missing
        if (!folderFilter.isEmpty()) {
            if (!folderFilter.endsWith("/")) {
                folderFilter += "/";
            }
            // add folder filter
            String filter = String.format("contains(path, `%s`)", folderFilter);
            requestBody.addProperty("filters", filter);
        }


        return gson.toJson(requestBody);
    }

}
