package com.skillmatch.controller;

import com.skillmatch.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.badRequest().body(new ChatResponse("Input message is empty"));
        }
        String apiResponse = chatService.ask(userInput);
        return ResponseEntity.ok(new ChatResponse(apiResponse));
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatResponse {
        private final String response;
    }
}



