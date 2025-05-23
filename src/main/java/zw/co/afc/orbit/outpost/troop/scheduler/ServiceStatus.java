package zw.co.afc.orbit.outpost.troop.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zw.co.afc.orbit.outpost.troop.dto.response.DiskInfo;
import zw.co.afc.orbit.outpost.troop.dto.response.DiskMetrics;
import zw.co.afc.orbit.outpost.troop.enums.IncidentStatus;
import zw.co.afc.orbit.outpost.troop.model.Alert;
import zw.co.afc.orbit.outpost.troop.model.Application;
import zw.co.afc.orbit.outpost.troop.model.Garrison;
import zw.co.afc.orbit.outpost.troop.model.Incident;
import zw.co.afc.orbit.outpost.troop.repository.AlertRepository;
import zw.co.afc.orbit.outpost.troop.repository.ApplicationRepository;
import zw.co.afc.orbit.outpost.troop.repository.GarrisonRepository;
import zw.co.afc.orbit.outpost.troop.repository.IncidentRepository;
import zw.co.afc.orbit.outpost.troop.service.iface.ApplicationService;
import zw.co.afc.orbit.outpost.troop.service.iface.NotificationService;
import zw.co.afc.orbit.outpost.troop.service.iface.ServerService;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceStatus {

    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;
    private final ServerService serverService;
    private final AlertRepository alertRepository;
    private final GarrisonRepository garrisonRepository;
    private final NotificationService notificationService;
    private final IncidentRepository incidentRepository;
    private int diskUsageAlertCount = 0;
    private final int MAX_DISK_ALERTS = 3;

    @Scheduled(fixedRate = 20000)
    public void runServiceStatusCheck() {
        log.info("🔔 THE SCHEDULER IS RUNNING 🔔");
        StringBuilder statusReport = new StringBuilder("<h3>Service Status Report:</h3><ul>");
        int initialLength = statusReport.length();

        List<Application> applications = applicationRepository.findAll();

        // Identify down applications
        List<Application> downApps = applications.stream()
                .filter(app -> {
                    String jarPath = app.getJarFileLocation() + "/" + app.getName() + "-0.0.1-SNAPSHOT.jar";
                    String getProcessId = "pgrep -f '" + jarPath + "'";
                    List<String> processIds = applicationService.getProcessId(getProcessId);
                    return processIds.isEmpty();
                }).toList();

        boolean allDown = downApps.size() == applications.size();
        boolean startup1Down = downApps.stream().anyMatch(app -> app.getStartupOrder() == 1);
        boolean startup2Down = downApps.stream().anyMatch(app -> app.getStartupOrder() == 2);

        if (allDown || startup1Down || startup2Down) {
            log.warn("Restarting services in startup order with waits (either all down or critical services down)");

            applications.stream()
                    .sorted(Comparator.comparing(Application::getStartupOrder))
                    .forEach(app -> {
                        String status = checkApplicationStatus(app);
                        statusReport.append("<li>").append(status).append("</li>");

                        try {
                            if (app.getStartupOrder() == 1) {
                                log.info("⏱ Waiting 30 seconds after starting {}", app.getName());
                                Thread.sleep(30_000);
                            } else if (app.getStartupOrder() == 2) {
                                log.info("⏱ Waiting 10 seconds after starting {}", app.getName());
                                Thread.sleep(10_000);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Interrupted during wait after startup order {}", app.getStartupOrder(), e);
                        }
                    });


        } else {
            log.info(". Some services are down, but startup order 1 & 2 are UP. Restarting only the down services.");
            //downApps.forEach(this::checkApplicationStatus);  // restart individually with no delay
            downApps.forEach(app -> {
                String status = checkApplicationStatus(app);
                //statusReport.append(status);
                statusReport.append("<li>").append(status).append("</li>");
            });

        }

        checkDiskUsageWithCommand(statusReport);  // Optional: Always check disk usage

        if (statusReport.length() > initialLength) {
            sendConsolidatedAlert(statusReport.toString());
        }
    }


    private String checkApplicationStatus(Application application) {


        String jarPath = application.getJarFileLocation() + "/" + application.getName() + "-0.0.1-SNAPSHOT.jar";
        String getProcessId = "pgrep -f '" + jarPath + "'";

        List<String> processIds = applicationService.getProcessId(getProcessId);

        if (processIds.isEmpty()) {
            log.error("\uD83D\uDED1 {} is not running", application.getName());
            String logFilePath = application.getLogFileLocation() + "/" + application.getName() + "-temp.log";
            String startApplication = "nohup java -jar " + application.getJarFileLocation() + "/" + application.getName() + "-0.0.1-SNAPSHOT.jar > " + logFilePath + " 2>&1 &";

            log.info("Starting service: {}", application.getName());
            applicationService.startApplication(startApplication);

            if (shouldSendAlert(application.getName())) {
                //sendAlert(application.getName() + " is down!", "CRITICAL");
                recordIncident(application.getName(), "SERVICE_DOWN");
            }

            return application.getName() + " was down and has been restarted.";

        } else {
            //sendAlert(application.getName() + " is running!",null);
            log.info("\uD83D\uDFE2 {} is running with process IDs: {}", application.getName(), processIds);
            return application.getName() + " is running.";
        }
    }

    private boolean shouldSendAlert(String alertMessage) {
        LocalDateTime lastAlert = alertRepository.findLatestAlertTimeByAlertMessage(alertMessage);
        if (lastAlert == null || lastAlert.isBefore(LocalDateTime.now().minusMinutes(10))) {
            return true;
        }
        return false;
    }

    private void sendConsolidatedAlert(String message) {

        List<Garrison> recipients = garrisonRepository.findAllByActiveTrue();

        for (Garrison user : recipients) {
            Alert alert = new Alert();
            alert.setUser(user);
            alert.setAlertMessage(message);
            alert.setAlertTime(LocalDateTime.now());
            alert.setStatus("SENT");
            alert.setMedium("EMAIL");
            alertRepository.save(alert);

            // Send via WhatsApp, Email, or SMS
            //notificationService.sendWhatsAppAlert(user, message);
            log.info("Initializing ");
            notificationService.sendEmailAlert(user,"Service & Disk Status Report", message);
        }
    }

    private void recordIncident(String name, String type) {
        Incident existingIncident = incidentRepository.findByIncidentTypeAndStatus(type, IncidentStatus.OPEN);
        if (existingIncident == null) {
            Incident incident = new Incident();
            incident.setIncidentType(type);
            incident.setDescription(name + " issue detected.");
            incident.setStatus(IncidentStatus.OPEN);
            incident.setPriority("HIGH");
            incident.setCreatedAt(LocalDateTime.now());
            incidentRepository.save(incident);
        }
    }

    private void checkDiskUsageWithCommand(StringBuilder statusReport) {
        try {
            Process process = Runtime.getRuntime().exec("df -h");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {

                // Check if the line contains /dev/sda3 (the main disk)
                if (line.startsWith("/dev/sda3")) {
                    String[] parts = line.split("\\s+"); // Split by whitespace

                    if (parts.length >= 5) {
                        String usageStr = parts[4];  // The 5th column (index 4) is the usage percentage
                        int usage = Integer.parseInt(usageStr.replace("%", ""));


                        log.info("📊 Main Disk (/dev/sda3) Usage: {}%", usage);

                        if (usage > 60) {
                            if (diskUsageAlertCount < MAX_DISK_ALERTS && shouldSendAlert("Main Disk")) {
                                log.error("🚨 Main disk usage is high: {}% used", usage);
                                statusReport.append("<li> Disk /dev/sda3 usage: ").append(usage).append("%<li>");
                                recordIncident("Main Disk", "DISK_HIGH_USAGE");
                                diskUsageAlertCount++;
                            } else {
                                log.info("⚠️ Disk usage alert already sent {} times. Skipping further alerts.", diskUsageAlertCount);
                            }
                        } else {
                            if (diskUsageAlertCount > 0) {
                                log.info("✅ Disk usage is normal. Resetting alert count.");
                                diskUsageAlertCount = 0;
                            }
                        }


                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("df -h command failed with exit code: {}", exitCode);
            }
        } catch (Exception e) {
            log.error("Error executing df -h: ", e);
        }
    }


}
