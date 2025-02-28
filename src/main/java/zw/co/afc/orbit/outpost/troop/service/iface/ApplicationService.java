package zw.co.afc.orbit.outpost.troop.service.iface;

import java.util.List;

public interface ApplicationService {
    List<String> getProcessId(String command);

    void startApplication(String command);
}
