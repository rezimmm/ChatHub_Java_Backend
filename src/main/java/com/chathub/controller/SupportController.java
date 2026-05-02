package com.chathub.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${support.email}")
    private String supportEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/report-bug")
    public ResponseEntity<?> reportBug(@RequestBody Map<String, Object> bugReport) {
        try {
            String title = (String) bugReport.get("title");
            String severity = (String) bugReport.get("severity");
            String description = (String) bugReport.get("description");
            String steps = (String) bugReport.get("steps");
            String browser = (String) bugReport.get("browser");

            String emailBody = String.format(
                "<h1>New Bug Report</h1>" +
                "<p><strong>Title:</strong> %s</p>" +
                "<p><strong>Severity:</strong> %s</p>" +
                "<p><strong>Description:</strong> %s</p>" +
                "<p><strong>Steps to Reproduce:</strong> %s</p>" +
                "<p><strong>Browser Info:</strong> %s</p>",
                title, severity, description, steps, browser
            );

            Map<String, Object> resendRequest = new HashMap<>();
            resendRequest.put("from", "ChatHub Support <onboarding@resend.dev>");
            resendRequest.put("to", supportEmail);
            resendRequest.put("subject", "Bug Report: " + title);
            resendRequest.put("html", emailBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(resendRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.resend.com/emails",
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of("message", "Bug report sent successfully"));
            } else {
                log.error("Resend API error: {}", response.getBody());
                return ResponseEntity.status(response.getStatusCode()).body(Map.of("error", "Failed to send email via Resend"));
            }
        } catch (Exception e) {
            log.error("Error reporting bug", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
