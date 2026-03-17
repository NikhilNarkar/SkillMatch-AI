package com.skillmatch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
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
        StructuredAnswer structured = askStructured(prompt);
        return structured.asPlainText();
    }

    public StructuredAnswer askStructured(String prompt) {
        try {
            String structuredPrompt = buildStructuredPrompt(prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(buildRequestBody(structuredPrompt), headers);

            String lastError = null;

            // Try configured URL first
            String firstUrl = buildEndpointUrl();
            String result = tryRequest(firstUrl, entity);
            if (result != null) {
                if (looksLikeError(result)) lastError = result;
                else return parseStructuredAnswer(result);
            }

            // If the configured URL failed with 404, try common Gemini endpoints in order
            String[] fallbacks = new String[] {
                    // v1beta endpoints (recommended for API key usage)
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent",
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent",
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-latest:generateContent"
            };
            for (String fb : fallbacks) {
                String url = appendKeyIfNeeded(fb);
                result = tryRequest(url, entity);
                if (result != null) {
                    if (looksLikeError(result)) {
                        lastError = result;
                        continue;
                    }
                    return parseStructuredAnswer(result);
                }
            }

            // If nothing worked, return a helpful message
            if (lastError != null) {
                return StructuredAnswer.error(lastError);
            }
            return StructuredAnswer.error("No supported Gemini model endpoint responded successfully with your API key. " +
                    "Please verify model access for your key or provide a working chat.api.url.");
        } catch (Exception e) {
            return StructuredAnswer.error(e.getMessage());
        }
    }

    @SuppressWarnings("null")
    private String tryRequest(String url, HttpEntity<Object> entity) {
        try {
            String safeUrl = Objects.requireNonNull(url, "url");
            HttpEntity<Object> safeEntity = Objects.requireNonNull(entity, "entity");
            ResponseEntity<String> response = restTemplate().exchange(safeUrl, HttpMethod.POST, safeEntity, String.class);
            return extractTextFromResponse(response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
            // 404 - try next fallback, but preserve details for final error reporting
            String body = null;
            try {
                body = nf.getResponseBodyAsString();
            } catch (Exception ignore) {}
            if (body != null && !body.isBlank()) {
                return "Error: 404 Not Found - " + body;
            }
            return "Error: 404 Not Found";
        } catch (Exception ex) {
            // Other errors - propagate an error marker so caller can decide to fallback
            return "Error: " + ex.getMessage();
        }
    }

    private boolean looksLikeError(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.startsWith("Error:");
    }

    private String buildStructuredPrompt(String userPrompt) {
        String prompt = userPrompt == null ? "" : userPrompt.trim();
        return """
You are SkillMatch AI Assistant. Produce a structured, easy-to-understand answer.

Return ONLY valid JSON (no markdown, no code fences, no extra text) that matches this schema:
{
  "summary": string,
  "keyPoints": string[],
  "nextSteps": string[],
  "examples": string[],
  "warnings": string[],
  "followUpQuestions": string[],
  "rawText": string
}

Rules:
- Keep summary 1-2 sentences.
- Use short bullets (max ~15 words each) in arrays.
- If a section is not applicable, return an empty array.
- rawText should be a readable plain-text version of the answer.

User message:
""" + prompt;
    }

    private StructuredAnswer parseStructuredAnswer(String modelText) {
        if (modelText == null) return StructuredAnswer.fromRaw("");
        String trimmed = modelText.trim();
        // Some models wrap JSON in ``` or add text; attempt to extract the first JSON object.
        String jsonCandidate = extractFirstJsonObject(trimmed);
        StructuredAnswer parsed = tryParseStructuredJson(jsonCandidate);
        if (parsed != null) return parsed;
        return StructuredAnswer.fromRaw(trimmed);
    }

    private StructuredAnswer tryParseStructuredJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            if (!root.isObject()) return null;

            StructuredAnswer ans = new StructuredAnswer();
            ans.setSummary(textOrEmpty(root.get("summary")));
            ans.setKeyPoints(readStringArray(root.get("keyPoints")));
            ans.setNextSteps(readStringArray(root.get("nextSteps")));
            ans.setExamples(readStringArray(root.get("examples")));
            ans.setWarnings(readStringArray(root.get("warnings")));
            ans.setFollowUpQuestions(readStringArray(root.get("followUpQuestions")));
            ans.setRawText(textOrEmpty(root.get("rawText")));

            // If model omitted rawText, build one.
            if (ans.getRawText() == null || ans.getRawText().isBlank()) {
                ans.setRawText(ans.asPlainText());
            }
            return ans;
        } catch (Exception ignore) {
            return null;
        }
    }

    private String extractFirstJsonObject(String s) {
        if (s == null) return "";
        int start = s.indexOf('{');
        if (start < 0) return s;
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return s.substring(start, i + 1);
            }
        }
        return s.substring(start);
    }

    private String textOrEmpty(JsonNode n) {
        return (n != null && n.isTextual()) ? n.asText() : "";
    }

    private List<String> readStringArray(JsonNode n) {
        if (n == null || !n.isArray()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (JsonNode item : n) {
            if (item != null && item.isTextual()) {
                String v = item.asText().trim();
                if (!v.isEmpty()) out.add(v);
            }
        }
        return out;
    }

    private String buildEndpointUrl() {
        if (chatApiUrl == null || chatApiUrl.isBlank()) {
            return appendKeyIfNeeded("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
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

    public static class StructuredAnswer {
        private String summary;
        private List<String> keyPoints = Collections.emptyList();
        private List<String> nextSteps = Collections.emptyList();
        private List<String> examples = Collections.emptyList();
        private List<String> warnings = Collections.emptyList();
        private List<String> followUpQuestions = Collections.emptyList();
        private String rawText;
        private String error;

        public static StructuredAnswer fromRaw(String raw) {
            StructuredAnswer ans = new StructuredAnswer();
            String text = raw == null ? "" : raw.trim();
            ans.rawText = text;
            ans.summary = inferSummary(text);
            ans.keyPoints = inferBullets(text, 6);
            ans.nextSteps = Collections.emptyList();
            ans.examples = Collections.emptyList();
            ans.warnings = Collections.emptyList();
            ans.followUpQuestions = Collections.emptyList();
            return ans;
        }

        public static StructuredAnswer error(String message) {
            StructuredAnswer ans = new StructuredAnswer();
            ans.error = message == null ? "Unknown error" : message;
            ans.summary = "I couldn't generate a structured answer due to an error.";
            ans.rawText = "Error: " + ans.error;
            ans.keyPoints = List.of("Check your API key and model access", "Review server logs for details");
            return ans;
        }

        public String asPlainText() {
            if (rawText != null && !rawText.isBlank()) return rawText;
            StringBuilder sb = new StringBuilder();
            if (summary != null && !summary.isBlank()) sb.append(summary).append("\n");
            if (keyPoints != null && !keyPoints.isEmpty()) {
                sb.append("\nKey points:\n");
                for (String kp : keyPoints) sb.append("- ").append(kp).append("\n");
            }
            if (nextSteps != null && !nextSteps.isEmpty()) {
                sb.append("\nNext steps:\n");
                for (String st : nextSteps) sb.append("- ").append(st).append("\n");
            }
            return sb.toString().trim();
        }

        private static String inferSummary(String text) {
            if (text == null) return "";
            String t = text.replace("\r", " ").trim();
            if (t.isEmpty()) return "";
            int end = t.indexOf('.');
            if (end > 0 && end < 220) return t.substring(0, end + 1).trim();
            int nl = t.indexOf('\n');
            if (nl > 0 && nl < 220) return t.substring(0, nl).trim();
            return t.length() <= 220 ? t : t.substring(0, 220).trim();
        }

        private static List<String> inferBullets(String text, int max) {
            if (text == null || text.isBlank()) return Collections.emptyList();
            String[] lines = text.split("\\r?\\n");
            List<String> bullets = new ArrayList<>();
            for (String line : lines) {
                String l = line.trim();
                if (l.startsWith("- ") || l.startsWith("• ") || l.startsWith("* ")) {
                    String b = l.substring(2).trim();
                    if (!b.isEmpty()) bullets.add(b);
                }
                if (bullets.size() >= max) break;
            }
            return bullets.isEmpty() ? Collections.emptyList() : bullets;
        }

        // Getters/setters
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public List<String> getKeyPoints() { return keyPoints; }
        public void setKeyPoints(List<String> keyPoints) { this.keyPoints = keyPoints == null ? Collections.emptyList() : keyPoints; }
        public List<String> getNextSteps() { return nextSteps; }
        public void setNextSteps(List<String> nextSteps) { this.nextSteps = nextSteps == null ? Collections.emptyList() : nextSteps; }
        public List<String> getExamples() { return examples; }
        public void setExamples(List<String> examples) { this.examples = examples == null ? Collections.emptyList() : examples; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings == null ? Collections.emptyList() : warnings; }
        public List<String> getFollowUpQuestions() { return followUpQuestions; }
        public void setFollowUpQuestions(List<String> followUpQuestions) { this.followUpQuestions = followUpQuestions == null ? Collections.emptyList() : followUpQuestions; }
        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public Map<String, Object> asMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("summary", summary == null ? "" : summary);
            m.put("keyPoints", keyPoints == null ? Collections.emptyList() : keyPoints);
            m.put("nextSteps", nextSteps == null ? Collections.emptyList() : nextSteps);
            m.put("examples", examples == null ? Collections.emptyList() : examples);
            m.put("warnings", warnings == null ? Collections.emptyList() : warnings);
            m.put("followUpQuestions", followUpQuestions == null ? Collections.emptyList() : followUpQuestions);
            m.put("rawText", rawText == null ? "" : rawText);
            if (error != null && !error.isBlank()) m.put("error", error);
            return m;
        }
    }
}


