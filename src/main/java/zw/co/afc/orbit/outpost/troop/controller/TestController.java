package zw.co.afc.orbit.outpost.troop.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.web.bind.annotation.*;
import zw.co.afc.orbit.outpost.troop.dto.request.ServerDetails;
import zw.co.afc.orbit.outpost.troop.model.LogEntry;
import zw.co.afc.orbit.outpost.troop.service.LogWebSocketHandler;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    private Tailer tailer;
    private final LogWebSocketHandler webSocketHandler = new LogWebSocketHandler();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(?s)\\[(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\]\\s*" +
                    "(?<logLevel>\\w+)\\s*" +
                    "\\[(?<serviceName>[^\\]]+),\\s*traceId=(?<traceId>[^\\s,]+),\\s*spanId=(?<spanId>[^\\s,]+)\\] - (?<message>.+)"
    );

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\]");
    private StringBuilder logBuffer = new StringBuilder(); // Buffer to hold multi-line logs

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/watch-log")
    public String startFileWatcher() {
        String logFilePath = "/Users/danaimunjemu/Downloads/outpost-application/mta-monitor/logs/mta-monitor.log";

        if (tailer != null) {
            return "Log watcher is already running!";
        }

        log.warn("Started watching log file: {}", logFilePath);

//        You can stop undo

        TailerListenerAdapter listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                String cleanLine = removeAnsiCodes(line);
                Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(cleanLine);

                if (timestampMatcher.find()) {
                    // If we have a buffered log entry, process and send it as JSON
                    if (logBuffer.length() > 0) {
                        sendJsonLog(logBuffer.toString().trim());
                        logBuffer.setLength(0); // Clear the buffer
                    }
                    // Start buffering the new log entry
                    logBuffer.append(cleanLine);
                } else {
                    // Append to current log entry if it's a continuation
                    if (logBuffer.length() > 0) {
                        logBuffer.append(" ").append(cleanLine);
                    }
                }
            }

            @Override
            public void endOfFileReached() {
                if (logBuffer.length() > 0) {
                    sendJsonLog(logBuffer.toString().trim()); // Ensure proper formatting
                    logBuffer.setLength(0);
                }
            }

            private void sendJsonLog(String logEntry) {
                Matcher logMatcher = LOG_PATTERN.matcher(logEntry);
                if (logMatcher.find()) {
                    Map<String, String> logJson = new HashMap<>();
                    logJson.put("timestamp", logMatcher.group("timestamp"));
                    logJson.put("logLevel", logMatcher.group("logLevel"));
                    logJson.put("serviceName", logMatcher.group("serviceName"));
                    logJson.put("traceId", logMatcher.group("traceId"));
                    logJson.put("spanId", logMatcher.group("spanId"));
                    logJson.put("message", logMatcher.group("message"));

                    try {
                        String json = objectMapper.writeValueAsString(logJson);
                        webSocketHandler.sendLogUpdate(json);
                    } catch (JsonProcessingException e) {
                        System.err.println("Failed to convert log entry to JSON: " + e.getMessage());
                    }
                } else {
                    System.err.println("Log entry did not match expected format: " + logEntry);
                }
            }
        };

        tailer = new Tailer(new File(logFilePath), listener, 1000, true);
        new Thread(tailer).start();

        return "Log watching started...";
    }

    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

    public static String removeAnsiCodes(String input) {
        if (input == null) {
            return ""; // or you could return null, depending on your use case
        }
        return ANSI_PATTERN.matcher(input).replaceAll("");
    }

    @GetMapping("/stop-watch")
    public String stopFileWatcher() {
        if (tailer != null) {
            tailer.stop();
            tailer = null;
            return "Log watcher stopped!";
        }
        return "Log watcher was not running.";
    }


    @PostMapping("/string")
    public String runLocalCommand(@RequestBody ServerDetails serverDetails) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", serverDetails.command());
        processBuilder.redirectErrorStream(true); // Merge stdout and stderr
        Process process = processBuilder.start();

        // Read the process output
        try (InputStream inputStream = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String output = reader.lines().collect(Collectors.joining("\n")); // Collect output as String
            log.info("Command Output:\n{}", output); // Properly log the output
            return output;
        } finally {
            process.destroy(); // Ensure the process is terminated
        }
    }

    @PostMapping
    public void runLocalCommand(HttpServletResponse response, @RequestBody ServerDetails serverDetails) throws IOException, IOException {
        Process process = null;
        InputStream inputStream = null;
        InputStream errorStream = null;

        try {
            // Create a process builder for the command
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", serverDetails.command()); // Use bash to run the command
            processBuilder.redirectErrorStream(true); // Merge error stream with input stream

            // Start the process
            process = processBuilder.start();

            // Get the input stream of the process
            inputStream = process.getInputStream();

            // Set up the response to stream data
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "inline");

            // Stream data directly to the client
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, read);
            }
            response.flushBuffer();
        } finally {
            // Close resources
            if (inputStream != null) {
                inputStream.close();
            }
            if (process != null) {
                process.destroy(); // Terminate the process
            }
        }
    }
}
