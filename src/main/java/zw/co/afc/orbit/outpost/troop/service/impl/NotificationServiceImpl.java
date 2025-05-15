package zw.co.afc.orbit.outpost.troop.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import zw.co.afc.orbit.outpost.troop.dto.request.NotificationRequest;
import zw.co.afc.orbit.outpost.troop.model.Garrison;
import zw.co.afc.orbit.outpost.troop.service.iface.NotificationService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private static final String WEBHOOK_URL = "https://integrations.afcholdings.co.zw/wtest/webhook/external/alert";

    @Override
    public boolean sendWhatsAppAlert(Garrison user, String message) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            log.warn("⚠️ User {} has no phone number set, skipping WhatsApp alert.", user.getName());
            return false;
        }

        Map<String, Object> requestBody = Map.of(
                "recipient", user.getPhoneNumber(),
                "payload", Map.of(
                        "messaging_product", "whatsapp",
                        "recipient_type", "individual",
                        "to", user.getPhoneNumber(),
                        "type", "text",
                        "text", Map.of(
                                "preview_url", false,
                                "body", message
                        )
                )
        );

        log.info("Request: ", requestBody);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(WEBHOOK_URL, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ WhatsApp alert sent successfully to {}", user.getPhoneNumber());
                return true;
            } else {
                log.error("❌ Failed to send WhatsApp alert to {}: {}", user.getPhoneNumber(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Exception while sending WhatsApp alert to {}: {}", user.getPhoneNumber(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendEmailAlert(Garrison user, String message) {
        log.info("Processing email alert");
        NotificationRequest notificationRequest = new NotificationRequest(
                "DEFAULT",
                List.of(user.getEmail()),
                "OUTPOST ALERT",
                message,
                "SLA",
                null,
                null,
                "SERVICE",
                 true

        );
        log.info("Sending alert to: {}", user.getEmail());

        Map notificationRequestString = objectMapper.convertValue(notificationRequest, Map.class);
        log.info("Successful conversion");

        kafkaTemplate.send("notification-notify", notificationRequestString);
        return true;
    }

}
