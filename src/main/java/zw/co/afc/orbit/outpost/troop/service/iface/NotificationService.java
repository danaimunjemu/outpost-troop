package zw.co.afc.orbit.outpost.troop.service.iface;

import zw.co.afc.orbit.outpost.troop.model.Garrison;

public interface NotificationService {
    boolean sendWhatsAppAlert(Garrison user, String message);

    boolean sendEmailAlert(Garrison user,String subject, String message);
}
