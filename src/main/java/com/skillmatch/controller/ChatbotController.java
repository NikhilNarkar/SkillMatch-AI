package com.skillmatch.controller;

import com.skillmatch.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatbotController {

    private final ChatService chatService;

    @PostMapping(path = "/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> getChatResponse(@RequestBody ChatRequest request) {
        String userInput = request == null ? null : request.getMessage();
        if (userInput == null || userInput.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ChatResponse.error("Input message is empty"));
        }
        ChatService.StructuredAnswer structured = chatService.askStructured(userInput);
        return ResponseEntity.ok(new ChatResponse(structured.asPlainText(), structured.asMap()));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String response;
        private Map<String, Object> structured;
        private String error;

        public ChatResponse(String response, Map<String, Object> structured) {
            this.response = response;
            this.structured = structured;
        }

        public static ChatResponse error(String message) {
            ChatResponse r = new ChatResponse("Error: " + message, Map.of());
            r.error = message;
            return r;
        }
    }
}



