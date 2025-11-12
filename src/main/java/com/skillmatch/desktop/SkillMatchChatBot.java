package com.skillmatch.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Desktop Swing chatbot that talks to the Spring Boot backend:
 * POST http://localhost:8080/api/v1/chat/ask  with JSON { "prompt": "..." }
 */
public class SkillMatchChatBot extends JFrame {

    private final JTextArea chatHistory;
    private final JTextField userEntry;
    private final JButton sendButton;
    private final JButton clearButton;
    private final JButton exitButton;

    public SkillMatchChatBot() {
        setTitle("SkillMatch AI Assistant");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatHistory = new JTextArea();
        chatHistory.setEditable(false);
        chatHistory.setLineWrap(true);
        chatHistory.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatHistory);

        userEntry = new JTextField(60);
        sendButton = new JButton("Send");
        clearButton = new JButton("Clear");
        exitButton = new JButton("Exit");

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        inputPanel.add(userEntry);
        inputPanel.add(sendButton);
        inputPanel.add(clearButton);
        inputPanel.add(exitButton);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        clearButton.addActionListener(e -> clearChat());
        exitButton.addActionListener(e -> System.exit(0));
        userEntry.addActionListener(e -> sendMessage());
        userEntry.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    userEntry.setText(userEntry.getText() + System.lineSeparator());
                    e.consume();
                }
            }
        });

        showWelcomeMessage();
    }

    private void showWelcomeMessage() {
        chatHistory.append("Welcome to SkillMatch AI ChatBot\n");
        chatHistory.append("Personal AI assistant\n");
        chatHistory.append("How can I help you today?\n");
    }

    private void sendMessage() {
        String userInput = userEntry.getText().trim();
        if (userInput.isEmpty()) return;
        String timestamp = DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.now());
        chatHistory.append("You [" + timestamp + "]: " + userInput + "\n");

        setControlsEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private String response;

            @Override
            protected Void doInBackground() {
                response = callBackend(userInput);
                return null;
            }

            @Override
            protected void done() {
                String botTimestamp = DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.now());
                chatHistory.append("AI Assistant [" + botTimestamp + "]: " + response + "\n");
                setControlsEnabled(true);
                userEntry.setText("");
                userEntry.requestFocusInWindow();
            }
        };
        worker.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
        userEntry.setEnabled(enabled);
    }

    private void clearChat() {
        chatHistory.setText("");
        showWelcomeMessage();
    }

    private String callBackend(String prompt) {
        try {
            URL url = new URL("http://localhost:8080/api/v1/chat/ask");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{\"prompt\":\"" + escapeJson(prompt) + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                conn.disconnect();
                // Expecting JSON: {"response":"..."} — extract naive value
                return extractResponseField(sb.toString());
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String extractResponseField(String json) {
        // Very simple extraction to avoid adding JSON libs to desktop classpath
        // Looks for "response":"...".
        String key = "\"response\":\"";
        int i = json.indexOf(key);
        if (i >= 0) {
            int start = i + key.length();
            int end = json.indexOf("\"", start);
            if (end > start) {
                return json.substring(start, end).replace("\\n", "\n");
            }
        }
        return json;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SkillMatchChatBot().setVisible(true));
    }
}



