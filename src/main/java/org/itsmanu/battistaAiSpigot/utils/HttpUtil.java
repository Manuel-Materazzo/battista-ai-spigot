package org.itsmanu.battistaAiSpigot.utils;

import com.google.gson.*;
import okhttp3.*;
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
        String endpointUrl = BattistaAiSpigot.getConfigs().getString("endpoint.answer-url", "http://localhost:8000/v2/answer");
        return coordinateRequest(question, endpointUrl);
    }

    /**
     * Retrieves a list of documents from the AI endpoint asynchronously.
     *
     * @return A CompletableFuture containing the list of documents as a JSON string.
     */
    public static CompletableFuture<String> getDocuments() {
        String endpointUrl = BattistaAiSpigot.getConfigs().getString("endpoint.list-url", "http://localhost:8000/v2/list_documents");
        return coordinateRequest("", endpointUrl);
    }

    /**
     * Coordinates the request to the specified URL with the given question asynchronously.
     *
     * @param question The question to send in the request.
     * @param url      The endpoint URL to send the request to.
     * @return A CompletableFuture containing the response from the server.
     */
    public static CompletableFuture<String> coordinateRequest(String question, String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            String jsonString = prepareJsonPayload(question);
            Request request = buildHttpRequest(url, jsonString);

            ChatUtil.sendDebug("Sending Battista HTTP request to: " + url);
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
     * Parses the HTTP response body and completes the future with the extracted response.
     * If the response body is null or empty, completes the future with an empty string.
     * If JSON parsing fails, completes the future with the original response body.
     * For any other unexpected errors, completes the future exceptionally.
     *
     * @param responseBody The HTTP response body to parse.
     * @param future       The CompletableFuture to complete with the parsed response.
     */
    private static void parseAndCompleteResponse(String responseBody, CompletableFuture<String> future) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            future.complete("");
            return;
        }

        try {
            JsonElement jsonElement = gson.fromJson(responseBody, JsonElement.class);
            String aiResponse = extractResponse(jsonElement, responseBody);
            future.complete(aiResponse);
        } catch (JsonSyntaxException e) {
            // If JSON parsing fails, complete with original response
            future.complete(responseBody);
        } catch (Exception e) {
            // For any other unexpected errors, complete exceptionally
            future.completeExceptionally(e);
        }
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
    private static String extractResponse(JsonElement jsonElement, String fallback) {
        if (jsonElement == null) {
            return fallback;
        }

        if (jsonElement.isJsonObject()) {
            var response = jsonElement.getAsJsonObject();
            return response.has("response") ? response.get("response").getAsString() : fallback;
        } else if (jsonElement.isJsonArray()) {
            var responseArray = jsonElement.getAsJsonArray();
            StringBuilder result = new StringBuilder();
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
            return result.toString();
        } else {
            return fallback;
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
            requestBody.addProperty("metadata_filter", filter);
        }


        return gson.toJson(requestBody);
    }

}
