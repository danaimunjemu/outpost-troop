package zw.co.afc.orbit.outpost.troop.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import zw.co.afc.orbit.outpost.troop.model.Application;
import zw.co.afc.orbit.outpost.troop.repository.ApplicationRepository;
import zw.co.afc.orbit.outpost.troop.service.iface.ApplicationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceStatus {

    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;

    @Scheduled(fixedRate = 20000)
    public void runServiceStatusCheck(){
        log.info("\uD83D\uDD14 THE SCHEDULER IS RUNNING \uD83D\uDD14");
        List<Application> applications = applicationRepository.findAll();
        for (Application application : applications) {
            String getProcessId = "cd " + application.getJarFileLocation() + " && pgrep -f -d ' ' " + application.getName() + "-0.0.1-SNAPSHOT.jar";
            List<String> processIds = applicationService.getProcessId(getProcessId);
            if (processIds.isEmpty()) {
                log.error("\uD83D\uDED1 " + application.getName() + " is not running");
//                String logFilePath = "/Users/danaimunjemu/Downloads/" + application.getName() +".log";
                String logFilePath = application.getLogFileLocation() + "/" + application.getName() + "-temp.log";
                String startApplication = "nohup java -jar " + application.getJarFileLocation() + "/" + application.getName() + "-0.0.1-SNAPSHOT.jar > " + logFilePath + " 2>&1 &";
                applicationService.startApplication(startApplication);
                // send notification via whatsapp
                // add it to database so that it is logged as a case where a service stopped
                // maybe add a check -> maybe the service should be kept offline? for some odd reason
            } else {
                log.info("\uD83D\uDFE2 " + application.getName() + " is running with process IDs: " + processIds );
            }
        }

    }

}
