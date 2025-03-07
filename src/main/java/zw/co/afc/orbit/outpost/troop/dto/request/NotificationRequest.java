package zw.co.afc.orbit.outpost.troop.dto.request;

import java.util.List;
import java.util.Map;

public record NotificationRequest(
        String node,
        List<String> recipients,
        String subject,
        String message,
        String source,
        String template,
        Map<String, Object> templateData
) {
}
