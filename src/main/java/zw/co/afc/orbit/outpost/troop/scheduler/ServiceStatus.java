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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

//    @Scheduled(fixedRate = 20000)
//    public void runServiceStatusCheck(){
//        log.info("\uD83D\uDD14 THE SCHEDULER IS RUNNING \uD83D\uDD14");
//        List<Application> applications = applicationRepository.findAll();
//        for (Application application : applications) {
//            String getProcessId = "cd " + application.getJarFileLocation() + " && pgrep -f -d ' ' " + application.getName() + "-0.0.1-SNAPSHOT.jar";
//            List<String> processIds = applicationService.getProcessId(getProcessId);
//            if (processIds.isEmpty()) {
//                log.error("\uD83D\uDED1 " + application.getName() + " is not running");
//                String logFilePath = application.getLogFileLocation() + "/" + application.getName() + "-temp.log";
//                String startApplication = "nohup java -jar " + application.getJarFileLocation() + "/" + application.getName() + "-0.0.1-SNAPSHOT.jar > " + logFilePath + " 2>&1 &";
//
//                applicationService.startApplication(startApplication);
//                // send notification via whatsapp
//                // add it to database so that it is logged as a case where a service stopped
//                // maybe add a check -> maybe the service should be kept offline? for some odd reason
//            } else {
//                log.info("\uD83D\uDFE2 " + application.getName() + " is running with process IDs: " + processIds );
//            }
//        }
//
//        log.info("Checking disks");
//        DiskMetrics disks = serverService.displaySpaceMetrics();
//        for (DiskInfo disk : disks.disks()) {
//            if (disk.isMainDisk()) {
//                // Log the details of the main disk
//                log.info("Main Disk: "+ disk.name() +" - Total: "+disk.totalGB()+" GB, Used: "+disk.usedGB()+" GB, Free: "+disk.freeGB()+" GB, Usage: "+disk.usagePercentage()+"%");
//
//                // Check the usage percentage and log accordingly
//                double usage = disk.usagePercentage();
//                BigDecimal usageFormatted = new BigDecimal(usage).setScale(2, RoundingMode.HALF_UP);
//
//                if (usage > 80) {
//                    log.error("\uD83D\uDED1 Main disk usage is high: {}% used", usageFormatted);
//                } else if (usage > 50) {
//                    log.warn("\uD83D\uDFE0 Main disk usage is moderate: {}% used", usageFormatted);
//                } else {
//                    log.info("\uD83D\uDFE2 Main disk usage is okay: {}% used", usageFormatted);
//                }
//            }
//        }
//
//
//    }

    @Scheduled(fixedRate = 20000)
    public void runServiceStatusCheck() {
        log.info("\uD83D\uDD14 THE SCHEDULER IS RUNNING \uD83D\uDD14");

        List<Application> applications = applicationRepository.findAll();
        for (Application application : applications) {
            checkApplicationStatus(application);
        }

        checkDiskUsage();
    }

    private void checkApplicationStatus(Application application) {
        String getProcessId = "cd " + application.getJarFileLocation() +
                " && pgrep -f -d ' ' " + application.getName() + "-0.0.1-SNAPSHOT.jar";
        List<String> processIds = applicationService.getProcessId(getProcessId);

        if (processIds.isEmpty()) {
            log.error("\uD83D\uDED1 {} is not running", application.getName());
            String logFilePath = application.getLogFileLocation() + "/" + application.getName() + "-temp.log";
            String startApplication = "nohup java -jar " + application.getJarFileLocation() + "/" + application.getName() + "-0.0.1-SNAPSHOT.jar > " + logFilePath + " 2>&1 &";

//            applicationService.startApplication(startApplication);

            if (shouldSendAlert(application.getName())) {
                sendAlert(application.getName() + " is down!", "CRITICAL");
                recordIncident(application.getName(), "SERVICE_DOWN");
            }
        } else {
            log.info("\uD83D\uDFE2 {} is running with process IDs: {}", application.getName(), processIds);
        }
    }

    private void checkDiskUsage() {
        DiskMetrics disks = serverService.displaySpaceMetrics();
        for (DiskInfo disk : disks.disks()) {
            if (disk.isMainDisk()) {
                double usage = disk.usagePercentage();
                BigDecimal usageFormatted = new BigDecimal(usage).setScale(2, RoundingMode.HALF_UP);

                if (usage > 80 && shouldSendAlert("Main Disk")) {
                    log.error("\uD83D\uDED1 Main disk usage is high: {}% used", usageFormatted);
                    sendAlert("Disk usage is critical: " + usageFormatted + "%", "HIGH");
                    recordIncident("Main Disk", "DISK_HIGH_USAGE");
                }
            }
        }
    }

    private boolean shouldSendAlert(String alertMessage) {
        LocalDateTime lastAlert = alertRepository.findLatestAlertTimeByAlertMessage(alertMessage);
        if (lastAlert == null || lastAlert.isBefore(LocalDateTime.now().minusMinutes(30))) {
            return true;
        }
        return false;
    }

    private void sendAlert(String message, String priority) {
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
            notificationService.sendWhatsAppAlert(user, message);
            log.info("Initializing ");
//            notificationService.sendEmailAlert(user, message);
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


}
