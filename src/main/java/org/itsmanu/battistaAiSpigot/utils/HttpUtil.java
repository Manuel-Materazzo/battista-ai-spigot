package org.itsmanu.battistaAiSpigot.utils;

import com.google.gson.*;
import okhttp3.*;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.dto.BackendResponse;
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

    private HttpUtil() {
    }

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
        String endpointUrl = BattistaAiSpigot.getConfigs().getString("endpoint.answer-url", "http://localhost:8000/v2/answer");
        String payload = prepareQuestionJsonPayload(question, true);
        return coordinateRequest(payload, endpointUrl).thenApply(BackendResponse::getMessage);
    }

    /**
     * Retrieves a list of documents from the AI endpoint asynchronously.
     *
     * @return A CompletableFuture containing the list of documents as a string.
     */
    public static CompletableFuture<String> getDocuments() {
        String endpointUrl = BattistaAiSpigot.getConfigs().getString("endpoint.list-url", "http://localhost:8000/v2/list_documents");
        String payload = prepareQuestionJsonPayload("", true);
        return coordinateRequest(payload, endpointUrl).thenApply(BackendResponse::getMessage);
    }

    /**
     * Moderate a message to assess its toxicity.
     *
     * @param message The message to send for moderation.
     * @return A CompletableFuture containing the moderator's response.
     */
    public static CompletableFuture<BackendResponse> askModerator(String message) {
        String endpointUrl = BattistaAiSpigot.getConfigs().getString("endpoint.moderate-url", "http://localhost:8000/v1/moderate");
        String payload = prepareQuestionJsonPayload(message, false);
        return coordinateRequest(payload, endpointUrl);
    }

    /**
     * Coordinates the request to the specified URL with the given question asynchronously.
     *
     * @param payload The payload to send in the request.
     * @param url     The endpoint URL to send the request to.
     * @return A CompletableFuture containing the response from the server.
     */
    private static CompletableFuture<BackendResponse> coordinateRequest(String payload, String url) {
        CompletableFuture<BackendResponse> future = new CompletableFuture<>();

        try {
            Request request = buildHttpRequest(url, payload);

            ChatUtil.sendDebug("Sending Battista HTTP request to: " + url);
            ChatUtil.sendDebug("Battista Payload: " + payload);

            executeHttpRequest(request, future);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error preparing the Battista HTTP request", e);
            String message = BattistaAiSpigot.getConfigs().getString("messages.internal_error", "Internal Error");
            future.complete(new BackendResponse(message));
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
        String apiKey = BattistaAiSpigot.getConfigs().getString("endpoint.api-key", "your-api-key");
        return new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonPayload, JSON))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", apiKey)
                .build();
    }

    /**
     * Executes an HTTP request asynchronously and handles the response or failure.
     *
     * @param request The HTTP request to execute.
     * @param future  The CompletableFuture to complete with the response or error message.
     */
    private static void executeHttpRequest(Request request, CompletableFuture<BackendResponse> future) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.log(Level.WARNING, "Battista HTTP request failed: " + e.getMessage(), e);
                String message = BattistaAiSpigot.getConfigs().getString("messages.cant_process", "Can't process request");
                future.complete(new BackendResponse(message));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        logger.warning("Invalid Battista HTTP response. Status code: " + response.code());
                        String message = BattistaAiSpigot.getConfigs().getString("messages.cant_process", "Service unavailable, Error: ");
                        future.complete(new BackendResponse(message + response.code()));
                        return;
                    }

                    String responseBody = response.body().string();
                    ChatUtil.sendDebug("Battista HTTP response received: " + responseBody);

                    if (responseBody == null || responseBody.trim().isEmpty()) {
                        future.complete(new BackendResponse(""));
                        return;
                    }

                    try {
                        JsonElement jsonElement = gson.fromJson(responseBody, JsonElement.class);
                        BackendResponse aiResponse = extractResponse(jsonElement, responseBody);
                        future.complete(aiResponse);
                    } catch (JsonSyntaxException e) {
                        // If JSON parsing fails, complete with original response
                        future.complete(new BackendResponse(responseBody));
                    } catch (Exception e) {
                        // For any other unexpected errors, complete exceptionally
                        future.completeExceptionally(e);
                    }
                }
            }
        });
    }

    /**
     * Extracts the AI's response from a JSON element.
     * If the element is a JSON object, it looks for a "response" field.
     * If the element is a JSON array, it concatenates all "path" fields from the objects in the array.
     * If the element is neither, it returns the fallback string.
     *
     * @param jsonElement The JSON element to extract the response from.
     * @param fallback    The fallback string to return if extraction fails.
     * @return The extracted response or the fallback string.
     */
    private static BackendResponse extractResponse(JsonElement jsonElement, String fallback) {
        if (jsonElement == null) {
            return new BackendResponse(fallback);
        }

        if (jsonElement.isJsonObject()) {
            var response = jsonElement.getAsJsonObject();
            // get message or fallback
            String message = fallback;
            Boolean flag = null;
            if (response.has("response")) {
                message = response.get("response").getAsString();
            }
            if (response.has("toxic")) {
                flag = response.get("toxic").getAsBoolean();
            }
            return new BackendResponse(flag, message);
        } else if (jsonElement.isJsonArray()) {
            var responseArray = jsonElement.getAsJsonArray();
            StringBuilder result = new StringBuilder("Idexed documents:\n");
            for (JsonElement element : responseArray) {
                if (element.isJsonObject()) {
                    JsonObject document = element.getAsJsonObject();
                    if (document.has("path")) {
                        JsonElement pathElement = document.get("path");
                        if (!pathElement.isJsonNull()) {
                            result.append(pathElement.getAsString()).append("\n");
                        }
                    }
                }
            }
            return new BackendResponse(result.toString());
        } else {
            return new BackendResponse(fallback);
        }
    }

    /**
     * Prepares the JSON payload for the AI request.
     *
     * @param question The question to include in the payload.
     * @return A JSON string representing the request payload.
     */
    private static String prepareQuestionJsonPayload(String question, boolean filterEnabled) {
        // add user request
        JsonObject requestBody = new JsonObject();
        if (!question.isEmpty()) {
            requestBody.addProperty("prompt", question);
        }

        String folderFilter = BattistaAiSpigot.getConfigs().getString("source-filter.folder", "");
        // add trailing slash if missing
        if (filterEnabled && !folderFilter.isEmpty()) {
            if (!folderFilter.endsWith("/")) {
                folderFilter += "/";
            }
            // add folder filter
            String filter = String.format("contains(path, `%s`)", folderFilter);
            requestBody.addProperty("filters", filter);
            requestBody.addProperty("metadata_filter", filter);
        }

        return gson.toJson(requestBody);
    }

}
