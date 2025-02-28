package zw.co.afc.orbit.outpost.troop.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.web.bind.annotation.*;
import zw.co.afc.orbit.outpost.troop.dto.request.ServerDetails;

import java.io.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    private Tailer tailer;

    @GetMapping("/watch-log")
    public String startFileWatcher() {
        String logFilePath = "/Users/danaimunjemu/Downloads/outpost-application/mta-monitor/logs/mta-monitor.log";  // Specify your log file

        if (tailer != null) {
            return "Log watcher is already running!";
        }

        log.warn("Started watching log file: {}", logFilePath);

        // TailerListener to handle new log lines
        TailerListenerAdapter listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                System.out.println(line);  // Print new log lines
            }
        };

        // Start tailing the log file
        tailer = new Tailer(new File(logFilePath), listener, 1000, true);
        new Thread(tailer).start();

        return "Log watching started...";
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
