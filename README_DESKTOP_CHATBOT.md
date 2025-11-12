## Java Swing Desktop Chatbot (integrated with Spring Boot backend)

A simple Java Swing desktop client that talks to your Spring Boot API at `/api/v1/chat/ask`.

### Location
- Class: `src/main/java/com/skillmatch/desktop/SkillMatchChatBot.java`

### Prerequisites
- Start your Spring Boot server so the chat API is available:

```bash
mvn spring-boot:run
```

By default it runs on port `8080`. The Swing app calls `http://localhost:8080/api/v1/chat/ask`.

### Run the desktop app
From your IDE: run the `main` method in `SkillMatchChatBot`.

From command line (example using Maven target classes on classpath):
```bash
mvn -q -DskipTests package
java -cp target/classes;target/dependency/* com.skillmatch.desktop.SkillMatchChatBot
```

If you packaged a fat jar, you can also add `target/skillmatch-*.jar` to the classpath.

### Notes
- The Swing client does NOT embed the server; keep Spring Boot running.
- All model/API credentials stay on the backend; the desktop app never sees your external API key.
- The backend endpoint it calls is `POST /api/v1/chat/ask` with JSON body `{"prompt":"..."}`.


