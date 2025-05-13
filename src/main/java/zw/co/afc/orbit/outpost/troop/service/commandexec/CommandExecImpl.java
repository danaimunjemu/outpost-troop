package zw.co.afc.orbit.outpost.troop.service.commandexec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Slf4j
@Service
public class CommandExecImpl implements CommandExec {

    @Override
    public String runLocalCommand(String command) throws IOException {
        Process process = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder output = new StringBuilder();

        try {
            // Create a process builder for the command
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", command); // Use bash to run the command
            processBuilder.redirectErrorStream(true); // Merge error stream with input stream

            // Start the process
            process = processBuilder.start();

            // Get the input stream of the process
            inputStream = process.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read the output of the command
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the process to complete
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            throw new IOException("Command execution was interrupted", e);
        } finally {
            // Close resources
            if (reader != null) {
                reader.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (process != null) {
                process.destroy(); // Terminate the process
            }
        }

        return output.toString();
}

}
