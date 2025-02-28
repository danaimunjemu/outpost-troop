package zw.co.afc.orbit.outpost.troop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zw.co.afc.orbit.outpost.troop.service.commandexec.CommandExec;
import zw.co.afc.orbit.outpost.troop.service.iface.ApplicationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final CommandExec commandExec;

    public List<String> getProcessId(String command) {
        List<String> processIds = new ArrayList<>();
        try {
            // Example command to get process IDs
            String output = commandExec.runLocalCommand(command);

            // Split the output by newlines to get individual process IDs
            if (output != null && !output.isEmpty()) {
                processIds = Arrays.asList(output.split("\n"));
            }

            // Log the process IDs
            log.info("Process IDs retrieved successfully");
        } catch (IOException e) {
            log.error("Failed to execute command", e);
        }

        return processIds;
    }

    public void startApplication(String command) {
        try {
            // Example command to get process IDs
            String output = commandExec.runLocalCommand(command);

            // Log the process IDs
            log.info("Command execution complete");
        } catch (IOException e) {
            log.error("Failed to execute command", e);
        }

        return;
    }



}
