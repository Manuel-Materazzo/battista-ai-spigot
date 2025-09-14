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
            String jsonString = prepareJsonPayload(question);
            String endpointUrl = BattistaAiSpigot.getConfigs().getString("endpoint.url", "http://localhost:8000/v2/answer");
            Request request = buildHttpRequest(endpointUrl, jsonString);

            ChatUtil.sendDebug("Sending Battista HTTP request to: " + endpointUrl);
            ChatUtil.sendDebug("Battista Payload: " + jsonString);

            executeHttpRequest(request, future);
        } catch (Exception e) {
            handleRequestPreparationError(e, future);
        }

        return future;
    }

    /**
     * Builds an HTTP request with the specified URL and JSON payload.
     *
     * @param url         The endpoint URL to send the request to.
     * @param jsonPayload The JSON payload to include in the request body.
     * @return A configured Request object ready to be executed.
     */
    private static Request buildHttpRequest(String url, String jsonPayload) {
        return new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonPayload, JSON))
                .addHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Executes an HTTP request asynchronously and handles the response or failure.
     *
     * @param request The HTTP request to execute.
     * @param future  The CompletableFuture to complete with the response or error message.
     */
    private static void executeHttpRequest(Request request, CompletableFuture<String> future) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handleHttpRequestFailure(e, future);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                handleHttpResponse(response, future);
            }
        });
    }

    /**
     * Handles HTTP request failures by logging the error and completing the future with an error message.
     *
     * @param e      The IOException that occurred during the HTTP request.
     * @param future The CompletableFuture to complete with an error message.
     */
    private static void handleHttpRequestFailure(IOException e, CompletableFuture<String> future) {
        logger.log(Level.WARNING, "Battista HTTP request failed: " + e.getMessage(), e);
        String message = BattistaAiSpigot.getConfigs().getString("messages.cant_process", "Can't process request");
        future.complete(message);
    }

    /**
     * Handles the HTTP response by processing the response body and completing the future with the result.
     * If the response is unsuccessful or the body is null, it delegates to handleUnsuccessfulResponse.
     * Otherwise, it parses the response body and completes the future with the AI's response.
     *
     * @param response The HTTP response to handle.
     * @param future   The CompletableFuture to complete with the response or error message.
     * @throws IOException If an I/O error occurs while reading the response body.
     */
    private static void handleHttpResponse(Response response, CompletableFuture<String> future) throws IOException {
        try (response) {
            if (!response.isSuccessful() || response.body() == null) {
                handleUnsuccessfulResponse(response, future);
                return;
            }

            String responseBody = response.body().string();
            ChatUtil.sendDebug("Battista HTTP response received: " + responseBody);

            parseAndCompleteResponse(responseBody, future);
        }
    }

    /**
     * Handles unsuccessful HTTP responses by logging the error and completing the future with an error message.
     * The error message includes the HTTP status code and a configurable message from the configuration.
     *
     * @param response The HTTP response that was unsuccessful.
     * @param future   The CompletableFuture to complete with an error message.
     */
    private static void handleUnsuccessfulResponse(Response response, CompletableFuture<String> future) {
        logger.warning("Invalid Battista HTTP response. Status code: " + response.code());
        String message = BattistaAiSpigot.getConfigs().getString("messages.cant_process", "Service unavailable, Error: ");
        future.complete(message + response.code());
    }

    /**
     * Parses the JSON response body and completes the future with the AI's response.
     * If the response contains a "response" field, it extracts that value; otherwise, it uses the raw response body.
     * If parsing fails, it falls back to using the raw response body.
     *
     * @param responseBody The raw response body from the HTTP request.
     * @param future       The CompletableFuture to complete with the parsed response or raw response body.
     */
    private static void parseAndCompleteResponse(String responseBody, CompletableFuture<String> future) {
        try {
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            String aiResponse = responseJson.has("response") ? responseJson.get("response").getAsString() : responseBody;
            future.complete(aiResponse);
        } catch (Exception e) {
            future.complete(responseBody);
        }
    }

    /**
     * Handles errors that occur during the preparation of the HTTP request.
     * Logs the error and completes the future with a configurable error message.
     *
     * @param e      The exception that occurred during request preparation.
     * @param future The CompletableFuture to complete with an error message.
     */
    private static void handleRequestPreparationError(Exception e, CompletableFuture<String> future) {
        logger.log(Level.SEVERE, "Error preparing the Battista HTTP request", e);
        String message = BattistaAiSpigot.getConfigs().getString("messages.internal_error", "Internal Error");
        future.complete(message);
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
