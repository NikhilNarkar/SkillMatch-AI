package com.skillmatch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${chat.api.url:https://api.yourgeminiendpoint.com/v1/generate}")
    private String chatApiUrl;

    @Value("${chat.api.key:}")
    private String chatApiKey;

    private RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String ask(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(buildRequestBody(prompt), headers);

            // Try configured URL first
            String firstUrl = buildEndpointUrl();
            String result = tryRequest(firstUrl, entity);
            if (result != null) return result;

            // If the configured URL failed with 404, try common Gemini endpoints in order
            String[] fallbacks = new String[] {
                    // 8B variants are commonly available
                    "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-8b-latest:generateContent",
                    "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-8b:generateContent",
                    "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent",
                    "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-latest:generateContent",
                    "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent",
                    "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent"
            };
            for (String fb : fallbacks) {
                String url = appendKeyIfNeeded(fb);
                result = tryRequest(url, entity);
                if (result != null) return result;
            }

            // If nothing worked, return a helpful message
            return "Error: No supported Gemini model endpoint responded successfully with your API key. " +
                    "Please verify model access for your key or provide a working chat.api.url.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String tryRequest(String url, HttpEntity<Object> entity) {
        try {
            ResponseEntity<String> response = restTemplate().exchange(url, HttpMethod.POST, entity, String.class);
            return extractTextFromResponse(response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
            // 404 - try next fallback
            return null;
        } catch (Exception ex) {
            // Other errors - return message immediately
            return "Error: " + ex.getMessage();
        }
    }

    private String buildEndpointUrl() {
        if (chatApiUrl == null || chatApiUrl.isBlank()) {
            return appendKeyIfNeeded("https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent");
        }
        String url = chatApiUrl.trim();
        if (url.contains("{key}")) {
            return url.replace("{key}", chatApiKey == null ? "" : chatApiKey);
        }
        return appendKeyIfNeeded(url);
    }

    private String appendKeyIfNeeded(String url) {
        boolean looksLikeGemini = url.contains("generativelanguage.googleapis.com");
        boolean hasQuery = url.contains("?");
        boolean hasKeyParam = url.contains("key=");
        if (looksLikeGemini && !hasKeyParam && chatApiKey != null && !chatApiKey.isBlank()) {
            return url + (hasQuery ? "&" : "?") + "key=" + chatApiKey;
        }
        return url;
    }

    private Object buildRequestBody(String prompt) {
        // If the URL looks like the generic placeholder, use the simple body {prompt: ...}
        boolean looksLikeGeneric = chatApiUrl == null || chatApiUrl.contains("yourgeminiendpoint.com");
        boolean looksLikeGemini = chatApiUrl != null && chatApiUrl.contains("generativelanguage.googleapis.com");

        if (looksLikeGemini || looksLikeGeneric) {
            // Gemini REST format
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            Map<String, Object> content = new HashMap<>();
            content.put("role", "user");
            content.put("parts", new Object[]{ part });
            Map<String, Object> body = new HashMap<>();
            body.put("contents", new Object[]{ content });
            return body;
        }
        // Fallback simple format
        return Map.of("prompt", prompt);
    }

    private String extractTextFromResponse(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            // Try Gemini structure
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode first = candidates.get(0);
                JsonNode textNode = first.path("content").path("parts");
                if (textNode.isArray() && textNode.size() > 0) {
                    JsonNode txt = textNode.get(0).path("text");
                    if (txt.isTextual()) return txt.asText();
                }
            }
            // Otherwise just return raw
            return body;
        } catch (Exception e) {
            return body;
        }
    }
}


